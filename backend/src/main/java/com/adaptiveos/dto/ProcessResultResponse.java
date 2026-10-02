package com.adaptiveos.dto;

import java.util.List;

/** Final per-process numbers from the engine. */
public record ProcessResultResponse(String pid, int arrivalTime, long startTime, long completionTime,
                                    long turnaroundTime, long waitingTime, long responseTime, int contextSwitches,
                                    int totalCpuTime, int totalIoTime, List<BurstRequest> bursts) { }
