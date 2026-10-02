package com.adaptiveos.engine.visualization;

import com.adaptiveos.engine.simulation.SimulationResult;
import com.adaptiveos.engine.simulation.TimelineSegment;

import java.io.PrintStream;
import java.util.List;

/**
 * Console Gantt chart. Example:
 * <pre>
 *   | P1 | P2 |  P3  | P1 |
 *   0    4    7      11   15
 * </pre>
 * Long charts are wrapped into several rows. For ADAPTIVE (and any run) the active policy timeline is
 * printed underneath in the same style.
 */
public final class GanttChartPrinter {

    private static final int MAX_LINE_WIDTH = 100;

    private GanttChartPrinter() { }

    public static void print(SimulationResult result, PrintStream out) {
        out.println("Gantt chart (" + result.mode() + ", workload " + result.workloadName() + "):");
        printRows(result.gantt(), out);
        out.println();
        out.println("Active policy over time:");
        printRows(result.policyTimeline(), out);
    }

    /** Prints segments as boxes with the boundary times underneath, wrapping at MAX_LINE_WIDTH. */
    static void printRows(List<TimelineSegment> segments, PrintStream out) {
        StringBuilder boxes = new StringBuilder();
        StringBuilder times = new StringBuilder();
        long rowEnd = 0;
        boolean rowEmpty = true;

        for (TimelineSegment seg : segments) {
            String startText = String.valueOf(seg.start());
            int cell = Math.max(seg.label().length() + 2, startText.length() + 1);
            if (!rowEmpty && boxes.length() + cell + 1 > MAX_LINE_WIDTH) {
                flushRow(boxes, times, rowEnd, out);
                rowEmpty = true;
            }
            if (rowEmpty) {
                boxes.append('|');
            }
            boxes.append(center(seg.label(), cell)).append('|');
            times.append(padRight(startText, cell + 1));
            rowEnd = seg.end();
            rowEmpty = false;
        }
        if (!rowEmpty) {
            flushRow(boxes, times, rowEnd, out);
        }
    }

    private static void flushRow(StringBuilder boxes, StringBuilder times, long endTime, PrintStream out) {
        // The time line is offset by one because the boxes start with a '|' character.
        out.println(boxes);
        out.println(times.append(endTime));
        boxes.setLength(0);
        times.setLength(0);
    }

    private static String center(String text, int width) {
        int total = width - text.length();
        int left = total / 2;
        return " ".repeat(left) + text + " ".repeat(total - left);
    }

    private static String padRight(String text, int width) {
        return text + " ".repeat(Math.max(0, width - text.length()));
    }
}
