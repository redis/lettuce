/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Latest sample of one time series, as returned by the Redis <a href="https://redis.io/commands/ts.mget/">TS.MGET</a> command,
 * with its labels if they were requested. RESP2 and RESP3 replies produce equal values. {@link TsMGetValue} is immutable.
 *
 * @param <K> Key type.
 * @since 8.0
 */
public final class TsMGetValue<K> {

    private final K key;

    private final Map<String, String> labels;

    private final TsSample sample;

    TsMGetValue(K key, Map<String, String> labels, TsSample sample) {
        this.key = key;
        this.labels = Collections.unmodifiableMap(labels);
        this.sample = sample;
    }

    /**
     * Get the key of the series.
     *
     * @return the key, never {@code null}.
     * @since 8.0
     */
    public K getKey() {
        return key;
    }

    /**
     * Get the labels of the series, in server order. Empty unless labels were requested with {@code WITHLABELS} or
     * {@code SELECTED_LABELS}. A selected label that the series does not have has a {@code null} value.
     *
     * @return the labels, never {@code null}.
     * @since 8.0
     */
    public Map<String, String> getLabels() {
        return labels;
    }

    /**
     * Get the latest sample of the series.
     *
     * @return the sample, or {@code null} if the series has no samples.
     * @since 8.0
     */
    public TsSample getSample() {
        return sample;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }

        if (!(o instanceof TsMGetValue)) {
            return false;
        }

        TsMGetValue<?> that = (TsMGetValue<?>) o;
        return Objects.equals(key, that.key) && labels.equals(that.labels) && Objects.equals(sample, that.sample);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, labels, sample);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [key=" + key + ", labels=" + labels + ", sample=" + sample + "]";
    }

}
