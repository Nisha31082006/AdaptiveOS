package com.adaptiveos.engine.logging;

/** Category of a structured simulation event (used for filtering in the web UI). */
public enum EventType {
    SYSTEM,
    ARRIVAL,
    DISPATCHED,
    PREEMPTED,
    QUANTUM_EXPIRED,
    BURST_COMPLETED,
    IO_STARTED,
    IO_COMPLETED,
    TERMINATED,
    /** Adaptive controller: an event was classified as interactive or CPU-heavy. */
    SIGNAL,
    /** Adaptive controller: the EWMA score was updated. */
    EWMA_UPDATE,
    /** Adaptive controller: the surge counter changed. */
    SURGE,
    /** Adaptive controller: anti-gaming ratio E was evaluated. */
    ANTI_GAMING,
    /** Adaptive controller: a global policy switch happened. */
    POLICY_SWITCH,
    /** Adaptive controller: cooldown started. */
    COOLDOWN,
    /** Adaptive controller: a wanted switch was blocked by the cooldown. */
    COOLDOWN_BLOCKED,
    /** Ready processes were moved to the newly active policy's queue. */
    MIGRATION;

    /** True for events produced by the AdaptiveController (not by the plain scheduler). */
    public boolean isAdaptive() {
        return switch (this) {
            case SIGNAL, EWMA_UPDATE, SURGE, ANTI_GAMING, POLICY_SWITCH, COOLDOWN, COOLDOWN_BLOCKED, MIGRATION -> true;
            default -> false;
        };
    }
}
