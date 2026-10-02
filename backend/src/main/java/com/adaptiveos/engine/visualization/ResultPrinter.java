package com.adaptiveos.engine.visualization;

import com.adaptiveos.engine.metrics.MetricsSummary;
import com.adaptiveos.engine.model.ProcessControlBlock;
import com.adaptiveos.engine.simulation.ExecutionMode;
import com.adaptiveos.engine.simulation.SimulationResult;

import java.io.PrintStream;
import java.util.Locale;
import java.util.Map;

/**
 * Formats simulation results for the console. Deliberately does NOT rank or declare a "winner":
 * it only reports measured numbers.
 */
public final class ResultPrinter {

    private static final String LINE = "=".repeat(60);
    private static final String WIDE_LINE = "=".repeat(66);

    private ResultPrinter() { }

    /** Spec section 37: summary block of one run. */
    public static void printSummary(SimulationResult r, PrintStream out) {
        MetricsSummary m = r.metrics();
        out.println(LINE);
        out.println("Simulation Mode: " + r.mode() + "   (workload: " + r.workloadName() + ")");
        out.println(LINE);
        out.println(String.format(Locale.ROOT, "Average Waiting Time   : %.2f", m.avgWaiting()));
        out.println(String.format(Locale.ROOT, "Average Turnaround     : %.2f", m.avgTurnaround()));
        out.println(String.format(Locale.ROOT, "Average Response Time  : %.2f", m.avgResponse()));
        out.println(String.format(Locale.ROOT, "Context Switches       : %d (avg %.2f per process)",
                m.totalContextSwitches(), m.avgContextSwitches()));
        out.println(String.format(Locale.ROOT, "CPU Utilization        : %.2f%%", m.cpuUtilizationPct()));
        out.println(String.format(Locale.ROOT, "Max Waiting (1 proc)   : %d", m.maxWaiting()));
        out.println(String.format(Locale.ROOT, "Total Simulated Ticks  : %d", r.totalTime()));
        out.println();
        out.println(String.format(Locale.ROOT, "Policy Switches        : %d", r.policySwitches()));
        out.println(String.format(Locale.ROOT, "RR Duration            : %d ticks", r.rrDuration()));
        out.println(String.format(Locale.ROOT, "SRTF Duration          : %d ticks", r.srtfDuration()));
        r.switches().forEach(s -> out.println(String.format(Locale.ROOT, "  t=%d: %s -> %s (%s)",
                s.time(), s.from(), s.to(), s.reason())));
    }

    /** Per-process table. */
    public static void printProcessTable(SimulationResult r, PrintStream out) {
        out.println(String.format(Locale.ROOT, "%-5s %7s %6s %11s %11s %8s %9s %6s",
                "PID", "Arrival", "Start", "Completion", "Turnaround", "Waiting", "Response", "CtxSw"));
        for (ProcessControlBlock p : r.processes()) {
            out.println(String.format(Locale.ROOT, "%-5s %7d %6d %11d %11d %8d %9d %6d",
                    p.getName(), p.getArrivalTime(), p.getStartTime(), p.getCompletionTime(),
                    p.getTurnaroundTime(), p.getWaitingTime(), p.getResponseTime(), p.getContextSwitches()));
        }
    }

    /** Spec section 37: side-by-side comparison. No "best" column, no ranking. */
    public static void printComparison(String workloadName, Map<ExecutionMode, SimulationResult> results,
                                       PrintStream out) {
        MetricsSummary rr = results.get(ExecutionMode.PURE_RR).metrics();
        MetricsSummary sr = results.get(ExecutionMode.PURE_SRTF).metrics();
        SimulationResult adaptive = results.get(ExecutionMode.ADAPTIVE);
        MetricsSummary ad = adaptive.metrics();

        out.println(WIDE_LINE);
        out.println("Comparison for workload: " + workloadName);
        out.println(WIDE_LINE);
        out.println(String.format(Locale.ROOT, "%-18s %12s %12s %12s", "", "RR", "SRTF", "ADAPTIVE"));
        out.println("-".repeat(58));
        row(out, "Avg Waiting", rr.avgWaiting(), sr.avgWaiting(), ad.avgWaiting());
        row(out, "Avg Turnaround", rr.avgTurnaround(), sr.avgTurnaround(), ad.avgTurnaround());
        row(out, "Avg Response", rr.avgResponse(), sr.avgResponse(), ad.avgResponse());
        out.println(String.format(Locale.ROOT, "%-18s %12d %12d %12d", "Max Waiting", rr.maxWaiting(), sr.maxWaiting(), ad.maxWaiting()));
        out.println(String.format(Locale.ROOT, "%-18s %12d %12d %12d", "Context Switches",
                rr.totalContextSwitches(), sr.totalContextSwitches(), ad.totalContextSwitches()));
        out.println(String.format(Locale.ROOT, "%-18s %11.2f%% %11.2f%% %11.2f%%", "CPU Utilization",
                rr.cpuUtilizationPct(), sr.cpuUtilizationPct(), ad.cpuUtilizationPct()));
        out.println(String.format(Locale.ROOT, "%-18s %12s %12s %12d", "Policy Switches", "-", "-",
                adaptive.policySwitches()));
        out.println(String.format(Locale.ROOT, "%-18s %12s %12s %12s", "ADAPTIVE RR/SRTF", "", "",
                adaptive.rrDuration() + "/" + adaptive.srtfDuration()));
        out.println(WIDE_LINE);
        out.println("(Numbers are measurements only; no policy is ranked. Lower is better for waiting/turnaround/response.)");
    }

    private static void row(PrintStream out, String label, double a, double b, double c) {
        out.println(String.format(Locale.ROOT, "%-18s %12.2f %12.2f %12.2f", label, a, b, c));
    }
}
