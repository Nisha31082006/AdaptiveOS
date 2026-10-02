package com.adaptiveos.engine.simulation;

import com.adaptiveos.engine.model.Burst;
import com.adaptiveos.engine.workload.ProcessSpec;
import com.adaptiveos.engine.workload.Workload;
import com.adaptiveos.engine.workload.WorkloadFactory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.adaptiveos.engine.simulation.TestSupport.gantt;
import static com.adaptiveos.engine.simulation.TestSupport.run;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PURE_SRTF validated against hand calculations.
 * <pre>
 * baseline: P1[0-1] P2[1-4] P3[4-10] P2[10-12] P1[12-21]
 *   (at t=8 P2 returns from I/O with remaining 2, P3 also has 2 left: tie, running P3 keeps the CPU)
 *   completion  P1=21 P2=12 P3=10
 *   turnaround  21 / 11 / 8   -> avg 13.333
 *   waiting     11 /  2 / 2   -> avg 5.0
 *   response     0 /  0 / 2   -> avg 0.667
 *   context switches = 4
 * </pre>
 */
class SrtfSimulationTest {

    @Test
    void baselineMatchesHandCalculation() {
        SimulationResult r = run(WorkloadFactory.baseline(), ExecutionMode.PURE_SRTF);
        assertEquals("P1[0-1] P2[1-4] P3[4-10] P2[10-12] P1[12-21]", gantt(r));
        assertEquals(5.0, r.metrics().avgWaiting(), 1e-9);
        assertEquals(40.0 / 3, r.metrics().avgTurnaround(), 1e-9);
        assertEquals(2.0 / 3, r.metrics().avgResponse(), 1e-9);
        assertEquals(4, r.metrics().totalContextSwitches());
        assertEquals(21, r.srtfDuration());
        assertEquals(0, r.rrDuration());
    }

    @Test
    void newShorterArrivalPreemptsTheRunningProcess() {
        Workload w = new Workload("preempt", "", List.of(
                ProcessSpec.of(1, 0, Burst.cpu(10)), ProcessSpec.of(2, 2, Burst.cpu(3))));
        SimulationResult r = run(w, ExecutionMode.PURE_SRTF);
        assertEquals("P1[0-2] P2[2-5] P1[5-13]", gantt(r));
    }

    @Test
    void tiesAreBrokenByArrivalThenPidAndRunningProcessIsNotPreemptedOnTie() {
        SimulationResult r = run(WorkloadFactory.edgeSrtfTies(), ExecutionMode.PURE_SRTF);
        assertEquals("P1[0-5] P2[5-10] P3[10-15]", gantt(r));
        assertEquals(2, r.metrics().totalContextSwitches());
    }

    @Test
    void srtfComparesTheCurrentCpuBurstNotTheTotalLifetime() {
        // P2 has total work 32 but its CURRENT CPU burst (2) is shorter than P1's 10, so P2 runs first.
        Workload w = new Workload("current-burst", "", List.of(
                ProcessSpec.of(1, 0, Burst.cpu(10)),
                ProcessSpec.of(2, 0, Burst.cpu(2), Burst.io(5), Burst.cpu(30))));
        SimulationResult r = run(w, ExecutionMode.PURE_SRTF);
        assertTrue(gantt(r).startsWith("P2[0-2] P1[2-12]"));
    }

    @Test
    void processReturningFromIoIsReconsideredWithItsNewBurst() {
        // P2 returns from I/O with a long new burst (20): it must NOT preempt P1 (remaining 7).
        Workload w = new Workload("io-return", "", List.of(
                ProcessSpec.of(1, 0, Burst.cpu(8)),
                ProcessSpec.of(2, 0, Burst.cpu(1), Burst.io(1), Burst.cpu(20))));
        SimulationResult r = run(w, ExecutionMode.PURE_SRTF);
        assertEquals("P2[0-1] P1[1-9] P2[9-29]", gantt(r));
        assertEquals(2, r.metrics().totalContextSwitches());
    }

    @Test
    void allProcessesArrivingAtZero() {
        SimulationResult r = run(WorkloadFactory.edgeAllArriveAtZero(), ExecutionMode.PURE_SRTF);
        assertEquals("P2[0-3] P1[3-8] P3[8-16]", gantt(r));
        assertEquals(11.0 / 3, r.metrics().avgWaiting(), 1e-9);
    }
}
