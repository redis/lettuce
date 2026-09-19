/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries;

import io.lettuce.core.internal.LettuceAssert;

/**
 * A single (key, timestamp, value) input to append via <a href="https://redis.io/commands/ts.madd/">TS.MADD</a>.
 *
 * @param <K> Key type.
 * @author Gyumin Hwang
 * @since 7.8
 */
public class TsMAddValue<K> {

    private final K key;

    private final long timestamp;

    private final double value;

    private TsMAddValue(K key, long timestamp, double value) {
        this.key = key;
        this.timestamp = timestamp;
        this.value = value;
    }

    /**
     * Creates a new {@link TsMAddValue}.
     *
     * @param key the key. Must not be {@code null}.
     * @param timestamp the sample timestamp, in milliseconds.
     * @param value the sample value.
     * @param <K> Key type.
     * @return the {@link TsMAddValue}.
     */
    public static <K> TsMAddValue<K> of(K key, long timestamp, double value) {

        LettuceAssert.notNull(key, "Key must not be null");

        return new TsMAddValue<>(key, timestamp, value);
    }

    /**
     * Returns the key of the series this value should be appended to.
     *
     * @return the key of the series this value should be appended to
     */
    public K getKey() {
        return key;
    }

    /**
     * Returns the sample timestamp, in milliseconds.
     *
     * @return the sample timestamp, in milliseconds
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Returns the sample value.
     *
     * @return the sample value
     */
    public double getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof TsMAddValue))
            return false;

        TsMAddValue<?> that = (TsMAddValue<?>) o;

        if (timestamp != that.timestamp)
            return false;
        if (Double.compare(that.value, value) != 0)
            return false;
        return key != null ? key.equals(that.key) : that.key == null;
    }

    @Override
    public int hashCode() {
        int result = key != null ? key.hashCode() : 0;
        result = 31 * result + (int) (timestamp ^ (timestamp >>> 32));
        long temp = Double.doubleToLongBits(value);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return String.format("TsMAddValue[%s, %d, %f]", key, timestamp, value);
    }

}
