/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries;

import java.util.Objects;

import io.lettuce.core.internal.LettuceAssert;

/**
 * One sample to add with the Redis <a href="https://redis.io/commands/ts.madd/">TS.MADD</a> command: a key, a timestamp and a
 * value. A sample created without a timestamp is sent with {@code *}, so the server clock sets its timestamp.
 * <p>
 * {@link TsMAddValue} is immutable.
 *
 * @param <K> Key type.
 * @since 8.0
 */
public final class TsMAddValue<K> {

    private final K key;

    private final Long timestamp;

    private final double value;

    private TsMAddValue(K key, Long timestamp, double value) {
        this.key = key;
        this.timestamp = timestamp;
        this.value = value;
    }

    /**
     * Create a sample with an explicit timestamp.
     *
     * @param key the key of the series, must not be {@code null}.
     * @param timestamp the timestamp in milliseconds, must not be negative.
     * @param value the value.
     * @param <K> Key type.
     * @return the sample.
     * @throws IllegalArgumentException if {@code key} is {@code null} or {@code timestamp} is negative.
     * @since 8.0
     */
    public static <K> TsMAddValue<K> of(K key, long timestamp, double value) {

        LettuceAssert.notNull(key, "Key must not be null");
        LettuceAssert.isTrue(timestamp >= 0, "Timestamp must not be negative");

        return new TsMAddValue<>(key, timestamp, value);
    }

    /**
     * Create a sample whose timestamp is set by the server clock. The sample is sent with timestamp {@code *}.
     *
     * @param key the key of the series, must not be {@code null}.
     * @param value the value.
     * @param <K> Key type.
     * @return the sample.
     * @throws IllegalArgumentException if {@code key} is {@code null}.
     * @since 8.0
     */
    public static <K> TsMAddValue<K> of(K key, double value) {

        LettuceAssert.notNull(key, "Key must not be null");

        return new TsMAddValue<>(key, null, value);
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
     * Get the timestamp of the sample.
     *
     * @return the timestamp in milliseconds, or {@code null} if the server clock sets it ({@code *}).
     * @since 8.0
     */
    public Long getTimestamp() {
        return timestamp;
    }

    /**
     * Get the value of the sample.
     *
     * @return the value.
     * @since 8.0
     */
    public double getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }

        if (!(o instanceof TsMAddValue)) {
            return false;
        }

        TsMAddValue<?> that = (TsMAddValue<?>) o;
        return Double.compare(value, that.value) == 0 && key.equals(that.key) && Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, timestamp, value);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [key=" + key + ", timestamp=" + (timestamp == null ? "*" : timestamp) + ", value="
                + value + "]";
    }

}
