package com.adaptiveos.engine;

import com.adaptiveos.engine.config.SchedulerConfig;
import com.adaptiveos.engine.simulation.ExperimentRunner;
import com.adaptiveos.engine.workload.Workload;
import com.adaptiveos.engine.workload.WorkloadFactory;
import com.adaptiveos.engine.workload.WorkloadLoader;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Command line entry point. Only parses arguments and delegates; no scheduling logic lives here.
 * Run with "help" to see all commands.
 */
public final class Main {

    private static final String HELP = """
            AdaptiveOS - a runtime-adaptive CPU scheduling simulator (RR + SRTF)

            USAGE
              java -jar adaptive-os-1.0.0.jar [command] [options]

            COMMANDS
              demo                       Baseline workload with full adaptive log and Gantt charts (default)
              list                       List the built-in workloads
              run <workload>             Compare RR / SRTF / ADAPTIVE on one built-in workload
              file <path>                Same, but the workload is read from a text file
              all                        Compact comparison tables for every built-in workload
              sweep <param> [workload]   Vary one parameter: alpha | cooldown | surge | antigaming | quantum | all
                                         (no workload given = the five core workloads)
              help                       Show this text

            OUTPUT OPTIONS
              --log        print the adaptive event log         --gantt     print Gantt charts

            PARAMETER OPTIONS (all are BASELINE experimental values, not claimed optimal)
              --quantum N (4)     --alpha X (0.25)      --initial-score X (512)
              --low X (307)       --high X (716)        --cooldown N (12)
              --surge N (3)       --antigaming X (0.95)

            EXAMPLES
              java -jar adaptive-os-1.0.0.jar run phase-change --log --gantt
              java -jar adaptive-os-1.0.0.jar run mixed --cooldown 8 --alpha 0.5
              java -jar adaptive-os-1.0.0.jar sweep cooldown mixed
              java -jar adaptive-os-1.0.0.jar file workloads/example.txt --gantt
            """;

    private Main() { }

    public static void main(String[] args) {
        System.exit(execute(args, System.out, System.err));
    }

    /** Testable core of main(): returns the process exit code. */
    static int execute(String[] args, PrintStream out, PrintStream err) {
        try {
            List<String> positional = new ArrayList<>();
            boolean log = false;
            boolean gantt = false;
            SchedulerConfig config = SchedulerConfig.defaults();

            for (int i = 0; i < args.length; i++) {
                String a = args[i];
                if (!a.startsWith("--")) {
                    positional.add(a);
                    continue;
                }
                switch (a) {
                    case "--log" -> log = true;
                    case "--gantt" -> gantt = true;
                    case "--quantum" -> config = config.withQuantum(intValue(args, ++i, a));
                    case "--alpha" -> config = config.withAlpha(doubleValue(args, ++i, a));
                    case "--initial-score" -> config = config.withInitialScore(doubleValue(args, ++i, a));
                    case "--low" -> config = config.withLowThreshold(doubleValue(args, ++i, a));
                    case "--high" -> config = config.withHighThreshold(doubleValue(args, ++i, a));
                    case "--cooldown" -> config = config.withCooldownTicks(intValue(args, ++i, a));
                    case "--surge" -> config = config.withSurgeThreshold(intValue(args, ++i, a));
                    case "--antigaming" -> config = config.withAntiGamingThreshold(doubleValue(args, ++i, a));
                    default -> throw new IllegalArgumentException("Unknown option " + a + " (try 'help')");
                }
            }

            String command = positional.isEmpty() ? "demo" : positional.get(0);
            switch (command) {
                case "help", "--help" -> out.print(HELP);
                case "list" -> listWorkloads(out);
                case "demo" -> ExperimentRunner.printFullReport(WorkloadFactory.baseline(), config, true, true, out);
                case "run" -> {
                    requireArgs(positional, 2, "run <workload>");
                    ExperimentRunner.printFullReport(WorkloadFactory.byName(positional.get(1)), config, log, gantt, out);
                }
                case "file" -> {
                    requireArgs(positional, 2, "file <path>");
                    Workload w = WorkloadLoader.fromFile(Path.of(positional.get(1)));
                    ExperimentRunner.printFullReport(w, config, log, gantt, out);
                }
                case "all" -> ExperimentRunner.printAllWorkloads(config, out);
                case "sweep" -> {
                    requireArgs(positional, 2, "sweep <param> [workload]");
                    List<Workload> workloads = positional.size() > 2
                            ? List.of(WorkloadFactory.byName(positional.get(2))) : WorkloadFactory.core();
                    if (positional.get(1).equals("all")) {
                        ExperimentRunner.sweepAll(workloads, config, out);
                    } else {
                        ExperimentRunner.sweep(positional.get(1), workloads, config, out);
                    }
                }
                default -> throw new IllegalArgumentException("Unknown command '" + command + "' (try 'help')");
            }
            return 0;
        } catch (IllegalArgumentException | IOException e) {
            err.println("Error: " + e.getMessage());
            return 2;
        }
    }

    private static void listWorkloads(PrintStream out) {
        out.println("Built-in workloads:");
        WorkloadFactory.all().values().forEach(w ->
                out.println(String.format("  %-34s %s", w.name(), w.description())));
    }

    private static void requireArgs(List<String> positional, int count, String usage) {
        if (positional.size() < count) {
            throw new IllegalArgumentException("Missing argument. Usage: " + usage);
        }
    }

    private static int intValue(String[] args, int index, String option) {
        String text = valueOf(args, index, option);
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(option + " needs a whole number, but got '" + text + "'");
        }
    }

    private static double doubleValue(String[] args, int index, String option) {
        String text = valueOf(args, index, option);
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(option + " needs a number, but got '" + text + "'");
        }
    }

    private static String valueOf(String[] args, int index, String option) {
        if (index >= args.length) {
            throw new IllegalArgumentException(option + " needs a value");
        }
        return args[index];
    }
}
