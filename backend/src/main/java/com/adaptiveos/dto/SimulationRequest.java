package com.adaptiveos.dto;

import java.util.List;

/**
 * Body of POST /api/simulate and POST /api/compare.
 * Every parameter is optional; a missing (null) value falls back to the baseline experimental default.
 * {@code mode} is required for /simulate (PURE_RR | PURE_SRTF | ADAPTIVE) and ignored by /compare.
 */
public record SimulationRequest(String mode, Integer quantum, Double alpha, Double initialScore,
                                Double lowThreshold, Double highThreshold, Integer cooldownTicks,
                                Integer surgeThreshold, Double antiGamingThreshold,
                                List<ProcessRequest> processes) { }
