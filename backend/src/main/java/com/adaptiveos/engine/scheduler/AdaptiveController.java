package com.adaptiveos.engine.scheduler;

import com.adaptiveos.engine.config.SchedulerConfig;
import com.adaptiveos.engine.logging.EventType;
import com.adaptiveos.engine.logging.SimLogger;
import com.adaptiveos.engine.model.ProcessControlBlock;
import com.adaptiveos.engine.model.SchedulingPolicy;

import java.util.Optional;

/**
 * The brain of AdaptiveOS: a deterministic, rule-based controller (NO machine learning, NO randomness).
 *
 * <pre>
 *  Runtime event
 *        |  observe()
 *        v
 *  Classify event  (INTERACTIVE or CPU_HEAVY, with the anti-gaming ratio E for voluntary yields)
 *        v
 *  Update global EWMA score        score = alpha*signal + (1-alpha)*score
 *        v
 *  Update surge counter            CPU-heavy: +1      interactive: reset to 0
 *        |
 *        |  evaluate()   (called once per tick by the engine)
 *        v
 *  Cooldown check   (now < safeUntil  ->  never switch)
 *        v
 *  Threshold check with hysteresis:
 *        RR   -> SRTF  only if score <  LOW  (307)
 *        SRTF -> RR    only if score >  HIGH (716)
 *        LOW..HIGH band: keep current policy (deadband)
 *        v
 *  Phase-change evidence (RR only): surgeCounter >= SURGE_THRESHOLD is EVIDENCE, not a trigger.
 *        The early switch additionally needs: score already below the neutral midpoint,
 *        and at least one other process ready (otherwise SRTF cannot help anyone).
 *        v
 *  Global policy switch RR <-> SRTF  (sets cooldown, consumes surge evidence)
 * </pre>
 *
 * All state here is GLOBAL (one score, one surge counter, one cooldown) because AdaptiveOS switches the
 * policy of the whole scheduler, not of individual processes.
 */
public final class AdaptiveController {

    /** Describes one policy switch; kept in the simulation result for the Gantt/log output. */
    /**
     * @param score         EWMA score at the moment of the switch
     * @param surgeCounter  surge counter at the moment of the switch (before it is consumed)
     * @param cooldownUntil tick until which further switches are blocked
     */
    public record PolicySwitch(long time, SchedulingPolicy from, SchedulingPolicy to, String reason,
                               double score, int surgeCounter, long cooldownUntil) { }

    private final SchedulerConfig config;
    private final SimLogger logger;

    private SchedulingPolicy policy = SchedulingPolicy.RR; // ADAPTIVE always starts with RR
    private double score;
    private int surgeCounter;
    /** Cooldown: while now < safeUntil the controller must not switch policy. */
    private long safeUntil;
    private int switchCount;
    private boolean suppressionLogged;

    public AdaptiveController(SchedulerConfig config, SimLogger logger) {
        this.config = config;
        this.logger = logger;
        this.score = config.initialScore();
    }

    // ================================================================== step 1-4: observe an event

    /**
     * Receives one runtime event from the engine, classifies it, and updates the EWMA score and
     * surge counter. Does NOT switch policy by itself (see {@link #evaluate}).
     *
     * @param process the process that produced the event (its executedBurst/allocatedQuantumTurn are read)
     * @param event   what happened
     * @param now     simulated time at which the event happened
     */
    public void observe(ProcessControlBlock process, RuntimeEvent event, long now) {
        BehaviorSignal signal = classify(process, event, now);

        double previous = score;
        score = ewma(signal.value(), previous);
        boolean interactive = signal == BehaviorSignal.INTERACTIVE;
        process.updateOwnScore(signal.value(), config.ewmaAlpha(), interactive, safeUntil);

        logger.log(now, EventType.SIGNAL, process.getName(), "%s signal", interactive ? "Interactive" : "CPU-heavy");
        logger.log(now, EventType.EWMA_UPDATE, process.getName(), "EWMA score: %.1f -> %.1f", previous, score);
        if (interactive) {
            if (surgeCounter != 0) {
                logger.log(now, EventType.SURGE, process.getName(), "Surge counter reset (was %d)", surgeCounter);
            }
            surgeCounter = 0;
        } else {
            surgeCounter++;
            logger.log(now, EventType.SURGE, process.getName(), "Surge counter: %d", surgeCounter);
        }
    }

    /**
     * Classification rules:
     * <ul>
     *   <li>QUANTUM_EXPIRED -> CPU_HEAVY (involuntary).</li>
     *   <li>CPU_BURST_COMPLETED_TO_IO with E = executed/allocated:
     *       E &lt; threshold -> INTERACTIVE, E &gt;= threshold -> CPU_HEAVY (anti-gaming).</li>
     * </ul>
     * The quantum is read from the PCB's allocatedQuantumTurn, never hard-coded.
     */
    BehaviorSignal classify(ProcessControlBlock process, RuntimeEvent event, long now) {
        if (event == RuntimeEvent.QUANTUM_EXPIRED) {
            return BehaviorSignal.CPU_HEAVY;
        }
        double e = process.quantumUsageRatio();
        boolean interactive = e < config.antiGamingThreshold();
        logger.log(now, EventType.ANTI_GAMING, process.getName(), "Anti-gaming: E = %d/%d = %.2f %s %.2f -> %s-like",
                process.getExecutedBurst(), process.getAllocatedQuantumTurn(), e,
                interactive ? "<" : ">=", config.antiGamingThreshold(),
                interactive ? "interactive" : "CPU-heavy");
        return interactive ? BehaviorSignal.INTERACTIVE : BehaviorSignal.CPU_HEAVY;
    }

    /** I_k = alpha * signal_k + (1 - alpha) * I_(k-1). Recent behaviour weighs most; old behaviour fades. */
    double ewma(double signal, double previous) {
        return config.ewmaAlpha() * signal + (1.0 - config.ewmaAlpha()) * previous;
    }

    // ================================================================== step 5-8: decide

    /**
     * Decides whether the global policy should change at time {@code now}.
     *
     * @param now        current simulated time
     * @param readyCount number of processes waiting in the ready queue (the running process not included)
     * @return the switch that was performed, or empty if the policy stays
     */
    public Optional<PolicySwitch> evaluate(long now, int readyCount) {
        SchedulingPolicy target = policy;
        String reason = null;

        if (policy == SchedulingPolicy.RR) {
            if (score < config.lowThreshold()) {
                target = SchedulingPolicy.SRTF;
                reason = String.format("score %.1f < LOW %.0f", score, config.lowThreshold());
            } else if (hasPhaseChangeEvidence() && score < config.neutralScore() && readyCount >= 1) {
                target = SchedulingPolicy.SRTF;
                reason = String.format("phase-change evidence: surge %d >= %d, score %.1f < neutral %.1f, %d ready",
                        surgeCounter, config.surgeThreshold(), score, config.neutralScore(), readyCount);
            }
        } else if (score > config.highThreshold()) {
            target = SchedulingPolicy.RR;
            reason = String.format("score %.1f > HIGH %.0f", score, config.highThreshold());
        }

        if (target == policy) {
            return Optional.empty();
        }
        if (now < safeUntil) { // cooldown blocks the switch
            if (!suppressionLogged) {
                logger.log(now, EventType.COOLDOWN_BLOCKED, null, "Adaptive controller: %s -> %s wanted (%s) but blocked by cooldown until t=%d",
                        policy, target, reason, safeUntil);
                suppressionLogged = true;
            }
            return Optional.empty();
        }
        return Optional.of(switchTo(target, now, reason));
    }

    /** Repeated CPU-heavy events = phase-change EVIDENCE (never an automatic switch by itself). */
    boolean hasPhaseChangeEvidence() {
        return surgeCounter >= config.surgeThreshold();
    }

    private PolicySwitch switchTo(SchedulingPolicy target, long now, String reason) {
        PolicySwitch sw = new PolicySwitch(now, policy, target, reason, score, surgeCounter, now + config.cooldownTicks());
        logger.log(now, EventType.POLICY_SWITCH, null, "Adaptive controller: %s -> %s (%s)", policy, target, reason);
        policy = target;
        safeUntil = now + config.cooldownTicks();
        surgeCounter = 0;          // the evidence has been used up
        suppressionLogged = false;
        switchCount++;
        logger.log(now, EventType.COOLDOWN, null, "Cooldown active until t=%d", safeUntil);
        return sw;
    }

    // ================================================================== read-only state
    public SchedulingPolicy currentPolicy() { return policy; }
    public double score() { return score; }
    public int surgeCounter() { return surgeCounter; }
    public long safeUntil() { return safeUntil; }
    public int switchCount() { return switchCount; }
}
