package com.adaptiveos.dto;

import java.util.List;

/** A predefined workload. {@code id} is the engine's workload name. */
public record WorkloadResponse(String id, String name, String description, String category,
                               List<ProcessRequest> processes) { }
