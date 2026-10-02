package com.adaptiveos.dto;

/** A period during which one policy was active. */
public record PolicySegmentResponse(String policy, long startTime, long endTime) { }
