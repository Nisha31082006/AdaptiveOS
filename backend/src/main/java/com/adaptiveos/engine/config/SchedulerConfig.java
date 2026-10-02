package com.adaptiveos.engine.config;

import java.util.Locale;

/**
 * All tunable experiment parameters in ONE place.
 * <p>
 * IMPORTANT: every default below is a <b>baseline experimental parameter</b>. None of them is claimed
 * to be mathematically optimal. Use the "sweep" command to see how results change when you vary them.
 * <p>
 * The class is immutable. To change a value use the {@code withXxx} methods, which return a NEW
 * validated configuration:
 * <pre>
 *   SchedulerConfig cfg = SchedulerConfig.defaults().withQuantum(6).withCooldownTicks(8);
 * </pre>
 */
public final class SchedulerConfig {

    // ---- fixed scale of the interactivity score (not tunable: the whole model is defined on 0..1024)
    /** Upper end of the interactivity score scale. */
    public static final double MAX_SCORE = 1024.0;
    /** EWMA input when a process behaved interactively (yielded early, e.g. to do I/O). */
    public static final double INTERACTIVE_SIGNAL = 1024.0;
    /** EWMA input when a process behaved CPU-heavy (used its whole slice / was preempted by the quantum). */
    public static final double CPU_HEAVY_SIGNAL = 0.0;

    // ---- baseline defaults
    public static final int DEFAULT_RR_QUANTUM = 4;
    public static final double DEFAULT_EWMA_ALPHA = 0.25;
    public static final double DEFAULT_INITIAL_SCORE = 512.0;
    public static final double DEFAULT_LOW_THRESHOLD = 307.0;   // ~30% of 1024
    public static final double DEFAULT_HIGH_THRESHOLD = 716.0;  // ~70% of 1024
    public static final int DEFAULT_COOLDOWN_TICKS = 12;
    public static final int DEFAULT_SURGE_THRESHOLD = 3;
    public static final double DEFAULT_ANTI_GAMING_THRESHOLD = 0.95;

    private final int rrQuantum;
    private final double ewmaAlpha;
    private final double initialScore;
    private final double lowThreshold;
    private final double highThreshold;
    private final int cooldownTicks;
    private final int surgeThreshold;
    private final double antiGamingThreshold;

    private SchedulerConfig(int rrQuantum, double ewmaAlpha, double initialScore, double lowThreshold,
                            double highThreshold, int cooldownTicks, int surgeThreshold,
                            double antiGamingThreshold) {
        if (rrQuantum < 1) {
            throw new IllegalArgumentException("RR quantum must be >= 1 tick, but was " + rrQuantum);
        }
        if (!(ewmaAlpha > 0.0 && ewmaAlpha <= 1.0)) {
            throw new IllegalArgumentException("EWMA alpha must be in (0, 1], but was " + ewmaAlpha);
        }
        if (!(initialScore >= 0.0 && initialScore <= MAX_SCORE)) {
            throw new IllegalArgumentException("Initial score must be in [0, " + MAX_SCORE + "], but was " + initialScore);
        }
        if (!(lowThreshold >= 0.0 && lowThreshold <= MAX_SCORE)) {
            throw new IllegalArgumentException("LOW threshold must be in [0, " + MAX_SCORE + "], but was " + lowThreshold);
        }
        if (!(highThreshold >= 0.0 && highThreshold <= MAX_SCORE)) {
            throw new IllegalArgumentException("HIGH threshold must be in [0, " + MAX_SCORE + "], but was " + highThreshold);
        }
        if (lowThreshold >= highThreshold) {
            throw new IllegalArgumentException("LOW threshold (" + lowThreshold
                    + ") must be smaller than HIGH threshold (" + highThreshold + ") to leave a hysteresis band");
        }
        if (cooldownTicks < 0) {
            throw new IllegalArgumentException("Cooldown must be >= 0 ticks, but was " + cooldownTicks);
        }
        if (surgeThreshold < 1) {
            throw new IllegalArgumentException("Surge threshold must be >= 1, but was " + surgeThreshold);
        }
        if (!(antiGamingThreshold > 0.0 && antiGamingThreshold <= 1.0)) {
            throw new IllegalArgumentException("Anti-gaming threshold must be in (0, 1], but was " + antiGamingThreshold);
        }
        this.rrQuantum = rrQuantum;
        this.ewmaAlpha = ewmaAlpha;
        this.initialScore = initialScore;
        this.lowThreshold = lowThreshold;
        this.highThreshold = highThreshold;
        this.cooldownTicks = cooldownTicks;
        this.surgeThreshold = surgeThreshold;
        this.antiGamingThreshold = antiGamingThreshold;
    }

    /** The baseline configuration described in the project specification. */
    public static SchedulerConfig defaults() {
        return new SchedulerConfig(DEFAULT_RR_QUANTUM, DEFAULT_EWMA_ALPHA, DEFAULT_INITIAL_SCORE,
                DEFAULT_LOW_THRESHOLD, DEFAULT_HIGH_THRESHOLD, DEFAULT_COOLDOWN_TICKS,
                DEFAULT_SURGE_THRESHOLD, DEFAULT_ANTI_GAMING_THRESHOLD);
    }

    public SchedulerConfig withQuantum(int v) {
        return new SchedulerConfig(v, ewmaAlpha, initialScore, lowThreshold, highThreshold, cooldownTicks, surgeThreshold, antiGamingThreshold);
    }

    public SchedulerConfig withAlpha(double v) {
        return new SchedulerConfig(rrQuantum, v, initialScore, lowThreshold, highThreshold, cooldownTicks, surgeThreshold, antiGamingThreshold);
    }

    public SchedulerConfig withInitialScore(double v) {
        return new SchedulerConfig(rrQuantum, ewmaAlpha, v, lowThreshold, highThreshold, cooldownTicks, surgeThreshold, antiGamingThreshold);
    }

    public SchedulerConfig withLowThreshold(double v) {
        return new SchedulerConfig(rrQuantum, ewmaAlpha, initialScore, v, highThreshold, cooldownTicks, surgeThreshold, antiGamingThreshold);
    }

    public SchedulerConfig withHighThreshold(double v) {
        return new SchedulerConfig(rrQuantum, ewmaAlpha, initialScore, lowThreshold, v, cooldownTicks, surgeThreshold, antiGamingThreshold);
    }

    public SchedulerConfig withCooldownTicks(int v) {
        return new SchedulerConfig(rrQuantum, ewmaAlpha, initialScore, lowThreshold, highThreshold, v, surgeThreshold, antiGamingThreshold);
    }

    public SchedulerConfig withSurgeThreshold(int v) {
        return new SchedulerConfig(rrQuantum, ewmaAlpha, initialScore, lowThreshold, highThreshold, cooldownTicks, v, antiGamingThreshold);
    }

    public SchedulerConfig withAntiGamingThreshold(double v) {
        return new SchedulerConfig(rrQuantum, ewmaAlpha, initialScore, lowThreshold, highThreshold, cooldownTicks, surgeThreshold, v);
    }

    public int rrQuantum() { return rrQuantum; }
    public double ewmaAlpha() { return ewmaAlpha; }
    public double initialScore() { return initialScore; }
    public double lowThreshold() { return lowThreshold; }
    public double highThreshold() { return highThreshold; }
    public int cooldownTicks() { return cooldownTicks; }
    public int surgeThreshold() { return surgeThreshold; }
    public double antiGamingThreshold() { return antiGamingThreshold; }

    /** Midpoint of the hysteresis band; used as the "neutral" score for phase-change early switching. */
    public double neutralScore() {
        return (lowThreshold + highThreshold) / 2.0;
    }

    /** One-line description for report headers. */
    public String describe() {
        return String.format(Locale.ROOT,
                "quantum=%d, alpha=%.2f, initialScore=%.0f, low=%.0f, high=%.0f, cooldown=%d, surge=%d, antiGaming=%.2f",
                rrQuantum, ewmaAlpha, initialScore, lowThreshold, highThreshold, cooldownTicks, surgeThreshold,
                antiGamingThreshold);
    }

    @Override
    public String toString() {
        return describe();
    }
}
