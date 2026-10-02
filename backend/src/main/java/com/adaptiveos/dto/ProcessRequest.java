package com.adaptiveos.dto;

import java.util.List;

/** One process: {@code {"pid":"P2","arrivalTime":1,"bursts":[...]}}. PID format is "P" followed by a positive number. */
public record ProcessRequest(String pid, Integer arrivalTime, List<BurstRequest> bursts) { }
