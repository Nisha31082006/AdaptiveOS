package com.adaptiveos.engine.simulation;

import com.adaptiveos.engine.config.SchedulerConfig;
import com.adaptiveos.engine.logging.SimLogger;
import com.adaptiveos.engine.model.ProcessControlBlock;
import com.adaptiveos.engine.workload.Workload;

/** Small helpers shared by the simulation tests. */
final class TestSupport {

    private TestSupport() { }

    static SimulationResult run(Workload w, ExecutionMode mode) {
        return run(w, mode, SchedulerConfig.defaults(), false);
    }

    static SimulationResult run(Workload w, ExecutionMode mode, SchedulerConfig cfg, boolean log) {
        return new SimulationEngine(w, mode, cfg, log ? SimLogger.recording() : SimLogger.disabled()).run();
    }

    static ProcessControlBlock proc(SimulationResult r, int pid) {
        return r.processes().stream().filter(p -> p.getPid() == pid).findFirst().orElseThrow();
    }

    /** Renders the Gantt as "P1[0-4] P2[4-7] ..." for easy comparison with hand calculations. */
    static String gantt(SimulationResult r) {
        StringBuilder sb = new StringBuilder();
        for (TimelineSegment s : r.gantt()) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(s.label()).append('[').append(s.start()).append('-').append(s.end()).append(']');
        }
        return sb.toString();
    }
}
