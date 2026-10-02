package com.adaptiveos.engine.logging;

import com.adaptiveos.engine.model.SchedulingPolicy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Collects the readable, structured event log of ONE simulation run.
 * A disabled logger ignores everything (fast; used for experiment sweeps).
 */
public final class SimLogger {

    private final boolean enabled;
    private final List<SimEvent> events = new ArrayList<>();
    private final List<String> lines = new ArrayList<>();
    private SchedulingPolicy currentPolicy = SchedulingPolicy.RR;

    private SimLogger(boolean enabled) {
        this.enabled = enabled;
    }

    public static SimLogger recording() {
        return new SimLogger(true);
    }

    public static SimLogger disabled() {
        return new SimLogger(false);
    }

    public boolean isEnabled() {
        return enabled;
    }

    /** The engine tells the logger which policy is active so every event can be tagged with it. */
    public void setPolicy(SchedulingPolicy policy) {
        this.currentPolicy = policy;
    }

    /** Logs a formatted message stamped with the simulated time, an event type and (optionally) a process. */
    public void log(long time, EventType type, String processId, String format, Object... args) {
        if (!enabled) {
            return;
        }
        SimEvent event = new SimEvent(time, type, processId, currentPolicy, String.format(Locale.ROOT, format, args));
        events.add(event);
        lines.add(event.toLine());
    }

    public List<String> lines() {
        return Collections.unmodifiableList(lines);
    }

    public List<SimEvent> events() {
        return Collections.unmodifiableList(events);
    }
}
