package com.adaptiveos.engine.workload;

import com.adaptiveos.engine.model.ProcessControlBlock;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A named set of process specifications. The workload itself is immutable; calling
 * {@link #instantiate(double)} always returns brand-new PCBs, so PURE_RR, PURE_SRTF and ADAPTIVE all
 * start from an identical, untouched copy (fair comparison).
 */
public final class Workload {

    private final String name;
    private final String description;
    private final List<ProcessSpec> specs;

    public Workload(String name, String description, List<ProcessSpec> specs) {
        if (specs == null || specs.isEmpty()) {
            throw new IllegalArgumentException("Workload '" + name + "' must contain at least one process");
        }
        Set<Integer> seen = new HashSet<>();
        for (ProcessSpec s : specs) {
            if (!seen.add(s.pid())) {
                throw new IllegalArgumentException("Workload '" + name + "' has duplicate PID " + s.pid());
            }
        }
        this.name = name;
        this.description = description;
        this.specs = List.copyOf(specs);
    }

    /** Builds fresh PCBs (sorted by PID) for one simulation run. */
    public List<ProcessControlBlock> instantiate(double initialScore) {
        List<ProcessControlBlock> result = new ArrayList<>();
        for (ProcessSpec s : specs) {
            result.add(new ProcessControlBlock(s.pid(), s.arrivalTime(), s.bursts(), initialScore));
        }
        result.sort((a, b) -> Integer.compare(a.getPid(), b.getPid()));
        return result;
    }

    public String name() { return name; }
    public String description() { return description; }
    public List<ProcessSpec> specs() { return specs; }

    /** Total CPU + I/O work plus the latest arrival: a safe upper bound for the simulation length. */
    public long tickUpperBound() {
        long total = 0;
        long lastArrival = 0;
        for (ProcessSpec s : specs) {
            lastArrival = Math.max(lastArrival, s.arrivalTime());
            total += s.bursts().stream().mapToLong(b -> b.duration()).sum();
        }
        return lastArrival + total + 10;
    }
}
