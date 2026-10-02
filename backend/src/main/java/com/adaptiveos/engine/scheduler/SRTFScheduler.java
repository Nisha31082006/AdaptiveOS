package com.adaptiveos.engine.scheduler;

import com.adaptiveos.engine.model.ProcessControlBlock;
import com.adaptiveos.engine.model.SchedulingPolicy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Shortest Remaining Time First (preemptive). Ready processes live in a PriorityQueue ordered by
 * <ol>
 *   <li>smaller remaining time of the CURRENT CPU burst,</li>
 *   <li>earlier arrival time,</li>
 *   <li>smaller PID</li>
 * </ol>
 * so the result is always deterministic. A queued process's remaining time never changes while it
 * waits (only the running process executes), so the heap order stays valid.
 */
public final class SRTFScheduler implements Scheduler {

    /** The SRTF ordering rule described in the class comment. */
    public static final Comparator<ProcessControlBlock> ORDER =
            Comparator.comparingInt(ProcessControlBlock::getRemainingTime)
                    .thenComparingInt(ProcessControlBlock::getArrivalTime)
                    .thenComparingInt(ProcessControlBlock::getPid);

    private final PriorityQueue<ProcessControlBlock> queue = new PriorityQueue<>(ORDER);

    @Override
    public SchedulingPolicy policy() {
        return SchedulingPolicy.SRTF;
    }

    @Override
    public void enqueue(ProcessControlBlock process) {
        queue.add(process);
    }

    @Override
    public ProcessControlBlock dequeueNext() {
        return queue.poll();
    }

    @Override
    public ProcessControlBlock peekNext() {
        return queue.peek();
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
        List<ProcessControlBlock> drained = new ArrayList<>(queue.size());
        while (!queue.isEmpty()) {
            drained.add(queue.poll());
        }
        return drained;
    }

    /**
     * Preempt only when a ready process is STRICTLY shorter than the running one. On an exact tie the
     * running process keeps the CPU, which avoids pointless context switches.
     */
    @Override
    public boolean shouldPreempt(ProcessControlBlock running) {
        ProcessControlBlock best = queue.peek();
        return best != null && best.getRemainingTime() < running.getRemainingTime();
    }
}
