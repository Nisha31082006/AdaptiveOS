package com.adaptiveos.engine.scheduler;

import com.adaptiveos.engine.model.ProcessControlBlock;
import com.adaptiveos.engine.model.SchedulingPolicy;

import java.util.List;

/**
 * A ready-queue plus the selection rule of one scheduling policy.
 * <p>
 * The simulation engine owns one RR and one SRTF scheduler, but only the ACTIVE one ever holds
 * processes. When AdaptiveOS switches policy, the engine {@link #drainAll() drains} the old scheduler
 * into the new one, so there are never two populated ready queues.
 */
public interface Scheduler {

    SchedulingPolicy policy();

    /** Adds a READY process to this scheduler's ready structure. */
    void enqueue(ProcessControlBlock process);

    /** Removes and returns the process that should run next, or null if none is ready. */
    ProcessControlBlock dequeueNext();

    /** Returns (without removing) the process that would run next, or null. */
    ProcessControlBlock peekNext();

    boolean isEmpty();

    int size();

    /** Read-only view of everything that is ready (used for waiting-time accounting). */
    Iterable<ProcessControlBlock> readyProcesses();

    /** Removes every ready process in this scheduler's own selection order and returns them. */
    List<ProcessControlBlock> drainAll();

    /**
     * Should the currently running process give up the CPU right now because of something in the
     * ready structure? RR never preempts here (its preemption is the quantum, handled by the engine);
     * SRTF preempts when a ready process has a strictly shorter remaining CPU burst.
     */
    boolean shouldPreempt(ProcessControlBlock running);
}
