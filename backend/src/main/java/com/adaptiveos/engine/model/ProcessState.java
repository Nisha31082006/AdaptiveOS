package com.adaptiveos.engine.model;

/**
 * Lifecycle states of a process.
 * <pre>
 * NEW -> READY -> RUNNING -> TERMINATED
 *          ^        |  \
 *          |        |   +--> WAITING (I/O) --+
 *          +--------+  (preempted / quantum) |
 *          ^-------------------------------- +
 * </pre>
 */
public enum ProcessState {
    /** Created but its arrival time has not been reached yet. */
    NEW,
    /** In a ready queue, waiting for the CPU. */
    READY,
    /** Currently executing on the CPU (only one process at a time). */
    RUNNING,
    /** Blocked on an I/O burst. */
    WAITING,
    /** All bursts completed. */
    TERMINATED
}
