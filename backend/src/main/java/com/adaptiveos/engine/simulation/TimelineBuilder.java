package com.adaptiveos.engine.simulation;

import java.util.ArrayList;
import java.util.List;

/** Builds a compact list of segments from one label per tick (consecutive equal labels are merged). */
final class TimelineBuilder {

    private final List<TimelineSegment> segments = new ArrayList<>();
    private String currentLabel;
    private long currentStart;

    /** Records that {@code label} occupied tick number {@code tick} (ticks must be appended in order). */
    void append(String label, long tick) {
        if (currentLabel == null) {
            currentLabel = label;
            currentStart = tick;
        } else if (!currentLabel.equals(label)) {
            segments.add(new TimelineSegment(currentLabel, currentStart, tick));
            currentLabel = label;
            currentStart = tick;
        }
    }

    /** Closes the last segment at time {@code end} and returns all segments. */
    List<TimelineSegment> finish(long end) {
        if (currentLabel != null) {
            segments.add(new TimelineSegment(currentLabel, currentStart, end));
            currentLabel = null;
        }
        return List.copyOf(segments);
    }
}
