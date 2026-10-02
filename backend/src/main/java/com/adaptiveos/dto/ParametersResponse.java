package com.adaptiveos.dto;

/** The parameters that were actually used (defaults filled in). */
public record ParametersResponse(int quantum, double alpha, double initialScore, double lowThreshold,
                                 double highThreshold, int cooldownTicks, int surgeThreshold,
                                 double antiGamingThreshold, double maxScore) { }
