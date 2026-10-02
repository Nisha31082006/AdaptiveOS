package com.adaptiveos.dto;

/** One structured engine event. {@code adaptive} is true for AdaptiveController events. */
public record EventResponse(long time, String type, String processId, String policy, String message,
                            boolean adaptive) { }
