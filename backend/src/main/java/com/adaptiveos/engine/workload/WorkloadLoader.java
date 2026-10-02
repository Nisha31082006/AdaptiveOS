package com.adaptiveos.engine.workload;

import com.adaptiveos.engine.model.Burst;
import com.adaptiveos.engine.model.BurstType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Loads a workload from a plain text file so you can experiment without recompiling.
 * <pre>
 * # comment lines start with '#'
 * # pid  arrival  bursts...
 * P1  0  CPU:10
 * P2  1  CPU:3 IO:4 CPU:2
 * P3  2  CPU:6
 * </pre>
 * The PID may be written as {@code P1} or {@code 1}.
 */
public final class WorkloadLoader {

    private WorkloadLoader() { }

    public static Workload fromFile(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path);
        String fileName = path.getFileName().toString();
        return parse(fileName, "Loaded from " + path, lines);
    }

    public static Workload parse(String name, String description, List<String> lines) {
        List<ProcessSpec> specs = new ArrayList<>();
        int lineNo = 0;
        for (String raw : lines) {
            lineNo++;
            String line = raw.strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            try {
                specs.add(parseLine(line));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Line " + lineNo + " ('" + line + "'): " + e.getMessage(), e);
            }
        }
        return new Workload(name, description, specs);
    }

    private static ProcessSpec parseLine(String line) {
        String[] tokens = line.split("\\s+");
        if (tokens.length < 3) {
            throw new IllegalArgumentException("expected: <pid> <arrival> <burst> [<burst> ...]");
        }
        int pid = parseInt(tokens[0].toUpperCase(Locale.ROOT).replaceFirst("^P", ""), "pid");
        int arrival = parseInt(tokens[1], "arrival time");
        List<Burst> bursts = new ArrayList<>();
        for (int i = 2; i < tokens.length; i++) {
            String[] parts = tokens[i].split(":");
            if (parts.length != 2) {
                throw new IllegalArgumentException("bad burst '" + tokens[i] + "' (use CPU:5 or IO:3)");
            }
            BurstType type;
            try {
                type = BurstType.valueOf(parts[0].toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("unknown burst type '" + parts[0] + "' (use CPU or IO)");
            }
            bursts.add(new Burst(type, parseInt(parts[1], "burst duration")));
        }
        return new ProcessSpec(pid, arrival, bursts);
    }

    private static int parseInt(String text, String what) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("'" + text + "' is not a valid " + what);
        }
    }
}
