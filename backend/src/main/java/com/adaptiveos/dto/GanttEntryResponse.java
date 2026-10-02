package com.adaptiveos.dto;

/** A [startTime, endTime) block on the CPU. processId is "IDLE" (idle=true) when nothing ran. */
public record GanttEntryResponse(String processId, long startTime, long endTime, String policy, boolean idle) { }
