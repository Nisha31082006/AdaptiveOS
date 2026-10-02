package com.adaptiveos.engine.workload;

import com.adaptiveos.engine.model.Burst;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Built-in test workloads. To add your own, write a new static method that returns a
 * {@link Workload} and register it in {@link #all()}, or use a text file (see {@link WorkloadLoader}).
 * <p>
 * Helper notation: {@code cpu(3)} = CPU burst of 3 ticks, {@code io(4)} = I/O burst of 4 ticks.
 */
public final class WorkloadFactory {

    private WorkloadFactory() { }

    private static Burst cpu(int d) { return Burst.cpu(d); }
    private static Burst io(int d) { return Burst.io(d); }

    private static ProcessSpec p(int pid, int arrival, Burst... bursts) {
        return ProcessSpec.of(pid, arrival, bursts);
    }

    private static Workload w(String name, String description, ProcessSpec... specs) {
        return new Workload(name, description, List.of(specs));
    }

    // ================================================================== main workloads

    /** Spec section 28: the small workload used for hand-checking RR and SRTF. */
    public static Workload baseline() {
        return w("baseline", "Spec baseline: P1 CPU10; P2 CPU3-IO4-CPU2; P3 CPU6",
                p(1, 0, cpu(10)),
                p(2, 1, cpu(3), io(4), cpu(2)),
                p(3, 2, cpu(6)));
    }

    /** Spec section 29 / Test 4: P3 starts interactive and turns CPU-heavy. */
    public static Workload phaseChange() {
        List<Burst> interactive = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            interactive.add(cpu(2));
            interactive.add(io(5));
        }
        interactive.add(cpu(2));
        return w("phase-change", "P1 pure compute, P2 interactive, P3 interactive then CPU-heavy",
                p(1, 0, cpu(20)),
                new ProcessSpec(2, 1, interactive),
                p(3, 2, cpu(2), io(5), cpu(2), io(5), cpu(2), cpu(10), cpu(10), cpu(10), cpu(10)));
    }

    /** Test 1: mostly long CPU bursts. */
    public static Workload cpuHeavy() {
        return w("cpu-heavy", "Four long CPU-only processes",
                p(1, 0, cpu(24)), p(2, 1, cpu(18)), p(3, 2, cpu(30)), p(4, 3, cpu(12)));
    }

    /** Test 2: short CPU bursts separated by I/O. */
    public static Workload interactive() {
        return w("interactive", "Four short-burst, I/O-heavy processes",
                p(1, 0, cpu(2), io(4), cpu(2), io(4), cpu(2), io(4), cpu(2)),
                p(2, 1, cpu(1), io(3), cpu(1), io(3), cpu(1), io(3), cpu(1), io(3), cpu(2)),
                p(3, 2, cpu(3), io(5), cpu(3), io(5), cpu(3)),
                p(4, 3, cpu(2), io(6), cpu(2), io(2), cpu(2)));
    }

    /** Test 3: CPU-heavy and interactive processes together. */
    public static Workload mixed() {
        return w("mixed", "Two CPU hogs, two interactive processes, one medium job",
                p(1, 0, cpu(30)),
                p(2, 1, cpu(2), io(4), cpu(2), io(4), cpu(2), io(4), cpu(2), io(4), cpu(2)),
                p(3, 2, cpu(20)),
                p(4, 3, cpu(3), io(5), cpu(3), io(5), cpu(3)),
                p(5, 5, cpu(8)));
    }

    // ================================================================== Test 5: edge cases

    public static Workload edgeSingleProcess() {
        return w("edge-single-process", "Only one process", p(1, 0, cpu(7)));
    }

    public static Workload edgeAllArriveAtZero() {
        return w("edge-all-arrive-at-0", "Three CPU-only processes, all arrive at t=0",
                p(1, 0, cpu(5)), p(2, 0, cpu(3)), p(3, 0, cpu(8)));
    }

    public static Workload edgeCpuOnly() {
        return w("edge-cpu-only", "CPU-only processes with a later arrival",
                p(1, 0, cpu(6)), p(2, 3, cpu(4)));
    }

    public static Workload edgeMultiIoPhases() {
        return w("edge-multi-io-phases", "One process with three CPU/I/O phases plus a CPU hog",
                p(1, 0, cpu(2), io(3), cpu(2), io(3), cpu(2), io(3), cpu(2)),
                p(2, 0, cpu(9)));
    }

    public static Workload edgeBurstLengthOne() {
        return w("edge-burst-length-1", "Bursts of length 1",
                p(1, 0, cpu(1), io(1), cpu(1)), p(2, 0, cpu(1)), p(3, 1, cpu(1)));
    }

    public static Workload edgeBurstEqualsQuantum() {
        return w("edge-burst-equals-quantum", "CPU bursts exactly equal to the default quantum (4)",
                p(1, 0, cpu(4), io(2), cpu(4)), p(2, 0, cpu(4)));
    }

    public static Workload edgeBurstLongerThanQuantum() {
        return w("edge-burst-longer-than-quantum", "CPU bursts longer than the default quantum (4)",
                p(1, 0, cpu(9)), p(2, 0, cpu(5), io(2), cpu(7)));
    }

    /** P1 finishes I/O at t=5, exactly when P2 arrives; P3 keeps the CPU busy meanwhile. */
    public static Workload edgeSimultaneousArrivalAndIoCompletion() {
        return w("edge-simultaneous-arrival-io", "I/O completion and a new arrival at the same tick (t=5)",
                p(1, 0, cpu(2), io(3), cpu(2)), p(3, 0, cpu(6)), p(2, 5, cpu(3)));
    }

    public static Workload edgeSrtfTies() {
        return w("edge-srtf-ties", "Equal remaining times: tie-break by arrival, then PID",
                p(1, 0, cpu(5)), p(2, 0, cpu(5)), p(3, 0, cpu(5)));
    }

    public static Workload edgeVeryShort() {
        return w("edge-very-short", "The shortest possible workload", p(1, 0, cpu(1)));
    }

    public static Workload edgeIdleGap() {
        return w("edge-idle-gap", "CPU sits idle between two processes (utilisation < 100%)",
                p(1, 0, cpu(2)), p(2, 10, cpu(2)));
    }

    // ================================================================== registry

    /** Every built-in workload, in a stable order, keyed by name. */
    public static Map<String, Workload> all() {
        Map<String, Workload> map = new LinkedHashMap<>();
        for (Workload wl : List.of(baseline(), cpuHeavy(), interactive(), mixed(), phaseChange(),
                edgeSingleProcess(), edgeAllArriveAtZero(), edgeCpuOnly(), edgeMultiIoPhases(),
                edgeBurstLengthOne(), edgeBurstEqualsQuantum(), edgeBurstLongerThanQuantum(),
                edgeSimultaneousArrivalAndIoCompletion(), edgeSrtfTies(), edgeVeryShort(), edgeIdleGap())) {
            map.put(wl.name(), wl);
        }
        return map;
    }

    /** The five "core" workloads (Tests 1-4 plus the baseline) used for parameter sweeps. */
    public static List<Workload> core() {
        return List.of(baseline(), cpuHeavy(), interactive(), mixed(), phaseChange());
    }

    public static Workload byName(String name) {
        Workload wl = all().get(name);
        if (wl == null) {
            throw new IllegalArgumentException("Unknown workload '" + name + "'. Available: " + all().keySet());
        }
        return wl;
    }
}
