package com.adaptiveos.engine.simulation;

import com.adaptiveos.engine.workload.WorkloadFactory;
import org.junit.jupiter.api.Test;

import static com.adaptiveos.engine.simulation.TestSupport.gantt;
import static com.adaptiveos.engine.simulation.TestSupport.proc;
import static com.adaptiveos.engine.simulation.TestSupport.run;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PURE_RR validated against hand calculations.
 * <pre>
 * baseline, Q=4:  P1[0-4] P2[4-7] P3[7-11] P1[11-15] P3[15-17] P2[17-19] P1[19-21]
 *   completion  P1=21 P2=19 P3=17
 *   turnaround  21 / 18 / 15   -> avg 18.00
 *   waiting     11 /  9 /  9   -> avg 9.667   (turnaround - cpu - io)
 *   response     0 /  3 /  5   -> avg 2.667
 *   context switches = 6
 * </pre>
 */
class RoundRobinSimulationTest {

    @Test
    void baselineMatchesHandCalculation() {
        SimulationResult r = run(WorkloadFactory.baseline(), ExecutionMode.PURE_RR);
        assertEquals("P1[0-4] P2[4-7] P3[7-11] P1[11-15] P3[15-17] P2[17-19] P1[19-21]", gantt(r));
        assertEquals(29.0 / 3, r.metrics().avgWaiting(), 1e-9);
        assertEquals(18.0, r.metrics().avgTurnaround(), 1e-9);
        assertEquals(8.0 / 3, r.metrics().avgResponse(), 1e-9);
        assertEquals(6, r.metrics().totalContextSwitches());
        assertEquals(100.0, r.metrics().cpuUtilizationPct(), 1e-9);
        assertEquals(21, r.totalTime());
        assertEquals(0, r.policySwitches());
        assertEquals(21, r.rrDuration());
    }

    @Test
    void perProcessValuesOfTheBaseline() {
        SimulationResult r = run(WorkloadFactory.baseline(), ExecutionMode.PURE_RR);
        assertEquals(21, proc(r, 1).getCompletionTime());
        assertEquals(19, proc(r, 2).getCompletionTime());
        assertEquals(17, proc(r, 3).getCompletionTime());
        assertEquals(9, proc(r, 2).getWaitingTime());   // 18 - 5 cpu - 4 io
        assertEquals(3, proc(r, 2).getResponseTime());  // first ran at 4, arrived at 1
    }

    @Test
    void ioTakesExactlyItsDuration() {
        // P2's CPU burst ends at t=7; its I/O of 4 ticks ends at t=11, where it re-enters the ready queue.
        SimulationResult r = run(WorkloadFactory.baseline(), ExecutionMode.ADAPTIVE,
                com.adaptiveos.engine.config.SchedulerConfig.defaults(), true);
        assertTrue(r.logLines().contains("[t=7] P2 enters IO for 4 ticks"));
        assertTrue(r.logLines().contains("[t=11] P2 I/O completed (next CPU burst 2)"));
    }

    @Test
    void singleProcessRunsWithoutContextSwitchesOrWaiting() {
        SimulationResult r = run(WorkloadFactory.edgeSingleProcess(), ExecutionMode.PURE_RR);
        assertEquals("P1[0-7]", gantt(r));
        assertEquals(0, r.metrics().totalContextSwitches());
        assertEquals(0.0, r.metrics().avgWaiting(), 1e-9);
    }

    @Test
    void allProcessesArrivingAtTimeZero() {
        // P1=5, P2=3, P3=8, Q=4 -> P1[0-4] P2[4-7] P3[7-11] P1[11-12] P3[12-16]
        SimulationResult r = run(WorkloadFactory.edgeAllArriveAtZero(), ExecutionMode.PURE_RR);
        assertEquals("P1[0-4] P2[4-7] P3[7-11] P1[11-12] P3[12-16]", gantt(r));
        assertEquals(19.0 / 3, r.metrics().avgWaiting(), 1e-9);
        assertEquals(4, r.metrics().totalContextSwitches());
    }

    @Test
    void burstOfLengthOne() {
        SimulationResult r = run(WorkloadFactory.edgeBurstLengthOne(), ExecutionMode.PURE_RR);
        assertEquals(4, r.totalTime());
        assertEquals(1.0, r.metrics().avgWaiting(), 1e-9);
    }

    @Test
    void burstExactlyEqualToQuantumCompletesInsteadOfExpiring() {
        // P1 CPU4 IO2 CPU4, P2 CPU4: no process is ever re-queued by quantum expiry.
        SimulationResult r = run(WorkloadFactory.edgeBurstEqualsQuantum(), ExecutionMode.PURE_RR);
        assertEquals("P1[0-4] P2[4-8] P1[8-12]", gantt(r));
    }

    @Test
    void burstLongerThanQuantumIsSlicedRoundRobin() {
        // P1 CPU9, P2 CPU5-IO2-CPU7 : P1[0-4] P2[4-8] P1[8-12] P2[12-13] ...
        SimulationResult r = run(WorkloadFactory.edgeBurstLongerThanQuantum(), ExecutionMode.PURE_RR);
        assertTrue(gantt(r).startsWith("P1[0-4] P2[4-8] P1[8-12] P2[12-13]"));
    }

    @Test
    void simultaneousArrivalAndIoCompletionIsOrderedArrivalFirst() {
        // At t=5 P2 arrives AND P1's I/O completes. Documented order: arrival first, then I/O completion,
        // both behind the process that was just preempted (P3). Queue at t=6: P2, P1, P3.
        SimulationResult r = run(WorkloadFactory.edgeSimultaneousArrivalAndIoCompletion(), ExecutionMode.PURE_RR);
        assertEquals("P1[0-2] P3[2-6] P2[6-9] P1[9-11] P3[11-13]", gantt(r));
        assertEquals(4, proc(r, 1).getWaitingTime());
        assertEquals(1, proc(r, 2).getWaitingTime());
        assertEquals(7, proc(r, 3).getWaitingTime());
    }

    @Test
    void idleGapLowersUtilisation() {
        SimulationResult r = run(WorkloadFactory.edgeIdleGap(), ExecutionMode.PURE_RR);
        assertEquals("P1[0-2] idle[2-10] P2[10-12]", gantt(r));
        assertEquals(100.0 * 4 / 12, r.metrics().cpuUtilizationPct(), 1e-9);
    }
}
