package com.adaptiveos.engine.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SchedulerConfigTest {

    @Test
    void defaultsMatchTheSpecification() {
        SchedulerConfig c = SchedulerConfig.defaults();
        assertEquals(4, c.rrQuantum());
        assertEquals(0.25, c.ewmaAlpha(), 1e-12);
        assertEquals(512.0, c.initialScore(), 1e-12);
        assertEquals(307.0, c.lowThreshold(), 1e-12);
        assertEquals(716.0, c.highThreshold(), 1e-12);
        assertEquals(12, c.cooldownTicks());
        assertEquals(3, c.surgeThreshold());
        assertEquals(0.95, c.antiGamingThreshold(), 1e-12);
    }

    @Test
    void withMethodsReturnNewConfigsAndKeepTheRest() {
        SchedulerConfig base = SchedulerConfig.defaults();
        SchedulerConfig changed = base.withQuantum(8).withCooldownTicks(4);
        assertEquals(8, changed.rrQuantum());
        assertEquals(4, changed.cooldownTicks());
        assertEquals(4, base.rrQuantum());          // original untouched
        assertEquals(0.25, changed.ewmaAlpha(), 1e-12);
    }

    @Test
    void invalidValuesAreRejectedWithClearMessages() {
        SchedulerConfig d = SchedulerConfig.defaults();
        assertThrows(IllegalArgumentException.class, () -> d.withQuantum(0));
        assertThrows(IllegalArgumentException.class, () -> d.withQuantum(-3));
        assertThrows(IllegalArgumentException.class, () -> d.withAlpha(0.0));
        assertThrows(IllegalArgumentException.class, () -> d.withAlpha(1.5));
        assertThrows(IllegalArgumentException.class, () -> d.withInitialScore(2000));
        assertThrows(IllegalArgumentException.class, () -> d.withLowThreshold(-1));
        assertThrows(IllegalArgumentException.class, () -> d.withHighThreshold(1025));
        assertThrows(IllegalArgumentException.class, () -> d.withLowThreshold(800)); // low >= high
        assertThrows(IllegalArgumentException.class, () -> d.withCooldownTicks(-1));
        assertThrows(IllegalArgumentException.class, () -> d.withSurgeThreshold(0));
        assertThrows(IllegalArgumentException.class, () -> d.withAntiGamingThreshold(1.2));
    }
}
