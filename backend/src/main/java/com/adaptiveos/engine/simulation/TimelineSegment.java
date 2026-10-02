package com.adaptiveos.engine.simulation;

/**
 * A half-open interval [start, end) with a label: a process name ("P1"), "idle", or a policy ("RR"/"SRTF").
 */
public record TimelineSegment(String label, long start, long end) {
    public long length() {
        return end - start;
    }
}
