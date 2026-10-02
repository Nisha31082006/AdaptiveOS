package com.adaptiveos.engine.scheduler;

import com.adaptiveos.engine.config.SchedulerConfig;
import com.adaptiveos.engine.logging.SimLogger;
import com.adaptiveos.engine.model.Burst;
import com.adaptiveos.engine.model.ProcessControlBlock;
import com.adaptiveos.engine.model.SchedulingPolicy;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Unit tests of the adaptive rules in isolation (no simulation engine involved). */
class AdaptiveControllerTest {

    /** A PCB that has executed {@code executed} ticks of a turn whose slice was {@code quantum} ticks. */
    private static ProcessControlBlock pcbWithTurn(int executed, int quantum) {
        ProcessControlBlock p = new ProcessControlBlock(1, 0, List.of(Burst.cpu(1000)), 512);
        p.beginTurn(quantum);
        for (int i = 0; i < executed; i++) {
            p.runOneTick();
        }
        return p;
    }

    private static AdaptiveController controller(SchedulerConfig cfg) {
        return new AdaptiveController(cfg, SimLogger.disabled());
    }

    private static void heavy(AdaptiveController c, long t) {
        c.observe(pcbWithTurn(4, 4), RuntimeEvent.QUANTUM_EXPIRED, t);
    }

    private static void interactive(AdaptiveController c, long t) {
        c.observe(pcbWithTurn(2, 4), RuntimeEvent.CPU_BURST_COMPLETED_TO_IO, t); // E = 0.5
    }

    // ------------------------------------------------------------------ EWMA
    @Test
    void ewmaHeavyExampleFromSpec_512_to_384() {
        AdaptiveController c = controller(SchedulerConfig.defaults());
        heavy(c, 4);
        assertEquals(384.0, c.score(), 1e-9);
    }

    @Test
    void ewmaInteractiveExampleFromSpec_288_to_472() {
        AdaptiveController c = controller(SchedulerConfig.defaults().withInitialScore(288));
        interactive(c, 7);
        assertEquals(472.0, c.score(), 1e-9);
    }

    @Test
    void startsWithRoundRobinAndInitialScore() {
        AdaptiveController c = controller(SchedulerConfig.defaults());
        assertEquals(SchedulingPolicy.RR, c.currentPolicy());
        assertEquals(512.0, c.score(), 1e-9);
    }

    // ------------------------------------------------------------------ thresholds + hysteresis
    @Test
    void scoreInsideTheDeadbandDoesNotSwitch() {
        AdaptiveController c = controller(SchedulerConfig.defaults());
        heavy(c, 4);                                   // 384: still above LOW = 307
        assertTrue(c.evaluate(4, 2).isEmpty());
        assertEquals(SchedulingPolicy.RR, c.currentPolicy());
    }

    @Test
    void rrSwitchesToSrtfOnlyBelowLow() {
        AdaptiveController c = controller(SchedulerConfig.defaults());
        heavy(c, 4);
        heavy(c, 8);                                   // 288 < 307
        Optional<AdaptiveController.PolicySwitch> sw = c.evaluate(8, 2);
        assertTrue(sw.isPresent());
        assertEquals(SchedulingPolicy.SRTF, sw.get().to());
        assertEquals(SchedulingPolicy.SRTF, c.currentPolicy());
    }

    @Test
    void srtfDoesNotReturnToRrInsideTheBandAndCooldownDelaysTheReturn() {
        AdaptiveController c = controller(SchedulerConfig.defaults());
        heavy(c, 4);
        heavy(c, 8);
        assertTrue(c.evaluate(8, 1).isPresent());      // RR -> SRTF at t=8, cooldown until t=20
        assertEquals(20, c.safeUntil());

        interactive(c, 9);                             // 472  (inside the band: no switch back)
        assertTrue(c.evaluate(9, 1).isEmpty());
        interactive(c, 10);                            // 610
        interactive(c, 11);                            // 713.5  (still below HIGH = 716)
        assertTrue(c.evaluate(11, 1).isEmpty());
        interactive(c, 12);                            // 791.1  (> HIGH) but cooldown is active
        assertTrue(c.score() > 716);
        assertTrue(c.evaluate(12, 1).isEmpty());
        assertTrue(c.evaluate(19, 1).isEmpty());
        assertEquals(SchedulingPolicy.SRTF, c.currentPolicy());

        Optional<AdaptiveController.PolicySwitch> back = c.evaluate(20, 1); // cooldown over
        assertTrue(back.isPresent());
        assertEquals(SchedulingPolicy.RR, back.get().to());
    }

    @Test
    void zeroCooldownAllowsImmediateSwitchBack() {
        AdaptiveController c = controller(SchedulerConfig.defaults().withCooldownTicks(0));
        heavy(c, 1);
        heavy(c, 2);
        assertTrue(c.evaluate(2, 1).isPresent());
        for (int t = 3; t <= 6; t++) {
            interactive(c, t);
        }
        assertTrue(c.evaluate(6, 1).isPresent());
    }

    // ------------------------------------------------------------------ surge / phase change
    @Test
    void interactiveEventResetsTheSurgeCounter() {
        AdaptiveController c = controller(SchedulerConfig.defaults());
        heavy(c, 1);
        heavy(c, 2);
        assertEquals(2, c.surgeCounter());
        interactive(c, 3);
        assertEquals(0, c.surgeCounter());
    }

    @Test
    void threeHeavyEventsAreEvidenceNotAnAutomaticSwitch() {
        // alpha 0.10 keeps the score high (1000 -> 900 -> 810 -> 729) although surge reaches 3
        SchedulerConfig cfg = SchedulerConfig.defaults().withAlpha(0.10).withInitialScore(1000);
        AdaptiveController c = controller(cfg);
        heavy(c, 1);
        heavy(c, 2);
        heavy(c, 3);
        assertEquals(3, c.surgeCounter());
        assertTrue(c.hasPhaseChangeEvidence());
        assertTrue(c.evaluate(3, 5).isEmpty());        // score is still far above neutral
        assertEquals(SchedulingPolicy.RR, c.currentPolicy());
    }

    @Test
    void phaseChangeEvidenceCanTriggerAnEarlySwitchWhenContextAgrees() {
        SchedulerConfig cfg = SchedulerConfig.defaults().withInitialScore(1000); // alpha 0.25
        AdaptiveController c = controller(cfg);
        heavy(c, 4);   // 750
        heavy(c, 8);   // 562.5
        assertTrue(c.evaluate(8, 2).isEmpty());        // surge = 2 < 3: no evidence yet
        heavy(c, 12);  // 421.875, surge = 3, still above LOW = 307
        assertTrue(c.score() > 307);
        assertTrue(c.evaluate(12, 0).isEmpty());       // nobody waiting: SRTF could not help anyone
        Optional<AdaptiveController.PolicySwitch> sw = c.evaluate(12, 1);
        assertTrue(sw.isPresent());
        assertTrue(sw.get().reason().contains("phase-change"));
        assertEquals(0, c.surgeCounter());             // evidence consumed by the switch
    }

    // ------------------------------------------------------------------ anti-gaming
    @Test
    void antiGamingUsesTheTrackedQuantumNotAHardCodedOne() {
        SchedulerConfig cfg = SchedulerConfig.defaults().withQuantum(20);
        AdaptiveController c = controller(cfg);
        ProcessControlBlock used95 = pcbWithTurn(19, 20);  // E = 0.95 -> CPU-heavy-like (>= threshold)
        ProcessControlBlock used90 = pcbWithTurn(18, 20);  // E = 0.90 -> interactive-like
        assertEquals(BehaviorSignal.CPU_HEAVY, c.classify(used95, RuntimeEvent.CPU_BURST_COMPLETED_TO_IO, 1));
        assertEquals(BehaviorSignal.INTERACTIVE, c.classify(used90, RuntimeEvent.CPU_BURST_COMPLETED_TO_IO, 1));
    }

    @Test
    void burstEndingExactlyAtQuantumIsCpuHeavyLikeButNotAnExpiry() {
        AdaptiveController c = controller(SchedulerConfig.defaults());
        ProcessControlBlock p = pcbWithTurn(4, 4);       // E = 1.0
        assertEquals(BehaviorSignal.CPU_HEAVY, c.classify(p, RuntimeEvent.CPU_BURST_COMPLETED_TO_IO, 4));
    }

    @Test
    void thresholdIsConfigurable() {
        AdaptiveController strict = controller(SchedulerConfig.defaults().withAntiGamingThreshold(0.5));
        ProcessControlBlock p = pcbWithTurn(2, 4);       // E = 0.5 -> now >= threshold
        assertEquals(BehaviorSignal.CPU_HEAVY, strict.classify(p, RuntimeEvent.CPU_BURST_COMPLETED_TO_IO, 1));
        assertFalse(controller(SchedulerConfig.defaults()).classify(p, RuntimeEvent.CPU_BURST_COMPLETED_TO_IO, 1)
                == BehaviorSignal.CPU_HEAVY);
    }
}
