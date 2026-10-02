package com.adaptiveos.dto;

/** State of one process inside a snapshot. burstType is CPU, IO or NONE. */
public record ProcessStateResponse(String pid, String state, String burstType, int remaining) { }
