package com.adaptiveos.engine.metrics;

/**
 * Aggregate scheduling metrics for one simulation run.
 *
 * @param avgWaiting      mean of (turnaround - totalCpu - totalIo)
 * @param avgTurnaround   mean of (completion - arrival)
 * @param avgResponse     mean of (first CPU start - arrival)
 * @param totalContextSwitches number of times the CPU changed from one process to a DIFFERENT process
 * @param avgContextSwitches   totalContextSwitches / process count
 * @param cpuUtilizationPct    100 * busyTicks / totalTicks
 * @param maxWaiting      largest single-process waiting time (a simple, clearly defined starvation indicator)
 * @param processCount    number of processes
 */
public record MetricsSummary(double avgWaiting, double avgTurnaround, double avgResponse,
                             long totalContextSwitches, double avgContextSwitches,
                             double cpuUtilizationPct, long maxWaiting, int processCount) { }
