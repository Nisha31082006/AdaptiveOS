package com.adaptiveos.engine.simulation;

import com.adaptiveos.engine.model.ProcessState;

/**
 * State of one process at one instant.
 *
 * @param pid        process name ("P1")
 * @param state      NEW / READY / RUNNING / WAITING / TERMINATED
 * @param burstType  "CPU", "IO" or "NONE" (finished) for the burst the process is currently in
 * @param remaining  remaining ticks of that current burst (CPU remaining, or I/O remaining while WAITING)
 */
public record ProcessSnapshot(String pid, ProcessState state, String burstType, int remaining) { }
