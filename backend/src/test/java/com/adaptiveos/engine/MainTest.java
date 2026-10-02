package com.adaptiveos.engine;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainTest {

    private static int exec(ByteArrayOutputStream out, ByteArrayOutputStream err, String... args) {
        return Main.execute(args, new PrintStream(out), new PrintStream(err));
    }

    @Test
    void helpAndListWork() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        assertEquals(0, exec(out, err, "help"));
        assertTrue(out.toString().contains("USAGE"));
        out.reset();
        assertEquals(0, exec(out, err, "list"));
        assertTrue(out.toString().contains("baseline"));
    }

    @Test
    void runPrintsAComparisonWithoutRankingAnyPolicy() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        assertEquals(0, exec(out, err, "run", "baseline"));
        String text = out.toString();
        assertTrue(text.contains("ADAPTIVE"));
        assertTrue(text.contains("measurements only"));
    }

    @Test
    void invalidInputFailsClearly() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        assertEquals(2, exec(out, err, "run", "no-such-workload"));
        assertTrue(err.toString().contains("Unknown workload"));
        err.reset();
        assertEquals(2, exec(out, err, "run", "baseline", "--quantum", "0"));
        assertTrue(err.toString().contains("quantum"));
        err.reset();
        assertEquals(2, exec(out, err, "run", "baseline", "--alpha", "abc"));
        assertEquals(2, exec(out, err, "sweep", "bogus"));
        assertEquals(2, exec(out, err, "frobnicate"));
    }

    @Test
    void sweepRuns() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        assertEquals(0, exec(out, err, "sweep", "cooldown", "baseline"));
        assertTrue(out.toString().contains("PARAMETER SWEEP: cooldown"));
    }
}
