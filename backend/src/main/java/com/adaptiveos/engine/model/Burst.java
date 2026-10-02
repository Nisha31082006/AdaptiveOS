package com.adaptiveos.engine.model;

/**
 * One phase of a process's life: either a CPU burst or an I/O burst.
 * Immutable, so the same Burst objects can be shared safely between simulation runs.
 *
 * @param type     CPU or IO
 * @param duration length in ticks; must be strictly positive
 */
public record Burst(BurstType type, int duration) {

    public Burst {
        if (type == null) {
            throw new IllegalArgumentException("Burst type must not be null");
        }
        if (duration <= 0) {
            throw new IllegalArgumentException(
                    "Burst duration must be positive, but was " + duration + " (type " + type + ")");
        }
    }

    /** Convenience factory: {@code Burst.cpu(5)}. */
    public static Burst cpu(int duration) {
        return new Burst(BurstType.CPU, duration);
    }

    /** Convenience factory: {@code Burst.io(3)}. */
    public static Burst io(int duration) {
        return new Burst(BurstType.IO, duration);
    }

    @Override
    public String toString() {
        return type + ":" + duration;
    }
}
