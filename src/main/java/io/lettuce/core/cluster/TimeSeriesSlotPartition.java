/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.cluster;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.timeseries.TsMAddValue;

/**
 * Partition of TS.MADD samples by hash slot. Samples are partitioned by their position, not by key, because one key can appear
 * several times with different timestamps.
 *
 * @param <K> Key type.
 * @since 8.0
 */
final class TimeSeriesSlotPartition<K> {

    private final Map<Integer, List<TsMAddValue<K>>> samplesBySlot = new LinkedHashMap<>();

    private final Map<Integer, List<Integer>> positionsBySlot = new LinkedHashMap<>();

    private final int size;

    private TimeSeriesSlotPartition(int size) {
        this.size = size;
    }

    /**
     * Partition {@code samples} by the slot of their key. Returns {@code null} if the samples cannot be partitioned
     * ({@code null}, empty or with {@code null} elements), so that the caller sends the command unchanged and the command
     * builder reports the problem.
     */
    static <K> TimeSeriesSlotPartition<K> of(RedisCodec<K, ?> codec, List<TsMAddValue<K>> samples) {

        if (samples == null || samples.isEmpty()) {
            return null;
        }

        TimeSeriesSlotPartition<K> partition = new TimeSeriesSlotPartition<>(samples.size());

        for (int position = 0; position < samples.size(); position++) {

            TsMAddValue<K> sample = samples.get(position);
            if (sample == null) {
                return null;
            }

            int slot = SlotHash.getSlot(codec.encodeKey(sample.getKey()));
            partition.samplesBySlot.computeIfAbsent(slot, k -> new ArrayList<>()).add(sample);
            partition.positionsBySlot.computeIfAbsent(slot, k -> new ArrayList<>()).add(position);
        }

        return partition;
    }

    /**
     * @return {@code true} if the samples hash to more than one slot.
     */
    boolean isMultiSlot() {
        return samplesBySlot.size() > 1;
    }

    /**
     * @return the samples per slot, each list in caller order.
     */
    Map<Integer, List<TsMAddValue<K>>> getSamplesBySlot() {
        return samplesBySlot;
    }

    /**
     * Put the per-slot results back into the caller's order.
     *
     * @param resultsBySlot the reply of each slot's command, one element per sample of that slot.
     * @return one element per sample, in caller order.
     */
    <T> List<T> restoreOrder(Map<Integer, List<T>> resultsBySlot) {

        List<T> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            result.add(null);
        }

        positionsBySlot.forEach((slot, positions) -> {
            List<T> slotResults = resultsBySlot.get(slot);
            for (int i = 0; i < positions.size(); i++) {
                result.set(positions.get(i), slotResults.get(i));
            }
        });

        return result;
    }

}
