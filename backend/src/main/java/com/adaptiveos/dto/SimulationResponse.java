package com.adaptiveos.dto;

import java.util.List;

/** Everything the frontend needs to visualise one simulation run. */
public record SimulationResponse(String mode, ParametersResponse parameters, MetricsResponse metrics,
                                 List<ProcessResultResponse> processResults, List<GanttEntryResponse> ganttEntries,
                                 List<PolicySegmentResponse> policySegments,
                                 List<PolicyTransitionResponse> policyTransitions, List<EventResponse> events,
                                 List<SnapshotResponse> snapshots, String finalPolicy, long totalTime) { }
