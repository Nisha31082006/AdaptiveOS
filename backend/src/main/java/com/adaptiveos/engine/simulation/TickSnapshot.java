package com.adaptiveos.engine.simulation;

import com.adaptiveos.engine.model.SchedulingPolicy;

import java.util.List;

/**
 * The system state during tick {@code time} (i.e. the interval [time, time+1)), captured after
 * arrivals / I/O completions / dispatch and before the tick executes. One extra final snapshot is
 * recorded at {@code time == totalTime}.
 *
 * @param time          simulated time
 * @param runningPid    process on the CPU, or null when idle
 * @param policy        active scheduling policy
 * @param score         AdaptiveController EWMA score (null unless ADAPTIVE)
 * @param surgeCounter  AdaptiveController surge counter (null unless ADAPTIVE)
 * @param cooldownUntil AdaptiveController safeUntil tick (null unless ADAPTIVE)
 * @param processes     every process's state (sorted by PID)
 */
public record TickSnapshot(long time, String runningPid, SchedulingPolicy policy, Double score,
                           Integer surgeCounter, Long cooldownUntil, List<ProcessSnapshot> processes) { }
