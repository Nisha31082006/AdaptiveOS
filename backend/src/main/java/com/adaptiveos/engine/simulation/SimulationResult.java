package com.adaptiveos.engine.simulation;

import com.adaptiveos.engine.logging.SimEvent;
import com.adaptiveos.engine.metrics.MetricsSummary;
import com.adaptiveos.engine.model.ProcessControlBlock;
import com.adaptiveos.engine.scheduler.AdaptiveController.PolicySwitch;

import java.util.List;

/**
 * Everything one simulation run produced.
 *
 * @param workloadName   name of the workload
 * @param mode           PURE_RR, PURE_SRTF or ADAPTIVE
 * @param processes      the finished PCBs (sorted by PID) with all per-process metrics
 * @param metrics        aggregate metrics
 * @param totalTime      total simulated ticks (time at which the last process finished)
 * @param busyTime       ticks during which the CPU executed something
 * @param policySwitches number of RR&lt;-&gt;SRTF switches (always 0 for pure modes)
 * @param rrDuration     ticks spent under RR
 * @param srtfDuration   ticks spent under SRTF
 * @param gantt          who ran when (process segments and "idle")
 * @param policyTimeline which policy was active when
 * @param switches       details of every policy switch
 * @param logLines       the readable event log (empty if logging was disabled)
 * @param events         the same log as structured events (empty if logging was disabled)
 * @param snapshots      per-tick system state (process states, running process, adaptive state)
 */
public record SimulationResult(String workloadName, ExecutionMode mode, List<ProcessControlBlock> processes,
                               MetricsSummary metrics, long totalTime, long busyTime, int policySwitches,
                               long rrDuration, long srtfDuration, List<TimelineSegment> gantt,
                               List<TimelineSegment> policyTimeline, List<PolicySwitch> switches,
                               List<String> logLines, List<SimEvent> events,
                               List<TickSnapshot> snapshots) { }
