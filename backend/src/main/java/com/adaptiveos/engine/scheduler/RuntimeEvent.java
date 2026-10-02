package com.adaptiveos.engine.scheduler;

/**
 * The raw runtime events the AdaptiveController learns from. AdaptiveOS is REACTIVE: it only ever
 * looks at events that already happened; it never predicts future bursts.
 */
public enum RuntimeEvent {
    /**
     * Event B: the time slice ran out while the CPU burst was still unfinished (involuntary).
     * Always a CPU-heavy-like signal.
     */
    QUANTUM_EXPIRED,

    /**
     * Events A and C: the process finished its CPU burst on its own and moves to I/O (voluntary yield).
     * Whether this is interactive-like or CPU-heavy-like depends on how much of the slice was used
     * (anti-gaming ratio E). A burst that ends EXACTLY at the slice boundary is a completion, never a
     * quantum expiry, but because E = 1.0 >= threshold it still counts as CPU-heavy-like evidence.
     */
    CPU_BURST_COMPLETED_TO_IO
}
