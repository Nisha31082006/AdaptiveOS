package com.adaptiveos.engine.model;

/**
 * The two scheduling policies AdaptiveOS can use. The <b>whole scheduler</b> has exactly one
 * active policy at a time (global policy switching), never one policy per process.
 */
public enum SchedulingPolicy {
    /** Round Robin: FIFO ready queue, fixed time quantum. */
    RR,
    /** Shortest Remaining Time First: preemptive, picks the shortest remaining CPU burst. */
    SRTF
}
