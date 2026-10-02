package com.adaptiveos.engine.simulation;

import com.adaptiveos.engine.config.SchedulerConfig;
import com.adaptiveos.engine.logging.SimLogger;
import com.adaptiveos.engine.metrics.MetricsSummary;
import com.adaptiveos.engine.visualization.GanttChartPrinter;
import com.adaptiveos.engine.visualization.ResultPrinter;
import com.adaptiveos.engine.workload.Workload;
import com.adaptiveos.engine.workload.WorkloadFactory;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Runs the SAME workload under PURE_RR, PURE_SRTF and ADAPTIVE (each with fresh process copies) and
 * prints comparisons and parameter sweeps. Contains no scheduling logic itself.
 */
public final class ExperimentRunner {

    private ExperimentRunner() { }

    /** Runs all three modes on one workload. Adaptive logging can be switched on. */
    public static Map<ExecutionMode, SimulationResult> runAllModes(Workload workload, SchedulerConfig config,
                                                                   boolean recordAdaptiveLog) {
        Map<ExecutionMode, SimulationResult> results = new EnumMap<>(ExecutionMode.class);
        for (ExecutionMode mode : ExecutionMode.values()) {
            SimLogger logger = (recordAdaptiveLog && mode == ExecutionMode.ADAPTIVE)
                    ? SimLogger.recording() : SimLogger.disabled();
            results.put(mode, new SimulationEngine(workload, mode, config, logger).run());
        }
        return results;
    }

    /** Full report for one workload: per-mode summaries, optional Gantt charts and log, then the comparison. */
    public static void printFullReport(Workload workload, SchedulerConfig config, boolean showLog,
                                       boolean showGantt, PrintStream out) {
        out.println();
        out.println("#".repeat(66));
        out.println("# WORKLOAD: " + workload.name() + " - " + workload.description());
        out.println("# Parameters: " + config.describe());
        out.println("#".repeat(66));
        Map<ExecutionMode, SimulationResult> results = runAllModes(workload, config, showLog);

        for (ExecutionMode mode : ExecutionMode.values()) {
            SimulationResult r = results.get(mode);
            out.println();
            ResultPrinter.printSummary(r, out);
            out.println();
            ResultPrinter.printProcessTable(r, out);
            if (showGantt) {
                out.println();
                GanttChartPrinter.print(r, out);
            }
            if (mode == ExecutionMode.ADAPTIVE && showLog) {
                out.println();
                out.println("Adaptive event log:");
                r.logLines().forEach(line -> out.println("  " + line));
            }
        }
        out.println();
        ResultPrinter.printComparison(workload.name(), results, out);
    }

    /** Compact comparison tables for every built-in workload. */
    public static void printAllWorkloads(SchedulerConfig config, PrintStream out) {
        out.println("Parameters: " + config.describe());
        for (Workload w : WorkloadFactory.all().values()) {
            out.println();
            ResultPrinter.printComparison(w.name(), runAllModes(w, config, false), out);
        }
    }

    // =====================================================================================
    //  parameter sweeps
    // =====================================================================================

    /** The parameter names accepted by the "sweep" command. */
    public static final List<String> SWEEP_NAMES = List.of("alpha", "cooldown", "surge", "antigaming", "quantum");

    /**
     * Varies ONE parameter over the values suggested in the specification, keeping all others at
     * {@code base}. Prints ADAPTIVE metrics per value (plus RR for the quantum sweep, because the quantum
     * also changes pure RR). Does not declare any value optimal.
     */
    public static void sweep(String parameter, List<Workload> workloads, SchedulerConfig base, PrintStream out) {
        List<Double> values;
        BiFunction<SchedulerConfig, Double, SchedulerConfig> apply;
        switch (parameter) {
            case "alpha" -> { values = List.of(0.10, 0.25, 0.50, 0.75); apply = (c, v) -> c.withAlpha(v); }
            case "cooldown" -> { values = List.of(4.0, 8.0, 12.0, 16.0, 20.0); apply = (c, v) -> c.withCooldownTicks(v.intValue()); }
            case "surge" -> { values = List.of(2.0, 3.0, 4.0, 5.0); apply = (c, v) -> c.withSurgeThreshold(v.intValue()); }
            case "antigaming" -> { values = List.of(0.90, 0.95, 0.98); apply = (c, v) -> c.withAntiGamingThreshold(v); }
            case "quantum" -> { values = List.of(2.0, 4.0, 6.0, 8.0); apply = (c, v) -> c.withQuantum(v.intValue()); }
            default -> throw new IllegalArgumentException("Unknown sweep parameter '" + parameter
                    + "'. Choose one of " + SWEEP_NAMES);
        }

        out.println();
        out.println("*".repeat(78));
        out.println("PARAMETER SWEEP: " + parameter + "   (other parameters: " + base.describe() + ")");
        out.println("*".repeat(78));
        for (Workload w : workloads) {
            out.println();
            out.println("Workload: " + w.name());
            out.println(String.format(Locale.ROOT, "%-10s %-9s %9s %11s %10s %6s %6s %9s",
                    parameter, "Mode", "AvgWait", "AvgTurn", "AvgResp", "CtxSw", "Util%", "Switches"));
            for (double v : values) {
                SchedulerConfig cfg = apply.apply(base, v);
                if (parameter.equals("quantum")) {
                    sweepRow(out, v, w, cfg, ExecutionMode.PURE_RR);
                }
                sweepRow(out, v, w, cfg, ExecutionMode.ADAPTIVE);
            }
            if (!parameter.equals("quantum")) {
                sweepRow(out, Double.NaN, w, base, ExecutionMode.PURE_RR);
                sweepRow(out, Double.NaN, w, base, ExecutionMode.PURE_SRTF);
            }
        }
        out.println();
        out.println("(Reference rows with '-' use the base configuration. Values are measurements, not recommendations.)");
    }

    private static void sweepRow(PrintStream out, double value, Workload w, SchedulerConfig cfg, ExecutionMode mode) {
        SimulationResult r = new SimulationEngine(w, mode, cfg, SimLogger.disabled()).run();
        MetricsSummary m = r.metrics();
        String label = Double.isNaN(value) ? "-" : (value == Math.rint(value)
                ? String.valueOf((long) value) : String.format(Locale.ROOT, "%.2f", value));
        out.println(String.format(Locale.ROOT, "%-10s %-9s %9.2f %11.2f %10.2f %6d %6.1f %9d",
                label, mode == ExecutionMode.ADAPTIVE ? "ADAPTIVE" : mode == ExecutionMode.PURE_RR ? "RR" : "SRTF",
                m.avgWaiting(), m.avgTurnaround(), m.avgResponse(), m.totalContextSwitches(),
                m.cpuUtilizationPct(), r.policySwitches()));
    }

    /** Convenience used by the "sweep all" command. */
    public static void sweepAll(List<Workload> workloads, SchedulerConfig base, PrintStream out) {
        for (String name : new ArrayList<>(SWEEP_NAMES)) {
            sweep(name, workloads, base, out);
        }
    }
}
