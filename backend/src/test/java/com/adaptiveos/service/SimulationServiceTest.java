package com.adaptiveos.service;

import com.adaptiveos.dto.BurstRequest;
import com.adaptiveos.dto.CompareResponse;
import com.adaptiveos.dto.GanttEntryResponse;
import com.adaptiveos.dto.ProcessRequest;
import com.adaptiveos.dto.SimulationRequest;
import com.adaptiveos.dto.SimulationResponse;
import com.adaptiveos.dto.SnapshotResponse;
import com.adaptiveos.engine.config.SchedulerConfig;
import com.adaptiveos.engine.logging.SimLogger;
import com.adaptiveos.engine.simulation.ExecutionMode;
import com.adaptiveos.engine.simulation.SimulationEngine;
import com.adaptiveos.engine.simulation.SimulationResult;
import com.adaptiveos.engine.simulation.TimelineSegment;
import com.adaptiveos.engine.workload.Workload;
import com.adaptiveos.engine.workload.WorkloadFactory;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the API layer against the engine. The key idea: whatever the service returns must equal what
 * the standalone Java engine produces for the same workload, mode and parameters.
 */
class SimulationServiceTest {

    private final SimulationService service = new SimulationService();

    // ------------------------------------------------------------------ helpers
    private static ProcessRequest proc(String pid, int arrival, Object... bursts) {
        List<BurstRequest> list = new ArrayList<>();
        for (int i = 0; i < bursts.length; i += 2) {
            list.add(new BurstRequest((String) bursts[i], (Integer) bursts[i + 1]));
        }
        return new ProcessRequest(pid, arrival, list);
    }

    private static SimulationRequest request(String mode, List<ProcessRequest> processes) {
        return new SimulationRequest(mode, null, null, null, null, null, null, null, null, processes);
    }

    private List<ProcessRequest> workloadProcesses(String id) {
        return service.getWorkload(id).processes();
    }

    private static SimulationResult engineRun(Workload w, ExecutionMode mode, SchedulerConfig cfg) {
        return new SimulationEngine(w, mode, cfg, SimLogger.recording()).run();
    }

    private static List<String> perTick(List<GanttEntryResponse> gantt) {
        List<String> ticks = new ArrayList<>();
        for (GanttEntryResponse g : gantt) {
            for (long t = g.startTime(); t < g.endTime(); t++) {
                ticks.add(g.processId());
            }
        }
        return ticks;
    }

    private static List<String> perTickEngine(SimulationResult r) {
        List<String> ticks = new ArrayList<>();
        for (TimelineSegment s : r.gantt()) {
            for (long t = s.start(); t < s.end(); t++) {
                ticks.add(s.label().equals("idle") ? "IDLE" : s.label());
            }
        }
        return ticks;
    }

    private static void assertInvalid(SimulationService svc, SimulationRequest req, String expectedFragment) {
        InvalidRequestException e = assertThrows(InvalidRequestException.class, () -> svc.simulate(req));
        assertTrue(e.getMessage().toLowerCase().contains(expectedFragment.toLowerCase()),
                "message '" + e.getMessage() + "' should contain '" + expectedFragment + "'");
    }

    // ------------------------------------------------------------------ parity with the standalone engine
    @Test
    void everyWorkloadAndModeMatchesTheStandaloneEngine() {
        SchedulerConfig cfg = SchedulerConfig.defaults();
        for (Workload w : WorkloadFactory.all().values()) {
            for (ExecutionMode mode : ExecutionMode.values()) {
                SimulationResponse api = service.simulate(request(mode.name(), workloadProcesses(w.name())));
                SimulationResult engine = engineRun(w, mode, cfg);
                String ctx = w.name() + "/" + mode;

                assertEquals(engine.metrics().avgWaiting(), api.metrics().averageWaitingTime(), 1e-12, ctx);
                assertEquals(engine.metrics().avgTurnaround(), api.metrics().averageTurnaroundTime(), 1e-12, ctx);
                assertEquals(engine.metrics().avgResponse(), api.metrics().averageResponseTime(), 1e-12, ctx);
                assertEquals(engine.metrics().totalContextSwitches(), api.metrics().contextSwitches(), ctx);
                assertEquals(engine.metrics().cpuUtilizationPct(), api.metrics().cpuUtilization(), 1e-12, ctx);
                assertEquals(engine.totalTime(), api.totalTime(), ctx);
                assertEquals(engine.policySwitches(), api.policyTransitions().size(), ctx);
                assertEquals(perTickEngine(engine), perTick(api.ganttEntries()), ctx);
                assertEquals(engine.logLines().size(), api.events().size(), ctx);
                for (int i = 0; i < engine.processes().size(); i++) {
                    assertEquals(engine.processes().get(i).getWaitingTime(), api.processResults().get(i).waitingTime(), ctx);
                    assertEquals(engine.processes().get(i).getCompletionTime(), api.processResults().get(i).completionTime(), ctx);
                }
            }
        }
    }

    @Test
    void parametersAreForwardedToTheEngine() {
        SimulationRequest req = new SimulationRequest("ADAPTIVE", 6, 0.5, 600.0, 250.0, 800.0, 8, 4, 0.9,
                workloadProcesses("mixed"));
        SchedulerConfig cfg = SchedulerConfig.defaults().withQuantum(6).withAlpha(0.5).withInitialScore(600)
                .withHighThreshold(800).withLowThreshold(250).withCooldownTicks(8).withSurgeThreshold(4)
                .withAntiGamingThreshold(0.9);
        SimulationResponse api = service.simulate(req);
        SimulationResult engine = engineRun(WorkloadFactory.mixed(), ExecutionMode.ADAPTIVE, cfg);
        assertEquals(engine.metrics().avgWaiting(), api.metrics().averageWaitingTime(), 1e-12);
        assertEquals(engine.policySwitches(), api.metrics().policySwitches());
        assertEquals(6, api.parameters().quantum());
        assertEquals(0.5, api.parameters().alpha(), 1e-12);
    }

    @Test
    void defaultsAreTheBaselineParameters() {
        SimulationResponse api = service.simulate(request("PURE_RR", workloadProcesses("baseline")));
        assertEquals(4, api.parameters().quantum());
        assertEquals(0.25, api.parameters().alpha(), 1e-12);
        assertEquals(512.0, api.parameters().initialScore(), 1e-12);
        assertEquals(307.0, api.parameters().lowThreshold(), 1e-12);
        assertEquals(716.0, api.parameters().highThreshold(), 1e-12);
        assertEquals(12, api.parameters().cooldownTicks());
        assertEquals(3, api.parameters().surgeThreshold());
        assertEquals(0.95, api.parameters().antiGamingThreshold(), 1e-12);
    }

    // ------------------------------------------------------------------ hand-checked baseline values
    @Test
    void baselineRoundRobinAndSrtfMatchHandCalculations() {
        SimulationResponse rr = service.simulate(request("PURE_RR", workloadProcesses("baseline")));
        assertEquals(29.0 / 3, rr.metrics().averageWaitingTime(), 1e-9);
        assertEquals(18.0, rr.metrics().averageTurnaroundTime(), 1e-9);
        assertEquals(6, rr.metrics().contextSwitches());
        SimulationResponse srtf = service.simulate(request("PURE_SRTF", workloadProcesses("baseline")));
        assertEquals(5.0, srtf.metrics().averageWaitingTime(), 1e-9);
        assertEquals(4, srtf.metrics().contextSwitches());
        assertEquals("SRTF", srtf.finalPolicy());
    }

    @Test
    void adaptiveBaselineExposesTheRealPolicyTransition() {
        SimulationResponse a = service.simulate(request("ADAPTIVE", workloadProcesses("baseline")));
        assertEquals(1, a.policyTransitions().size());
        var t = a.policyTransitions().get(0);
        assertEquals(15, t.time());
        assertEquals("RR", t.fromPolicy());
        assertEquals("SRTF", t.toPolicy());
        assertEquals(306.0, t.interactivityScore(), 1e-9);
        assertEquals(2, t.surgeCounter());
        assertEquals(27, t.cooldownUntil());
        assertTrue(t.reason().contains("LOW"));
        assertEquals("SRTF", a.finalPolicy());
        assertEquals(15, a.metrics().rrDuration());
        assertEquals(6, a.metrics().srtfDuration());
    }

    @Test
    void ganttEntriesAreSplitAtPolicyBoundaries() {
        SimulationResponse a = service.simulate(request("ADAPTIVE", workloadProcesses("baseline")));
        // engine segment P1[11-17] spans the switch at t=15 and must be cut there
        boolean rrPart = a.ganttEntries().stream().anyMatch(g -> g.processId().equals("P1")
                && g.startTime() == 11 && g.endTime() == 15 && g.policy().equals("RR"));
        boolean srtfPart = a.ganttEntries().stream().anyMatch(g -> g.processId().equals("P1")
                && g.startTime() == 15 && g.endTime() == 17 && g.policy().equals("SRTF"));
        assertTrue(rrPart && srtfPart);
    }

    @Test
    void idlePeriodsAreMarked() {
        SimulationResponse r = service.simulate(request("PURE_RR", workloadProcesses("edge-idle-gap")));
        assertTrue(r.ganttEntries().stream().anyMatch(g -> g.idle() && g.processId().equals("IDLE")
                && g.startTime() == 2 && g.endTime() == 10));
    }

    // ------------------------------------------------------------------ snapshots / events
    @Test
    void snapshotsDescribeTheSystemOverTime() {
        SimulationResponse a = service.simulate(request("ADAPTIVE", workloadProcesses("baseline")));
        SnapshotResponse first = a.snapshots().get(0);
        assertEquals(0, first.time());
        assertEquals("P1", first.runningProcessId());
        assertEquals(3, first.processes().size());
        assertEquals("NEW", first.processes().get(1).state());        // P2 arrives at t=1
        assertEquals(512.0, first.interactivityScore(), 1e-9);

        SnapshotResponse at4 = a.snapshots().get(4);
        assertEquals(4, at4.time());
        assertEquals(384.0, at4.interactivityScore(), 1e-9);           // 512 -> 384 after P1's quantum expired
        assertEquals(1, at4.surgeCounter());
        assertEquals("P2", at4.runningProcessId());

        SnapshotResponse at15 = a.snapshots().get(15);
        assertEquals("SRTF", at15.policy());
        assertEquals("CPU-heavy tendency", at15.classification());     // 306 < LOW 307
        assertTrue(at15.cooldownActive());

        SnapshotResponse last = a.snapshots().get(a.snapshots().size() - 1);
        assertEquals(a.totalTime(), last.time());
        assertTrue(last.processes().stream().allMatch(p -> p.state().equals("TERMINATED")));
    }

    @Test
    void pureModesHaveNoAdaptiveState() {
        SimulationResponse rr = service.simulate(request("PURE_RR", workloadProcesses("baseline")));
        assertNull(rr.snapshots().get(3).interactivityScore());
        assertNull(rr.snapshots().get(3).classification());
        assertTrue(rr.policyTransitions().isEmpty());
        assertTrue(rr.events().stream().noneMatch(e -> e.adaptive()));
    }

    @Test
    void eventsAreStructuredAndTagged() {
        SimulationResponse a = service.simulate(request("ADAPTIVE", workloadProcesses("baseline")));
        assertTrue(a.events().stream().anyMatch(e -> e.type().equals("QUANTUM_EXPIRED") && e.time() == 4
                && "P1".equals(e.processId())));
        assertTrue(a.events().stream().anyMatch(e -> e.type().equals("EWMA_UPDATE") && e.adaptive()
                && e.message().equals("EWMA score: 512.0 -> 384.0")));
        assertTrue(a.events().stream().anyMatch(e -> e.type().equals("POLICY_SWITCH") && e.time() == 15));
        assertTrue(a.events().stream().anyMatch(e -> e.type().equals("IO_STARTED") && "P2".equals(e.processId())));
    }

    // ------------------------------------------------------------------ compare
    @Test
    void compareRunsAllThreeModesOnTheSameWorkload() {
        CompareResponse c = service.compare(request(null, workloadProcesses("mixed")));
        assertEquals("PURE_RR", c.pureRr().mode());
        assertEquals("PURE_SRTF", c.pureSrtf().mode());
        assertEquals("ADAPTIVE", c.adaptive().mode());
        assertEquals(c.pureRr().processResults().size(), c.adaptive().processResults().size());
        SimulationResponse single = service.simulate(request("PURE_RR", workloadProcesses("mixed")));
        assertEquals(single.metrics(), c.pureRr().metrics());
        assertEquals(service.simulate(request("ADAPTIVE", workloadProcesses("mixed"))).metrics(), c.adaptive().metrics());
    }

    @Test
    void resultsAreDeterministic() {
        SimulationResponse a = service.simulate(request("ADAPTIVE", workloadProcesses("phase-change")));
        SimulationResponse b = service.simulate(request("ADAPTIVE", workloadProcesses("phase-change")));
        assertEquals(a.metrics(), b.metrics());
        assertEquals(a.ganttEntries(), b.ganttEntries());
        assertEquals(a.events(), b.events());
    }

    // ------------------------------------------------------------------ workloads
    @Test
    void predefinedWorkloadsAreListedAndRunnable() {
        var list = service.listWorkloads();
        assertTrue(list.size() >= 16);
        assertTrue(list.stream().filter(w -> w.category().equals("core")).count() == 5);
        for (var w : list) {
            assertNotNull(service.simulate(request("ADAPTIVE", w.processes())));
        }
        assertEquals("Basic Mixed", service.getWorkload("baseline").name());
        assertThrows(NotFoundException.class, () -> service.getWorkload("nope"));
    }

    // ------------------------------------------------------------------ validation
    @Test
    void invalidProcessDataIsRejected() {
        List<ProcessRequest> ok = workloadProcesses("baseline");
        assertInvalid(service, request("ADAPTIVE", List.of()), "at least one process");
        assertInvalid(service, request("ADAPTIVE", null), "at least one process");
        assertInvalid(service, request("ADAPTIVE", List.of(proc("P1", 0, "CPU", 3), proc("P1", 1, "CPU", 2))), "duplicate");
        assertInvalid(service, request("ADAPTIVE", List.of(proc("P1", 0, "CPU", -3))), "duration");
        assertInvalid(service, request("ADAPTIVE", List.of(proc("P1", 0, "CPU", 0))), "duration");
        assertInvalid(service, request("ADAPTIVE", List.of(proc("P1", -1, "CPU", 3))), "arrival");
        assertInvalid(service, request("ADAPTIVE", List.of(proc("P1", 0, "CPU", 1, "IO", 2))), "last burst");
        assertInvalid(service, request("ADAPTIVE", List.of(proc("P1", 0, "IO", 2, "CPU", 1))), "first burst");
        assertInvalid(service, request("ADAPTIVE", List.of(proc("P1", 0, "GPU", 2))), "burst type");
        assertInvalid(service, request("ADAPTIVE", List.of(proc("PX", 0, "CPU", 2))), "invalid process id");
        assertInvalid(service, request("ADAPTIVE", List.of(proc("P0", 0, "CPU", 2))), "invalid process id");
        assertInvalid(service, request("ADAPTIVE", List.of(new ProcessRequest("P1", 0, List.of()))), "at least one burst");
        assertInvalid(service, request("ADAPTIVE", List.of(new ProcessRequest("P1", null, List.of(new BurstRequest("CPU", 1))))), "arrival");
        assertInvalid(service, request("ADAPTIVE", List.of(proc("P1", 0, "CPU", 1, "IO", 1, "IO", 1, "CPU", 1))), "two i/o");
        assertFalse(ok.isEmpty());
    }

    @Test
    void invalidModeAndParametersAreRejected() {
        List<ProcessRequest> ps = workloadProcesses("baseline");
        assertInvalid(service, request(null, ps), "mode is required");
        assertInvalid(service, request("FCFS", ps), "unknown mode");
        assertInvalid(service, new SimulationRequest("ADAPTIVE", 0, null, null, null, null, null, null, null, ps), "quantum");
        assertInvalid(service, new SimulationRequest("ADAPTIVE", null, 1.5, null, null, null, null, null, null, ps), "alpha");
        assertInvalid(service, new SimulationRequest("ADAPTIVE", null, 0.0, null, null, null, null, null, null, ps), "alpha");
        assertInvalid(service, new SimulationRequest("ADAPTIVE", null, Double.NaN, null, null, null, null, null, null, ps), "alpha");
        assertInvalid(service, new SimulationRequest("ADAPTIVE", null, null, null, 800.0, 700.0, null, null, null, ps), "smaller than high");
        assertInvalid(service, new SimulationRequest("ADAPTIVE", null, null, null, null, 2000.0, null, null, null, ps), "high threshold");
        assertInvalid(service, new SimulationRequest("ADAPTIVE", null, null, null, -5.0, null, null, null, null, ps), "low threshold");
        assertInvalid(service, new SimulationRequest("ADAPTIVE", null, null, 5000.0, null, null, null, null, null, ps), "initial score");
        assertInvalid(service, new SimulationRequest("ADAPTIVE", null, null, null, null, null, -1, null, null, ps), "cooldown");
        assertInvalid(service, new SimulationRequest("ADAPTIVE", null, null, null, null, null, null, 0, null, ps), "surge");
        assertInvalid(service, new SimulationRequest("ADAPTIVE", null, null, null, null, null, null, null, 1.5, ps), "anti-gaming");
        assertThrows(InvalidRequestException.class, () -> service.simulate(null));
    }

    @Test
    void bothThresholdsMayMoveTogetherInEitherDirection() {
        List<ProcessRequest> ps = workloadProcesses("baseline");
        assertNotNull(service.simulate(new SimulationRequest("ADAPTIVE", null, null, null, 800.0, 900.0, null, null, null, ps)));
        assertNotNull(service.simulate(new SimulationRequest("ADAPTIVE", null, null, null, 100.0, 200.0, null, null, null, ps)));
    }

    @Test
    void oversizedRequestsAreRejected() {
        List<ProcessRequest> many = new ArrayList<>();
        for (int i = 1; i <= SimulationService.MAX_PROCESSES + 1; i++) {
            many.add(proc("P" + i, 0, "CPU", 1));
        }
        assertInvalid(service, request("PURE_RR", many), "at most");
        assertInvalid(service, request("PURE_RR", List.of(proc("P1", 0, "CPU", SimulationService.MAX_BURST_DURATION + 1))), "duration");
    }

    @Test
    void userControlledTextIsSanitisedInErrors() {
        InvalidRequestException e = assertThrows(InvalidRequestException.class,
                () -> service.simulate(request("PURE_RR", List.of(proc("<script>alert(1)</script>\n\u0000", 0, "CPU", 1)))));
        assertFalse(e.getMessage().contains("\n"));
        assertFalse(e.getMessage().contains("\u0000"));
        assertTrue(e.getMessage().length() < 200);
    }
}
