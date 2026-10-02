package com.adaptiveos.dto;

import java.util.List;

/**
 * System state during tick {@code time}. Adaptive fields are null unless the mode is ADAPTIVE.
 * {@code classification} is derived by fixed rules: score &lt; LOW = CPU-heavy tendency,
 * score &gt; HIGH = interactive tendency, otherwise neutral (inside the hysteresis band).
 */
public record SnapshotResponse(long time, String runningProcessId, String policy, Double interactivityScore,
                               Integer surgeCounter, Long cooldownUntil, boolean cooldownActive,
                               String classification, List<ProcessStateResponse> processes) { }
