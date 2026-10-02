package com.adaptiveos.engine.model;

/** The two kinds of bursts a process can have. Using an enum avoids fragile raw strings like "CPU". */
public enum BurstType {
    /** The process needs the CPU for this many ticks. */
    CPU,
    /** The process is blocked doing input/output for this many ticks (it does NOT need the CPU). */
    IO
}
