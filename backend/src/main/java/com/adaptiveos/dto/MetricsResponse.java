package com.adaptiveos.dto;

/** Aggregate metrics exactly as computed by the Java engine (percent for cpuUtilization). */
public record MetricsResponse(double averageWaitingTime, double averageTurnaroundTime, double averageResponseTime,
                              long contextSwitches, double cpuUtilization, long maxWaitingTime, long totalTime,
                              long busyTime, int policySwitches, long rrDuration, long srtfDuration) { }
