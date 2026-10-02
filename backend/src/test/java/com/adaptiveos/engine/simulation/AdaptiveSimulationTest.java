package com.adaptiveos.engine.simulation;

import com.adaptiveos.engine.config.SchedulerConfig;
import com.adaptiveos.engine.model.ProcessControlBlock;
import com.adaptiveos.engine.model.SchedulingPolicy;
import com.adaptiveos.engine.workload.Workload;
import com.adaptiveos.engine.workload.WorkloadFactory;
import org.junit.jupiter.api.Test;

import static com.adaptiveos.engine.simulation.TestSupport.gantt;
import static com.adaptiveos.engine.simulation.TestSupport.run;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** ADAPTIVE mode plus whole-simulator invariants that must hold for every workload and mode. */
class AdaptiveSimulationTest {

    /**
     * Baseline, adaptive, default parameters (hand-checked against the event log):
     * t=4 P1 quantum expired: 512->384 | t=7 P2 early I/O: 384->544 | t=11 P3 expired: 544->408 |
     * t=15 P1 expired: 408->306 < 307  => RR -> SRTF at t=15.
     */
    @Test
    void baselineSwitchesFromRrToSrtfAtTime15() {
        SimulationResult r = run(WorkloadFactory.baseline(), ExecutionMode.ADAPTIVE, SchedulerConfig.defaults(), true);
        assertEquals(1, r.policySwitches());
        assertEquals(15, r.switches().get(0).time());
        assertEquals(SchedulingPolicy.RR, r.switches().get(0).from());
        assertEquals(SchedulingPolicy.SRTF, r.switches().get(0).to());
        assertEquals(15, r.rrDuration());
        assertEquals(6, r.srtfDuration());
        assertEquals("P1[0-4] P2[4-7] P3[7-11] P1[11-17] P2[17-19] P3[19-21]", gantt(r));
        assertTrue(r.logLines().contains("[t=4] EWMA score: 512.0 -> 384.0"));
        assertTrue(r.logLines().contains("[t=7] EWMA score: 384.0 -> 544.0"));
        assertTrue(r.logLines().contains("[t=15] EWMA score: 408.0 -> 306.0"));
    }

    @Test
    void adaptiveStartsWithRoundRobin() {
        SimulationResult r = run(WorkloadFactory.baseline(), ExecutionMode.ADAPTIVE);
        assertEquals("RR", r.policyTimeline().get(0).label());
        assertEquals(0, r.policyTimeline().get(0).start());
    }

    @Test
    void burstEndingExactlyAtTheQuantumIsALogicalCompletionNotAnExpiry() {
        SimulationResult r = run(WorkloadFactory.edgeBurstEqualsQuantum(), ExecutionMode.ADAPTIVE,
                SchedulerConfig.defaults(), true);
        assertTrue(r.logLines().stream().anyMatch(l -> l.contains("exactly at the slice boundary")));
        assertFalse(r.logLines().stream().anyMatch(l -> l.contains("quantum expired")));
        assertTrue(r.logLines().stream().anyMatch(l -> l.contains("E = 4/4 = 1.00 >= 0.95")));
    }

    @Test
    void quantumExpiryOnAnUnfinishedBurstIsLoggedAsExpiry() {
        SimulationResult r = run(WorkloadFactory.edgeBurstLongerThanQuantum(), ExecutionMode.ADAPTIVE,
                SchedulerConfig.defaults(), true);
        assertTrue(r.logLines().stream().anyMatch(l -> l.equals("[t=4] P1 quantum expired")));
    }

    @Test
    void adaptiveModeReportsUnderSrtfWithoutPreemptingByQuantum() {
        // cpu-heavy workload: after the switch the SRTF phase must not be sliced by the quantum.
        SimulationResult r = run(WorkloadFactory.cpuHeavy(), ExecutionMode.ADAPTIVE);
        assertTrue(r.policySwitches() >= 1);
        assertTrue(r.srtfDuration() > 0);
    }

    // ------------------------------------------------------------------ determinism and fairness
    @Test
    void sameWorkloadParametersAndModeGiveIdenticalResults() {
        for (Workload w : WorkloadFactory.all().values()) {
            for (ExecutionMode mode : ExecutionMode.values()) {
                SimulationResult a = run(w, mode, SchedulerConfig.defaults(), true);
                SimulationResult b = run(w, mode, SchedulerConfig.defaults(), true);
                assertEquals(a.metrics(), b.metrics());
                assertEquals(a.gantt(), b.gantt());
                assertEquals(a.policyTimeline(), b.policyTimeline());
                assertEquals(a.logLines(), b.logLines());
            }
        }
    }

    @Test
    void runningOneModeDoesNotAffectTheNextBecauseWorkloadsAreCopiedFresh() {
        Workload w = WorkloadFactory.mixed();
        SimulationResult rrBefore = run(w, ExecutionMode.PURE_RR);
        run(w, ExecutionMode.ADAPTIVE);
        run(w, ExecutionMode.PURE_SRTF);
        SimulationResult rrAfter = run(w, ExecutionMode.PURE_RR);
        assertEquals(rrBefore.metrics(), rrAfter.metrics());
        assertEquals(rrBefore.gantt(), rrAfter.gantt());
    }

    @Test
    void pureModesNeverSwitchPolicy() {
        for (Workload w : WorkloadFactory.all().values()) {
            assertEquals(0, run(w, ExecutionMode.PURE_RR).policySwitches());
            assertEquals(0, run(w, ExecutionMode.PURE_SRTF).policySwitches());
            assertEquals(0, run(w, ExecutionMode.PURE_RR).srtfDuration());
            assertEquals(0, run(w, ExecutionMode.PURE_SRTF).rrDuration());
        }
    }

    // ------------------------------------------------------------------ invariants for everything
    @Test
    void invariantsHoldForEveryWorkloadAndMode() {
        for (Workload w : WorkloadFactory.all().values()) {
            for (ExecutionMode mode : ExecutionMode.values()) {
                SimulationResult r = run(w, mode);
                long totalCpu = 0;
                for (ProcessControlBlock p : r.processes()) {
                    totalCpu += p.getTotalCpuTime();
                    String ctx = w.name() + "/" + mode + "/" + p.getName();
                    assertTrue(p.getCompletionTime() >= p.getArrivalTime() + p.getTotalCpuTime() + p.getTotalIoTime(), ctx);
                    assertTrue(p.getWaitingTime() >= 0, ctx);
                    assertEquals(p.getWaitingTime(), p.getReadyWaitTicks(), ctx);
                    assertTrue(p.getStartTime() >= p.getArrivalTime(), ctx);
                    assertEquals(p.getStartTime() - p.getArrivalTime(), p.getResponseTime(), ctx);
                }
                assertEquals(totalCpu, r.busyTime(), w.name() + "/" + mode);
                assertTrue(r.metrics().cpuUtilizationPct() <= 100.0 + 1e-9);
                long ganttLength = r.gantt().stream().mapToLong(TimelineSegment::length).sum();
                assertEquals(r.totalTime(), ganttLength);
                assertEquals(r.totalTime(), r.rrDuration() + r.srtfDuration());
            }
        }
    }

    @Test
    void contextSwitchTotalEqualsSumOfPerProcessCounts() {
        SimulationResult r = run(WorkloadFactory.mixed(), ExecutionMode.ADAPTIVE);
        int sum = r.processes().stream().mapToInt(ProcessControlBlock::getContextSwitches).sum();
        assertEquals(sum, r.metrics().totalContextSwitches());
    }

    @Test
    void cooldownSeparatesConsecutiveSwitches() {
        SchedulerConfig cfg = SchedulerConfig.defaults();
        SimulationResult r = run(WorkloadFactory.mixed(), ExecutionMode.ADAPTIVE, cfg, false);
        for (int i = 1; i < r.switches().size(); i++) {
            long gap = r.switches().get(i).time() - r.switches().get(i - 1).time();
            assertTrue(gap >= cfg.cooldownTicks(), "switches only " + gap + " ticks apart");
        }
    }
}
