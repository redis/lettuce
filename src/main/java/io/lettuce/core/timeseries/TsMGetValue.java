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
 * @since 7.7
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

}
