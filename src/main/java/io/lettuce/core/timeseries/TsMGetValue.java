/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries;

import java.util.Collections;
import java.util.Map;

/**
 * Represents a single entry of the result of the Redis <a href="https://redis.io/commands/ts.mget/">TS.MGET</a> command.
 *
 * @param <K> Key type
 * @author Gyumin Hwang
 * @since 7.8
 */
public class TsMGetValue<K> {

    private final K key;

    private final Map<String, String> labels;

    private final TsSample sample;

    public TsMGetValue(K key, Map<String, String> labels, TsSample sample) {
        this.key = key;
        this.labels = labels == null ? Collections.emptyMap() : Collections.unmodifiableMap(labels);
        this.sample = sample;
    }

    /**
     * Returns the key of the series this sample belongs to.
     *
     * @return the key of the series this sample belongs to
     */
    public K getKey() {
        return key;
    }

    /**
     * Returns the labels of the series this sample belongs to.
     *
     * @return the labels of the series this sample belongs to, never {@code null}
     */
    public Map<String, String> getLabels() {
        return labels;
    }

    /**
     * Returns the last sample of the series, or {@code null} if the series has no samples.
     *
     * @return the last sample of the series, or {@code null} if the series has no samples
     */
    public TsSample getSample() {
        return sample;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof TsMGetValue))
            return false;

        TsMGetValue<?> that = (TsMGetValue<?>) o;

        if (key != null ? !key.equals(that.key) : that.key != null)
            return false;
        if (!labels.equals(that.labels))
            return false;
        return sample != null ? sample.equals(that.sample) : that.sample == null;
    }

    @Override
    public int hashCode() {
        int result = key != null ? key.hashCode() : 0;
        result = 31 * result + labels.hashCode();
        result = 31 * result + (sample != null ? sample.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return String.format("TsMGetValue[%s, %s, %s]", key, labels, sample);
    }

}
