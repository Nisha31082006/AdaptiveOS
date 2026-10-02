package com.adaptiveos.engine.scheduler;

import com.adaptiveos.engine.model.ProcessControlBlock;
import com.adaptiveos.engine.model.SchedulingPolicy;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * Round Robin ready queue: strict FIFO. P1 -> P2 -> P3 -> P1 ...
 * The time quantum itself is enforced by the SimulationEngine (it counts ticks in the current turn);
 * this class only decides ORDER.
 */
public final class RoundRobinScheduler implements Scheduler {

    private final ArrayDeque<ProcessControlBlock> queue = new ArrayDeque<>();

    @Override
    public SchedulingPolicy policy() {
        return SchedulingPolicy.RR;
    }

    @Override
    public void enqueue(ProcessControlBlock process) {
        queue.addLast(process);
    }

    @Override
    public ProcessControlBlock dequeueNext() {
        return queue.pollFirst();
    }

    @Override
    public ProcessControlBlock peekNext() {
        return queue.peekFirst();
    }

    @Override
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    @Override
    public int size() {
        return queue.size();
    }

    @Override
    public Iterable<ProcessControlBlock> readyProcesses() {
        return queue;
    }

    @Override
    public List<ProcessControlBlock> drainAll() {
        List<ProcessControlBlock> drained = new ArrayList<>(queue);
        queue.clear();
        return drained;
    }

    @Override
    public boolean shouldPreempt(ProcessControlBlock running) {
        return false; // RR preempts only when the quantum expires (engine's job)
    }
}
