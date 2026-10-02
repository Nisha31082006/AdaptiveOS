package com.adaptiveos.engine.scheduler;

import com.adaptiveos.engine.config.SchedulerConfig;

/** The result of classifying a {@link RuntimeEvent}; it is the input value fed to the EWMA. */
public enum BehaviorSignal {
    /** Voluntary early yield: process behaves like an interactive / I/O-bound one. */
    INTERACTIVE(SchedulerConfig.INTERACTIVE_SIGNAL),
    /** Used the whole slice or was cut off by the quantum: process behaves CPU-heavy. */
    CPU_HEAVY(SchedulerConfig.CPU_HEAVY_SIGNAL);

    private final double value;

    BehaviorSignal(double value) {
        this.value = value;
    }

    /** Numeric EWMA input (1024 for interactive, 0 for CPU-heavy). */
    public double value() {
        return value;
    }
}
