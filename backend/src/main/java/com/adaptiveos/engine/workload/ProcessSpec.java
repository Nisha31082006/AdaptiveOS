package com.adaptiveos.engine.workload;

import com.adaptiveos.engine.model.Burst;
import com.adaptiveos.engine.model.BurstType;

import java.util.List;

/**
 * Immutable description of one process (the "recipe"). A fresh mutable
 * {@link com.adaptiveos.engine.model.ProcessControlBlock} is built from it for every simulation run.
 * <p>
 * Validation rules (violations throw IllegalArgumentException with a clear message):
 * pid &gt; 0, arrival &gt;= 0, non-empty burst list, every duration &gt; 0 (checked by {@link Burst}),
 * first and last burst must be CPU, and two I/O bursts may not be adjacent.
 */
public record ProcessSpec(int pid, int arrivalTime, List<Burst> bursts) {

    public ProcessSpec {
        if (pid <= 0) {
            throw new IllegalArgumentException("PID must be positive, but was " + pid);
        }
        if (arrivalTime < 0) {
            throw new IllegalArgumentException("P" + pid + ": arrival time must be >= 0, but was " + arrivalTime);
        }
        if (bursts == null || bursts.isEmpty()) {
            throw new IllegalArgumentException("P" + pid + ": burst list must not be empty");
        }
        if (bursts.get(0).type() != BurstType.CPU) {
            throw new IllegalArgumentException("P" + pid + ": first burst must be CPU");
        }
        if (bursts.get(bursts.size() - 1).type() != BurstType.CPU) {
            throw new IllegalArgumentException("P" + pid + ": last burst must be CPU (a process cannot end while in I/O)");
        }
        for (int i = 1; i < bursts.size(); i++) {
            if (bursts.get(i).type() == BurstType.IO && bursts.get(i - 1).type() == BurstType.IO) {
                throw new IllegalArgumentException("P" + pid + ": two I/O bursts in a row; merge them into one");
            }
        }
        bursts = List.copyOf(bursts);
    }

    public static ProcessSpec of(int pid, int arrivalTime, Burst... bursts) {
        return new ProcessSpec(pid, arrivalTime, List.of(bursts));
    }
}
