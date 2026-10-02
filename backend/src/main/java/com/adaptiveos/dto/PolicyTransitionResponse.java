package com.adaptiveos.dto;

/** One global policy switch and the adaptive state at that moment, all taken from the engine. */
public record PolicyTransitionResponse(long time, String fromPolicy, String toPolicy, String reason,
                                       double interactivityScore, int surgeCounter, long cooldownUntil) { }
