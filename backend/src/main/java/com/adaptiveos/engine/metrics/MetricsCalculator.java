package com.adaptiveos.engine.metrics;

import com.adaptiveos.engine.model.ProcessControlBlock;

import java.util.List;

/**
 * Turns finished PCBs into a {@link MetricsSummary}. The standard definitions are used everywhere:
 * <pre>
 *   turnaround = completionTime - arrivalTime
 *   response   = firstCpuStartTime - arrivalTime
 *   waiting    = turnaround - totalCpuTime - totalIoTime      (time spent in the READY queue)
 *   CPU util.  = busy ticks / total simulated ticks * 100
 * </pre>
 */
public final class MetricsCalculator {

    private MetricsCalculator() { }

    public static MetricsSummary compute(List<ProcessControlBlock> finished, long busyTicks, long totalTicks) {
        int n = finished.size();
        if (n == 0) {
            throw new IllegalArgumentException("Cannot compute metrics for zero processes");
        }
        double sumWait = 0;
        double sumTurn = 0;
        double sumResp = 0;
        long switches = 0;
        long maxWait = 0;
        for (ProcessControlBlock p : finished) {
            sumWait += p.getWaitingTime();
            sumTurn += p.getTurnaroundTime();
            sumResp += p.getResponseTime();
            switches += p.getContextSwitches();
            maxWait = Math.max(maxWait, p.getWaitingTime());
        }
        double util = totalTicks == 0 ? 0.0 : 100.0 * busyTicks / totalTicks;
        return new MetricsSummary(sumWait / n, sumTurn / n, sumResp / n, switches, (double) switches / n,
                util, maxWait, n);
    }
}
