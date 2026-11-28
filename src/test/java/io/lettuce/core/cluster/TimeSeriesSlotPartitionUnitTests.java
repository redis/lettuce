/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.cluster;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.timeseries.TsMAddValue;

/**
 * Unit tests for {@link TimeSeriesSlotPartition}: the split of TS.MADD samples by slot and the restored caller order.
 *
 * @author Aleksandar Todorov
 */
@Tag(UNIT_TEST)
class TimeSeriesSlotPartitionUnitTests {

    private static final int SLOT_A = SlotHash.getSlot("{a}");

    private static final int SLOT_B = SlotHash.getSlot("{b}");

    /**
     * Scenario K05: samples are split by the slot of their key, each slot's list in caller order, and a repeated key keeps
     * every sample.
     */
    @Test
    void splitsBySlotInCallerOrder() {

        TsMAddValue<String> a1 = TsMAddValue.of("{a}:1", 1000L, 1.0);
        TsMAddValue<String> b1 = TsMAddValue.of("{b}:1", 1000L, 2.0);
        TsMAddValue<String> a2 = TsMAddValue.of("{a}:2", 1000L, 3.0);
        TsMAddValue<String> a1Again = TsMAddValue.of("{a}:1", 2000L, 4.0);

        TimeSeriesSlotPartition<String> partition = TimeSeriesSlotPartition.of(StringCodec.UTF8,
                Arrays.asList(a1, b1, a2, a1Again));

        assertThat(partition.isMultiSlot()).isTrue();
        assertThat(partition.getSamplesBySlot()).containsOnlyKeys(SLOT_A, SLOT_B);
        assertThat(partition.getSamplesBySlot().get(SLOT_A)).containsExactly(a1, a2, a1Again);
        assertThat(partition.getSamplesBySlot().get(SLOT_B)).containsExactly(b1);
    }

    /**
     * Scenario K05: the per-slot results go back to the caller's positions, including a repeated key and a {@code null} for a
     * rejected sample.
     */
    @Test
    void restoresCallerOrder() {

        TimeSeriesSlotPartition<String> partition = TimeSeriesSlotPartition.of(StringCodec.UTF8,
                Arrays.asList(TsMAddValue.of("{a}:1", 1000L, 1.0), TsMAddValue.of("{b}:1", 1000L, 2.0),
                        TsMAddValue.of("{a}:1", 1000L, 3.0), TsMAddValue.of("{b}:2", 1500L, 4.0)));

        Map<Integer, List<Long>> results = new HashMap<>();
        results.put(SLOT_A, Arrays.asList(1000L, null));
        results.put(SLOT_B, Arrays.asList(1000L, 1500L));

        assertThat(partition.restoreOrder(results)).containsExactly(1000L, 1000L, null, 1500L);
    }

    /**
     * Scenario K05: samples in one slot are a single partition, so the caller can send the command unchanged.
     */
    @Test
    void singleSlot() {

        TimeSeriesSlotPartition<String> partition = TimeSeriesSlotPartition.of(StringCodec.UTF8,
                Arrays.asList(TsMAddValue.of("{a}:1", 1000L, 1.0), TsMAddValue.of("{a}:2", 1000L, 2.0)));

        assertThat(partition.isMultiSlot()).isFalse();
        assertThat(partition.getSamplesBySlot()).containsOnlyKeys(SLOT_A);
        assertThat(partition.restoreOrder(Collections.singletonMap(SLOT_A, Arrays.asList(1L, 2L)))).containsExactly(1L, 2L);
    }

    /**
     * Scenario K05: {@code null}, empty, or with a {@code null} element gives no partition, so the command builder reports the
     * problem.
     */
    @Test
    void invalidSamplesGiveNoPartition() {

        assertThat(TimeSeriesSlotPartition.of(StringCodec.UTF8, null)).isNull();
        assertThat(TimeSeriesSlotPartition.of(StringCodec.UTF8, Collections.<TsMAddValue<String>> emptyList())).isNull();
        assertThat(TimeSeriesSlotPartition.of(StringCodec.UTF8, Arrays.asList(TsMAddValue.of("{a}:1", 1000L, 1.0), null)))
                .isNull();
    }

}
