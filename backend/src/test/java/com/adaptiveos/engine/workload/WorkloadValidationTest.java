package com.adaptiveos.engine.workload;

import com.adaptiveos.engine.model.Burst;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkloadValidationTest {

    @Test
    void negativeOrZeroBurstDurationIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> Burst.cpu(-1));
        assertThrows(IllegalArgumentException.class, () -> Burst.io(0));
    }

    @Test
    void emptyBurstListIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new ProcessSpec(1, 0, List.of()));
    }

    @Test
    void negativeArrivalIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> ProcessSpec.of(1, -1, Burst.cpu(3)));
    }

    @Test
    void invalidPidIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> ProcessSpec.of(0, 0, Burst.cpu(3)));
    }

    @Test
    void burstOrderRulesAreEnforced() {
        assertThrows(IllegalArgumentException.class, () -> ProcessSpec.of(1, 0, Burst.io(2), Burst.cpu(3)));
        assertThrows(IllegalArgumentException.class, () -> ProcessSpec.of(1, 0, Burst.cpu(2), Burst.io(3)));
        assertThrows(IllegalArgumentException.class,
                () -> ProcessSpec.of(1, 0, Burst.cpu(2), Burst.io(3), Burst.io(1), Burst.cpu(1)));
    }

    @Test
    void duplicatePidsAndEmptyWorkloadsAreRejected() {
        ProcessSpec a = ProcessSpec.of(1, 0, Burst.cpu(2));
        ProcessSpec b = ProcessSpec.of(1, 3, Burst.cpu(2));
        assertThrows(IllegalArgumentException.class, () -> new Workload("dup", "", List.of(a, b)));
        assertThrows(IllegalArgumentException.class, () -> new Workload("empty", "", List.of()));
    }

    @Test
    void instantiateReturnsIndependentFreshCopies() {
        Workload w = WorkloadFactory.baseline();
        var first = w.instantiate(512);
        var second = w.instantiate(512);
        first.get(0).runOneTick();
        assertEquals(9, first.get(0).getRemainingTime());
        assertEquals(10, second.get(0).getRemainingTime());
    }

    @Test
    void textLoaderParsesAWorkload() {
        Workload w = WorkloadLoader.parse("t", "d", List.of(
                "# comment", "", "P1 0 CPU:10", "2 1 CPU:3 IO:4 CPU:2"));
        assertEquals(2, w.specs().size());
        assertEquals(3, w.specs().get(1).bursts().size());
    }

    @Test
    void textLoaderReportsTheOffendingLine() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> WorkloadLoader.parse("t", "d", List.of("P1 0 CPU:10", "P2 1 CPU:-3")));
        assertTrue(e.getMessage().contains("Line 2"));
        assertThrows(IllegalArgumentException.class, () -> WorkloadLoader.parse("t", "d", List.of("P1 0 GPU:3")));
        assertThrows(IllegalArgumentException.class, () -> WorkloadLoader.parse("t", "d", List.of("P1 0")));
    }

    @Test
    void everyBuiltInWorkloadIsValid() {
        assertTrue(WorkloadFactory.all().size() >= 16);
        assertThrows(IllegalArgumentException.class, () -> WorkloadFactory.byName("does-not-exist"));
    }
}
