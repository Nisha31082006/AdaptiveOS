package com.adaptiveos.engine.logging;

import com.adaptiveos.engine.model.SchedulingPolicy;

/**
 * One structured log entry produced by the engine.
 *
 * @param time      simulated time
 * @param type      event category
 * @param processId process name such as "P1", or null for system-wide events
 * @param policy    policy that was active when the event happened
 * @param message   human readable text (the same text as in the console log)
 */
public record SimEvent(long time, EventType type, String processId, SchedulingPolicy policy, String message) {
    /** Console log format: {@code [t=4] P1 quantum expired}. */
    public String toLine() {
        return "[t=" + time + "] " + message;
    }
}
