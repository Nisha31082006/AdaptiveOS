package com.adaptiveos.engine.scheduler;

import com.adaptiveos.engine.model.Burst;
import com.adaptiveos.engine.model.ProcessControlBlock;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchedulerQueueTest {

    private static ProcessControlBlock pcb(int pid, int arrival, int cpu) {
        return new ProcessControlBlock(pid, arrival, List.of(Burst.cpu(cpu)), 512);
    }

    @Test
    void roundRobinIsStrictFifo() {
        RoundRobinScheduler rr = new RoundRobinScheduler();
        rr.enqueue(pcb(3, 0, 9));
        rr.enqueue(pcb(1, 0, 1));
        rr.enqueue(pcb(2, 0, 5));
        assertEquals(3, rr.dequeueNext().getPid());
        assertEquals(1, rr.dequeueNext().getPid());
        assertEquals(2, rr.dequeueNext().getPid());
        assertTrue(rr.isEmpty());
    }

    @Test
    void roundRobinNeverPreemptsByItself() {
        RoundRobinScheduler rr = new RoundRobinScheduler();
        rr.enqueue(pcb(1, 0, 1));
        assertFalse(rr.shouldPreempt(pcb(2, 0, 100)));
    }

    @Test
    void srtfPicksShortestRemainingFirst() {
        SRTFScheduler s = new SRTFScheduler();
        s.enqueue(pcb(1, 0, 10));
        s.enqueue(pcb(2, 1, 3));
        s.enqueue(pcb(3, 2, 6));
        assertEquals(2, s.dequeueNext().getPid());
        assertEquals(3, s.dequeueNext().getPid());
        assertEquals(1, s.dequeueNext().getPid());
    }

    @Test
    void srtfTieBreaksByArrivalThenPid() {
        SRTFScheduler s = new SRTFScheduler();
        s.enqueue(pcb(5, 2, 4));
        s.enqueue(pcb(4, 1, 4));
        s.enqueue(pcb(3, 1, 4));
        assertEquals(3, s.dequeueNext().getPid()); // same remaining, same arrival -> smaller PID
        assertEquals(4, s.dequeueNext().getPid());
        assertEquals(5, s.dequeueNext().getPid()); // later arrival last
    }

    @Test
    void srtfPreemptsOnlyForStrictlyShorterRemainingTime() {
        SRTFScheduler s = new SRTFScheduler();
        s.enqueue(pcb(2, 0, 5));
        assertTrue(s.shouldPreempt(pcb(1, 0, 6)));   // 5 < 6
        assertFalse(s.shouldPreempt(pcb(1, 0, 5)));  // tie: keep running
        assertFalse(s.shouldPreempt(pcb(1, 0, 4)));  // ready one is longer
    }

    @Test
    void drainAllEmptiesTheQueueInSelectionOrder() {
        SRTFScheduler s = new SRTFScheduler();
        s.enqueue(pcb(1, 0, 9));
        s.enqueue(pcb(2, 0, 2));
        List<ProcessControlBlock> drained = s.drainAll();
        assertEquals(2, drained.get(0).getPid());
        assertEquals(1, drained.get(1).getPid());
        assertTrue(s.isEmpty());
    }
}
