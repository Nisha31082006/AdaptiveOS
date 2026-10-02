package com.adaptiveos.service;

import com.adaptiveos.dto.BurstRequest;
import com.adaptiveos.dto.CompareResponse;
import com.adaptiveos.dto.EventResponse;
import com.adaptiveos.dto.GanttEntryResponse;
import com.adaptiveos.dto.MetricsResponse;
import com.adaptiveos.dto.ParametersResponse;
import com.adaptiveos.dto.PolicySegmentResponse;
import com.adaptiveos.dto.PolicyTransitionResponse;
import com.adaptiveos.dto.ProcessRequest;
import com.adaptiveos.dto.ProcessResultResponse;
import com.adaptiveos.dto.ProcessStateResponse;
import com.adaptiveos.dto.SimulationRequest;
import com.adaptiveos.dto.SimulationResponse;
import com.adaptiveos.dto.SnapshotResponse;
import com.adaptiveos.dto.WorkloadResponse;
import com.adaptiveos.engine.config.SchedulerConfig;
import com.adaptiveos.engine.logging.SimEvent;
import com.adaptiveos.engine.logging.SimLogger;
import com.adaptiveos.engine.metrics.MetricsSummary;
import com.adaptiveos.engine.model.Burst;
import com.adaptiveos.engine.model.BurstType;
import com.adaptiveos.engine.model.ProcessControlBlock;
import com.adaptiveos.engine.scheduler.AdaptiveController.PolicySwitch;
import com.adaptiveos.engine.simulation.ExecutionMode;
import com.adaptiveos.engine.simulation.SimulationEngine;
import com.adaptiveos.engine.simulation.SimulationResult;
import com.adaptiveos.engine.simulation.TickSnapshot;
import com.adaptiveos.engine.simulation.TimelineSegment;
import com.adaptiveos.engine.workload.ProcessSpec;
import com.adaptiveos.engine.workload.Workload;
import com.adaptiveos.engine.workload.WorkloadFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The only bridge between the REST layer and the AdaptiveOS engine.
 * <p>
 * It (1) validates the untrusted request, (2) builds an engine workload + configuration, (3) runs the
 * REAL {@link SimulationEngine}, and (4) copies the engine's output into DTOs. It contains NO
 * scheduling logic: every number in a response comes from the engine.
 * <p>
 * This class deliberately has no Spring dependency so it can be unit-tested (and used by the CLI) on its own.
 */
public class SimulationService {

    // Hard limits protect the local server from accidental or malicious huge requests.
    public static final int MAX_PROCESSES = 50;
    public static final int MAX_BURSTS_PER_PROCESS = 101;
    public static final int MAX_BURST_DURATION = 1000;
    public static final int MAX_ARRIVAL_TIME = 10_000;
    public static final int MAX_QUANTUM = 1000;
    public static final int MAX_COOLDOWN = 100_000;
    public static final int MAX_SURGE = 1000;
    public static final long MAX_SIMULATION_TICKS = 100_000;

    private static final Pattern PID_PATTERN = Pattern.compile("^[Pp]?(\\d{1,6})$");
    private static final Set<String> CORE_WORKLOADS = Set.of("baseline", "cpu-heavy", "interactive", "mixed", "phase-change");
    private static final Map<String, String> DISPLAY_NAMES = Map.of(
            "baseline", "Basic Mixed",
            "cpu-heavy", "CPU Heavy",
            "interactive", "Interactive",
            "mixed", "Mixed (CPU-heavy + interactive)",
            "phase-change", "Phase Change");

    // =================================================================== public API

    /** Runs ONE mode. */
    public SimulationResponse simulate(SimulationRequest request) {
        requireBody(request);
        ExecutionMode mode = parseMode(request.mode());
        SchedulerConfig config = buildConfig(request);
        Workload workload = buildWorkload(request.processes());
        return runMode(workload, mode, config);
    }

    /** Runs PURE_RR, PURE_SRTF and ADAPTIVE on identical copies of the same workload and parameters. */
    public CompareResponse compare(SimulationRequest request) {
        requireBody(request);
        SchedulerConfig config = buildConfig(request);
        Workload workload = buildWorkload(request.processes());
        return new CompareResponse(toParameters(config),
                runMode(workload, ExecutionMode.PURE_RR, config),
                runMode(workload, ExecutionMode.PURE_SRTF, config),
                runMode(workload, ExecutionMode.ADAPTIVE, config));
    }

    public List<WorkloadResponse> listWorkloads() {
        List<WorkloadResponse> list = new ArrayList<>();
        for (Workload w : WorkloadFactory.all().values()) {
            list.add(toWorkloadResponse(w));
        }
        return list;
    }

    public WorkloadResponse getWorkload(String id) {
        Workload w = WorkloadFactory.all().get(id);
        if (w == null) {
            throw new NotFoundException("Unknown workload '" + id + "'");
        }
        return toWorkloadResponse(w);
    }

    // =================================================================== running the engine

    private SimulationResponse runMode(Workload workload, ExecutionMode mode, SchedulerConfig config) {
        long bound = workload.tickUpperBound();
        if (bound > MAX_SIMULATION_TICKS) {
            throw new InvalidRequestException("Workload is too large (" + bound + " ticks worst case, limit "
                    + MAX_SIMULATION_TICKS + ")");
        }
        SimulationResult result = new SimulationEngine(workload, mode, config, SimLogger.recording()).run();
        return toResponse(result, config);
    }

    // =================================================================== request -> engine objects

    private static void requireBody(SimulationRequest request) {
        if (request == null) {
            throw new InvalidRequestException("Request body is required");
        }
    }

    static ExecutionMode parseMode(String text) {
        if (text == null || text.isBlank()) {
            throw new InvalidRequestException("mode is required (PURE_RR, PURE_SRTF or ADAPTIVE)");
        }
        try {
            return ExecutionMode.valueOf(text.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("Unknown mode '" + text + "' (use PURE_RR, PURE_SRTF or ADAPTIVE)");
        }
    }

    /** Applies the optional parameters over the baseline defaults, letting the engine validate them. */
    static SchedulerConfig buildConfig(SimulationRequest r) {
        try {
            SchedulerConfig c = SchedulerConfig.defaults();
            if (r.quantum() != null) {
                if (r.quantum() > MAX_QUANTUM) {
                    throw new IllegalArgumentException("RR quantum must be <= " + MAX_QUANTUM);
                }
                c = c.withQuantum(r.quantum());
            }
            if (r.alpha() != null) c = c.withAlpha(r.alpha());
            if (r.initialScore() != null) c = c.withInitialScore(r.initialScore());
            if (r.lowThreshold() != null && r.highThreshold() != null) {
                // apply in an order that never creates a temporary low >= high state
                if (r.highThreshold() > c.highThreshold()) {
                    c = c.withHighThreshold(r.highThreshold()).withLowThreshold(r.lowThreshold());
                } else {
                    c = c.withLowThreshold(r.lowThreshold()).withHighThreshold(r.highThreshold());
                }
            } else {
                if (r.lowThreshold() != null) c = c.withLowThreshold(r.lowThreshold());
                if (r.highThreshold() != null) c = c.withHighThreshold(r.highThreshold());
            }
            if (r.cooldownTicks() != null) {
                if (r.cooldownTicks() > MAX_COOLDOWN) {
                    throw new IllegalArgumentException("Cooldown must be <= " + MAX_COOLDOWN + " ticks");
                }
                c = c.withCooldownTicks(r.cooldownTicks());
            }
            if (r.surgeThreshold() != null) {
                if (r.surgeThreshold() > MAX_SURGE) {
                    throw new IllegalArgumentException("Surge threshold must be <= " + MAX_SURGE);
                }
                c = c.withSurgeThreshold(r.surgeThreshold());
            }
            if (r.antiGamingThreshold() != null) c = c.withAntiGamingThreshold(r.antiGamingThreshold());
            return c;
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException(e.getMessage());
        }
    }

    static Workload buildWorkload(List<ProcessRequest> processes) {
        if (processes == null || processes.isEmpty()) {
            throw new InvalidRequestException("At least one process is required");
        }
        if (processes.size() > MAX_PROCESSES) {
            throw new InvalidRequestException("At most " + MAX_PROCESSES + " processes are allowed");
        }
        List<ProcessSpec> specs = new ArrayList<>();
        Set<Integer> pids = new HashSet<>();
        for (ProcessRequest p : processes) {
            if (p == null) {
                throw new InvalidRequestException("A process entry is null");
            }
            int pid = parsePid(p.pid());
            if (!pids.add(pid)) {
                throw new InvalidRequestException("Duplicate process id P" + pid);
            }
            specs.add(toSpec(pid, p));
        }
        try {
            return new Workload("custom", "User-defined workload", specs);
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException(e.getMessage());
        }
    }

    private static int parsePid(String text) {
        if (text == null) {
            throw new InvalidRequestException("Process id is required");
        }
        Matcher m = PID_PATTERN.matcher(text.trim());
        if (!m.matches() || Integer.parseInt(m.group(1)) <= 0) {
            throw new InvalidRequestException("Invalid process id '" + abbreviate(text)
                    + "' (use the form P1, P2, ... with a positive number)");
        }
        return Integer.parseInt(m.group(1));
    }

    private static ProcessSpec toSpec(int pid, ProcessRequest p) {
        String name = "P" + pid;
        if (p.arrivalTime() == null || p.arrivalTime() < 0 || p.arrivalTime() > MAX_ARRIVAL_TIME) {
            throw new InvalidRequestException(name + ": arrival time must be between 0 and " + MAX_ARRIVAL_TIME);
        }
        if (p.bursts() == null || p.bursts().isEmpty()) {
            throw new InvalidRequestException(name + ": at least one burst is required");
        }
        if (p.bursts().size() > MAX_BURSTS_PER_PROCESS) {
            throw new InvalidRequestException(name + ": at most " + MAX_BURSTS_PER_PROCESS + " bursts allowed");
        }
        List<Burst> bursts = new ArrayList<>();
        for (BurstRequest b : p.bursts()) {
            if (b == null || b.type() == null) {
                throw new InvalidRequestException(name + ": burst type is required (CPU or IO)");
            }
            BurstType type;
            try {
                type = BurstType.valueOf(b.type().trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new InvalidRequestException(name + ": unknown burst type '" + abbreviate(b.type()) + "' (use CPU or IO)");
            }
            if (b.duration() == null || b.duration() <= 0 || b.duration() > MAX_BURST_DURATION) {
                throw new InvalidRequestException(name + ": burst duration must be between 1 and " + MAX_BURST_DURATION);
            }
            bursts.add(new Burst(type, b.duration()));
        }
        try {
            return new ProcessSpec(pid, p.arrivalTime(), bursts);
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException(e.getMessage());
        }
    }

    /** Keeps user-controlled text short before it is echoed in an error message. */
    private static String abbreviate(String s) {
        String clean = s.replaceAll("[\\p{Cntrl}]", "?");
        return clean.length() > 20 ? clean.substring(0, 20) + "..." : clean;
    }

    // =================================================================== engine -> DTO

    SimulationResponse toResponse(SimulationResult r, SchedulerConfig config) {
        List<PolicySegmentResponse> segments = new ArrayList<>();
        for (TimelineSegment s : r.policyTimeline()) {
            segments.add(new PolicySegmentResponse(s.label(), s.start(), s.end()));
        }
        String finalPolicy = segments.get(segments.size() - 1).policy();
        return new SimulationResponse(r.mode().name(), toParameters(config), toMetrics(r),
                toProcessResults(r), toGantt(r), segments, toTransitions(r), toEvents(r),
                toSnapshots(r, config), finalPolicy, r.totalTime());
    }

    private static ParametersResponse toParameters(SchedulerConfig c) {
        return new ParametersResponse(c.rrQuantum(), c.ewmaAlpha(), c.initialScore(), c.lowThreshold(),
                c.highThreshold(), c.cooldownTicks(), c.surgeThreshold(), c.antiGamingThreshold(),
                SchedulerConfig.MAX_SCORE);
    }

    private static MetricsResponse toMetrics(SimulationResult r) {
        MetricsSummary m = r.metrics();
        return new MetricsResponse(m.avgWaiting(), m.avgTurnaround(), m.avgResponse(), m.totalContextSwitches(),
                m.cpuUtilizationPct(), m.maxWaiting(), r.totalTime(), r.busyTime(), r.policySwitches(),
                r.rrDuration(), r.srtfDuration());
    }

    private static List<ProcessResultResponse> toProcessResults(SimulationResult r) {
        List<ProcessResultResponse> list = new ArrayList<>();
        for (ProcessControlBlock p : r.processes()) {
            List<BurstRequest> bursts = new ArrayList<>();
            for (Burst b : p.getBursts()) {
                bursts.add(new BurstRequest(b.type().name(), b.duration()));
            }
            list.add(new ProcessResultResponse(p.getName(), p.getArrivalTime(), p.getStartTime(),
                    p.getCompletionTime(), p.getTurnaroundTime(), p.getWaitingTime(), p.getResponseTime(),
                    p.getContextSwitches(), p.getTotalCpuTime(), p.getTotalIoTime(), bursts));
        }
        return list;
    }

    /**
     * Engine Gantt segments can span a policy switch (a process keeps running while the policy changes).
     * They are cut at policy boundaries so every entry carries exactly one policy; this only SPLITS
     * engine segments, it never invents or moves any execution time.
     */
    private static List<GanttEntryResponse> toGantt(SimulationResult r) {
        List<GanttEntryResponse> list = new ArrayList<>();
        for (TimelineSegment g : r.gantt()) {
            boolean idle = g.label().equals("idle");
            for (TimelineSegment pol : r.policyTimeline()) {
                long start = Math.max(g.start(), pol.start());
                long end = Math.min(g.end(), pol.end());
                if (start < end) {
                    list.add(new GanttEntryResponse(idle ? "IDLE" : g.label(), start, end, pol.label(), idle));
                }
            }
        }
        return list;
    }

    private static List<PolicyTransitionResponse> toTransitions(SimulationResult r) {
        List<PolicyTransitionResponse> list = new ArrayList<>();
        for (PolicySwitch s : r.switches()) {
            list.add(new PolicyTransitionResponse(s.time(), s.from().name(), s.to().name(), s.reason(),
                    s.score(), s.surgeCounter(), s.cooldownUntil()));
        }
        return list;
    }

    private static List<EventResponse> toEvents(SimulationResult r) {
        List<EventResponse> list = new ArrayList<>();
        for (SimEvent e : r.events()) {
            list.add(new EventResponse(e.time(), e.type().name(), e.processId(), e.policy().name(),
                    e.message(), e.type().isAdaptive()));
        }
        return list;
    }

    private static List<SnapshotResponse> toSnapshots(SimulationResult r, SchedulerConfig c) {
        List<SnapshotResponse> list = new ArrayList<>();
        for (TickSnapshot s : r.snapshots()) {
            List<ProcessStateResponse> states = new ArrayList<>();
            s.processes().forEach(p -> states.add(new ProcessStateResponse(p.pid(), p.state().name(),
                    p.burstType(), p.remaining())));
            boolean cooling = s.cooldownUntil() != null && s.time() < s.cooldownUntil();
            list.add(new SnapshotResponse(s.time(), s.runningPid(), s.policy().name(), s.score(), s.surgeCounter(),
                    s.cooldownUntil(), cooling, classify(s.score(), c), states));
        }
        return list;
    }

    /** Fixed, documented rule (no interpretation): compares the score with the LOW / HIGH thresholds. */
    static String classify(Double score, SchedulerConfig c) {
        if (score == null) {
            return null;
        }
        if (score < c.lowThreshold()) {
            return "CPU-heavy tendency";
        }
        if (score > c.highThreshold()) {
            return "Interactive tendency";
        }
        return "Neutral (inside hysteresis band)";
    }

    // =================================================================== workloads

    private static WorkloadResponse toWorkloadResponse(Workload w) {
        List<ProcessRequest> processes = new ArrayList<>();
        for (ProcessSpec s : w.specs()) {
            List<BurstRequest> bursts = new ArrayList<>();
            s.bursts().forEach(b -> bursts.add(new BurstRequest(b.type().name(), b.duration())));
            processes.add(new ProcessRequest("P" + s.pid(), s.arrivalTime(), bursts));
        }
        processes.sort((a, b) -> Integer.compare(Integer.parseInt(a.pid().substring(1)), Integer.parseInt(b.pid().substring(1))));
        String display = DISPLAY_NAMES.getOrDefault(w.name(), prettify(w.name()));
        String category = CORE_WORKLOADS.contains(w.name()) ? "core" : "edge";
        return new WorkloadResponse(w.name(), display, w.description(), category, processes);
    }

    private static String prettify(String id) {
        String text = id.replaceFirst("^edge-", "Edge: ").replace('-', ' ');
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
