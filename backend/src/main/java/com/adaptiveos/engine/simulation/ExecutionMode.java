package com.adaptiveos.engine.simulation;

/** Which scheduling behaviour a simulation run uses. The same workload can be run under all three. */
public enum ExecutionMode {
    /** Round Robin for the whole run. */
    PURE_RR,
    /** Shortest Remaining Time First for the whole run. */
    PURE_SRTF,
    /** Starts with RR and lets the AdaptiveController switch the ACTIVE policy (globally) at runtime. */
    ADAPTIVE
}
