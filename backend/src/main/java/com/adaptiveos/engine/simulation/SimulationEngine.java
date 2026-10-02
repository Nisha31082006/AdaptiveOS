package com.adaptiveos.engine.simulation;

import com.adaptiveos.engine.config.SchedulerConfig;
import com.adaptiveos.engine.logging.EventType;
import com.adaptiveos.engine.logging.SimLogger;
import com.adaptiveos.engine.metrics.MetricsCalculator;
import com.adaptiveos.engine.metrics.MetricsSummary;
import com.adaptiveos.engine.model.Burst;
import com.adaptiveos.engine.model.BurstType;
import com.adaptiveos.engine.model.ProcessControlBlock;
import com.adaptiveos.engine.model.ProcessState;
import com.adaptiveos.engine.model.SchedulingPolicy;
import com.adaptiveos.engine.scheduler.AdaptiveController;
import com.adaptiveos.engine.scheduler.AdaptiveController.PolicySwitch;
import com.adaptiveos.engine.scheduler.RoundRobinScheduler;
import com.adaptiveos.engine.scheduler.RuntimeEvent;
import com.adaptiveos.engine.scheduler.SRTFScheduler;
import com.adaptiveos.engine.scheduler.Scheduler;
import com.adaptiveos.engine.workload.Workload;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Discrete-time CPU scheduling simulator. One loop iteration = one tick = one unit of simulated time.
 * Tick {@code t} covers the time interval [t, t+1). Events caused by executing during tick t happen at
 * time t+1 ("now" in the code).
 *
 * <h3>Deterministic order of work inside one tick</h3>
 * <ol>
 *   <li><b>Arrivals</b>: processes with arrivalTime == t become READY (in PID order).</li>
 *   <li><b>I/O completions</b>: waiters whose I/O has reached 0 become READY (in PID order), AFTER arrivals.</li>
 *   <li><b>Dispatch / preempt</b>: SRTF may preempt the running process (strictly shorter candidate only);
 *       if the CPU is free the active policy picks the next process. This is done at the START of the
 *       tick (after arrivals) so a process arriving at t can preempt at t.</li>
 *   <li><b>Waiting-time accounting</b>: every process still READY gains one waiting tick.</li>
 *   <li><b>Execute</b>: the running process executes ONE tick (or the CPU idles).</li>
 *   <li><b>I/O advance</b>: every process that was already WAITING at the start of the tick does ioRemaining--.
 *       (Done before step 7 so a process that enters I/O this tick does not lose a tick of I/O.)</li>
 *   <li><b>Events</b>: burst completion / quantum expiry of the running process; the AdaptiveController
 *       is told about them (telemetry).</li>
 *   <li><b>Adaptive decision</b>: the controller may switch the global policy; the ready queue is migrated.
 *       A preempted/expired process is re-queued in step 7, i.e. BEFORE next tick's arrivals.</li>
 * </ol>
 *
 * <h3>Conventions</h3>
 * <ul>
 *   <li>A "turn" is one uninterrupted stretch of CPU time; its slice length is the RR quantum. Under SRTF
 *       there is no preemption by quantum, but the same slice length is used as a virtual observation
 *       window so the AdaptiveController can keep observing behaviour while SRTF is active.</li>
 *   <li>CPU burst followed by ANOTHER CPU burst: the process re-enters the ready queue (RUNNING to READY) so
 *       the new burst is scheduled from scratch. No behaviour event is generated (no yield-to-I/O evidence).</li>
 *   <li>Last CPU burst finishes: TERMINATED, no behaviour event.</li>
 *   <li>Context switch = the CPU is given to a process that is different from the last process that ran.
 *       The very first dispatch and idle gaps followed by the same process do not count.</li>
 * </ul>
 */
public final class SimulationEngine {

    private final Workload workload;
    private final ExecutionMode mode;
    private final SchedulerConfig config;
    private final SimLogger logger;

    private final List<ProcessControlBlock> processes;       // fresh copies, sorted by PID
    private final List<ProcessControlBlock> pendingArrivals; // sorted by (arrival, pid)
    private final List<ProcessControlBlock> waitingForIo = new ArrayList<>();
    private int nextArrivalIndex = 0;

    private final RoundRobinScheduler rrScheduler = new RoundRobinScheduler();
    private final SRTFScheduler srtfScheduler = new SRTFScheduler();
    private Scheduler activeScheduler;
    private final AdaptiveController controller; // null unless ADAPTIVE

    private ProcessControlBlock running;
    private ProcessControlBlock lastOnCpu;
    private long tick = 0;
    private long busyTicks = 0;
    private int completedCount = 0;

    private final TimelineBuilder ganttBuilder = new TimelineBuilder();
    private final TimelineBuilder policyBuilder = new TimelineBuilder();
    private final List<PolicySwitch> switches = new ArrayList<>();
    private final List<TickSnapshot> snapshots = new ArrayList<>();

    public SimulationEngine(Workload workload, ExecutionMode mode, SchedulerConfig config, SimLogger logger) {
        this.workload = workload;
        this.mode = mode;
        this.config = config;
        this.logger = logger;
        this.processes = workload.instantiate(config.initialScore());
        this.pendingArrivals = new ArrayList<>(processes);
        this.pendingArrivals.sort(Comparator.comparingInt(ProcessControlBlock::getArrivalTime)
                .thenComparingInt(ProcessControlBlock::getPid));
        this.activeScheduler = (mode == ExecutionMode.PURE_SRTF) ? srtfScheduler : rrScheduler;
        this.controller = (mode == ExecutionMode.ADAPTIVE) ? new AdaptiveController(config, logger) : null;
    }

    /** Runs the whole simulation and returns the results. */
    public SimulationResult run() {
        logger.setPolicy(activeScheduler.policy());
        logger.log(0, EventType.SYSTEM, null, "Simulation start: workload=%s, mode=%s, %s", workload.name(), mode, config.describe());
        long maxTicks = workload.tickUpperBound();
        while (completedCount < processes.size()) {
            if (tick > maxTicks) {
                throw new IllegalStateException("Simulation exceeded its safety bound of " + maxTicks + " ticks");
            }
            stepOneTick();
        }
        captureSnapshot(tick);
        logger.log(tick, EventType.SYSTEM, null, "Simulation finished");
        return buildResult();
    }

    // =====================================================================================
    //  one tick
    // =====================================================================================

    private void stepOneTick() {
        logger.setPolicy(activeScheduler.policy());
        admitArrivals();
        releaseFinishedIo();
        dispatchOrPreempt();
        policyBuilder.append(activeScheduler.policy().name(), tick);
        captureSnapshot(tick);
        accumulateReadyWaiting();
        ProcessControlBlock ran = executeOneTick();
        advanceIoWaiters();
        if (ran != null) {
            handleRunningProcessEvents(ran);
        }
        if (controller != null && completedCount < processes.size()) {
            evaluateAdaptiveController();
        }
        tick++;
    }

    /** Step 1: NEW -> READY. */
    private void admitArrivals() {
        while (nextArrivalIndex < pendingArrivals.size()
                && pendingArrivals.get(nextArrivalIndex).getArrivalTime() <= tick) {
            ProcessControlBlock p = pendingArrivals.get(nextArrivalIndex++);
            makeReady(p);
            logger.log(tick, EventType.ARRIVAL, p.getName(), "%s arrived (first CPU burst %d)", p.getName(), p.getRemainingTime());
        }
    }

    /** Step 2: WAITING -> READY for every process whose I/O finished. */
    private void releaseFinishedIo() {
        List<ProcessControlBlock> done = new ArrayList<>();
        for (ProcessControlBlock p : waitingForIo) {
            if (p.isIoFinished()) {
                done.add(p);
            }
        }
        done.sort(Comparator.comparingInt(ProcessControlBlock::getPid));
        for (ProcessControlBlock p : done) {
            waitingForIo.remove(p);
            p.moveToNextBurst();   // I/O burst -> the following CPU burst
            p.loadCpuBurst();
            makeReady(p);
            logger.log(tick, EventType.IO_COMPLETED, p.getName(), "%s I/O completed (next CPU burst %d)", p.getName(), p.getRemainingTime());
        }
    }

    private void makeReady(ProcessControlBlock p) {
        p.setState(ProcessState.READY);
        p.setCurrentPolicy(activeScheduler.policy());
        activeScheduler.enqueue(p);
    }

    /** Step 3: SRTF preemption check, then fill an empty CPU. */
    private void dispatchOrPreempt() {
        if (running != null && activeScheduler.shouldPreempt(running)) {
            ProcessControlBlock challenger = activeScheduler.peekNext();
            logger.log(tick, EventType.PREEMPTED, running.getName(), "%s preempted by %s (remaining %d < %d)", running.getName(), challenger.getName(),
                    challenger.getRemainingTime(), running.getRemainingTime());
            makeReady(running);
            running = null;
        }
        if (running == null && !activeScheduler.isEmpty()) {
            dispatch(activeScheduler.dequeueNext());
        }
    }

    /** READY -> RUNNING. Also counts context switches and starts a new turn. */
    private void dispatch(ProcessControlBlock p) {
        boolean sameAsBefore = lastOnCpu == p;
        if (lastOnCpu != null && !sameAsBefore) {
            p.incrementContextSwitches();
        }
        p.markFirstDispatch(tick);
        p.setState(ProcessState.RUNNING);
        p.setCurrentPolicy(activeScheduler.policy());
        p.beginTurn(config.rrQuantum());
        running = p;
        lastOnCpu = p;
        if (sameAsBefore) {
            logger.log(tick, EventType.DISPATCHED, p.getName(), "%s continues on the CPU under %s (new slice, remaining %d)",
                    p.getName(), activeScheduler.policy(), p.getRemainingTime());
        } else {
            logger.log(tick, EventType.DISPATCHED, p.getName(), "%s dispatched under %s (CPU remaining %d, slice %d)",
                    p.getName(), activeScheduler.policy(), p.getRemainingTime(), p.getAllocatedQuantumTurn());
        }
    }

    /** Step 4: every process still in a ready queue waited one more tick. */
    private void accumulateReadyWaiting() {
        for (ProcessControlBlock p : activeScheduler.readyProcesses()) {
            p.incrementReadyWait();
        }
    }

    /** Step 5: execute one tick; returns the process that ran (null when idle). */
    private ProcessControlBlock executeOneTick() {
        if (running == null) {
            ganttBuilder.append("idle", tick);
            return null;
        }
        running.runOneTick();
        busyTicks++;
        ganttBuilder.append(running.getName(), tick);
        return running;
    }

    /** Step 6: I/O progresses only for processes that were already waiting when the tick began. */
    private void advanceIoWaiters() {
        for (ProcessControlBlock p : waitingForIo) {
            p.tickIo();
        }
    }

    // =====================================================================================
    //  events of the running process
    // =====================================================================================

    /** Step 7: decide what the tick that just executed meant for the running process. */
    private void handleRunningProcessEvents(ProcessControlBlock p) {
        long now = tick + 1;
        boolean burstDone = p.isCpuBurstFinished();
        boolean sliceUsedUp = p.getExecutedBurst() >= p.getAllocatedQuantumTurn();
        if (burstDone) {
            onCpuBurstCompleted(p, now, sliceUsedUp);
        } else if (sliceUsedUp) {
            onSliceExpired(p, now);
        }
    }

    private void onCpuBurstCompleted(ProcessControlBlock p, long now, boolean exactlyAtSliceEnd) {
        running = null;
        p.moveToNextBurst();
        String boundaryNote = exactlyAtSliceEnd ? " exactly at the slice boundary (a completion, not a quantum expiry)" : "";

        if (!p.hasCurrentBurst()) {
            terminate(p, now, boundaryNote);
            return;
        }
        Burst next = p.currentBurst();
        if (next.type() == BurstType.IO) {
            logger.log(now, EventType.BURST_COMPLETED, p.getName(), "%s CPU burst completed%s", p.getName(), boundaryNote);
            p.startIo();
            p.setState(ProcessState.WAITING);
            waitingForIo.add(p);
            logger.log(now, EventType.IO_STARTED, p.getName(), "%s enters IO for %d ticks", p.getName(), next.duration());
            if (controller != null) {
                controller.observe(p, RuntimeEvent.CPU_BURST_COMPLETED_TO_IO, now);
            }
        } else {
            p.loadCpuBurst();
            logger.log(now, EventType.BURST_COMPLETED, p.getName(), "%s CPU burst completed%s; next burst is CPU (%d), back to READY",
                    p.getName(), boundaryNote, p.getRemainingTime());
            makeReady(p);
        }
    }

    private void terminate(ProcessControlBlock p, long now, String boundaryNote) {
        p.markCompleted(now);
        completedCount++;
        if (p.getWaitingTime() != p.getReadyWaitTicks()) { // internal consistency check
            throw new IllegalStateException(p.getName() + ": waiting formula " + p.getWaitingTime()
                    + " != ticks spent READY " + p.getReadyWaitTicks());
        }
        logger.log(now, EventType.TERMINATED, p.getName(), "%s finished all bursts%s; TERMINATED (turnaround %d, waiting %d)", p.getName(),
                boundaryNote, p.getTurnaroundTime(), p.getWaitingTime());
    }

    /**
     * The slice ran out while the burst is unfinished.
     * Under RR this is a real quantum expiry (RUNNING -> READY, back of the queue).
     * Under SRTF nothing is preempted, but the observation slice is renewed and the event is still reported.
     */
    private void onSliceExpired(ProcessControlBlock p, long now) {
        boolean roundRobin = activeScheduler.policy() == SchedulingPolicy.RR;
        if (roundRobin) {
            logger.log(now, EventType.QUANTUM_EXPIRED, p.getName(), "%s quantum expired", p.getName());
        } else if (controller != null) {
            logger.log(now, EventType.QUANTUM_EXPIRED, p.getName(), "%s used a full observation slice under SRTF (no preemption)", p.getName());
        }
        if (controller != null) {
            controller.observe(p, RuntimeEvent.QUANTUM_EXPIRED, now);
        }
        if (roundRobin) {
            running = null;
            makeReady(p);
        } else {
            p.beginTurn(config.rrQuantum());
        }
    }

    // =====================================================================================
    //  adaptive policy switching
    // =====================================================================================

    /** Step 8: ask the controller, and if it switched, migrate the ready queue to the new policy. */
    private void evaluateAdaptiveController() {
        long now = tick + 1;
        controller.evaluate(now, activeScheduler.size()).ifPresent(this::applyPolicySwitch);
    }

    private void applyPolicySwitch(PolicySwitch sw) {
        List<ProcessControlBlock> ready = activeScheduler.drainAll();
        activeScheduler = (sw.to() == SchedulingPolicy.RR) ? rrScheduler : srtfScheduler;
        for (ProcessControlBlock p : ready) {
            p.setCurrentPolicy(sw.to());
            activeScheduler.enqueue(p);
        }
        if (running != null) {
            running.setCurrentPolicy(sw.to());
        }
        switches.add(sw);
        logger.setPolicy(sw.to());
        logger.log(sw.time(), EventType.MIGRATION, null, "Ready queue migrated: %d process(es) moved to the %s structure", ready.size(), sw.to());
    }

    /** Records the state of the whole system at time {@code time} for the visualisation layer. */
    private void captureSnapshot(long time) {
        List<ProcessSnapshot> states = new ArrayList<>(processes.size());
        for (ProcessControlBlock p : processes) {
            String burstType = p.hasCurrentBurst() ? p.currentBurst().type().name() : "NONE";
            int remaining = p.getState() == ProcessState.WAITING ? p.getIoRemaining()
                    : p.getState() == ProcessState.TERMINATED ? 0 : p.getRemainingTime();
            states.add(new ProcessSnapshot(p.getName(), p.getState(), burstType, remaining));
        }
        boolean adaptive = controller != null;
        snapshots.add(new TickSnapshot(time, running == null ? null : running.getName(), activeScheduler.policy(),
                adaptive ? controller.score() : null, adaptive ? controller.surgeCounter() : null,
                adaptive ? controller.safeUntil() : null, List.copyOf(states)));
    }

    // =====================================================================================
    //  result
    // =====================================================================================

    private SimulationResult buildResult() {
        long total = tick;
        List<TimelineSegment> gantt = ganttBuilder.finish(total);
        List<TimelineSegment> policyTimeline = policyBuilder.finish(total);
        long rr = 0;
        long srtf = 0;
        for (TimelineSegment s : policyTimeline) {
            if (s.label().equals(SchedulingPolicy.RR.name())) {
                rr += s.length();
            } else {
                srtf += s.length();
            }
        }
        MetricsSummary metrics = MetricsCalculator.compute(processes, busyTicks, total);
        return new SimulationResult(workload.name(), mode, List.copyOf(processes), metrics, total, busyTicks,
                switches.size(), rr, srtf, gantt, policyTimeline, List.copyOf(switches), logger.lines(),
                logger.events(), List.copyOf(snapshots));
    }
}
