/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries;

import java.util.Collections;
import java.util.List;

/**
 * Represents a single sample of a RedisTimeSeries series, as returned by
 * <a href="https://redis.io/commands/ts.get/">TS.GET</a>, <a href="https://redis.io/commands/ts.range/">TS.RANGE</a>,
 * <a href="https://redis.io/commands/ts.revrange/">TS.REVRANGE</a> and
 * <a href="https://redis.io/commands/ts.mget/">TS.MGET</a>.
 * <p>
 * Most queries return one value per sample, in which case {@link #getValue()} returns it directly. Queries issued with multiple
 * aggregators (e.g. {@code AGGREGATION avg,min}) return one value per aggregator per bucket; in that case all values are
 * accessible, in declaration order, via {@link #getValues()}.
 *
 * @author Gyumin Hwang
 * @since 7.7
 */
public class TsSample {

    private final long timestamp;

    private final List<Double> values;

    public TsSample(long timestamp, List<Double> values) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("Failed constructing TsSample: values must not be null or empty");
        }
        this.timestamp = timestamp;
        this.values = Collections.unmodifiableList(values);
    }

    /**
     * Returns the timestamp of this sample.
     *
     * @return the timestamp of this sample
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Returns all values of this sample, in the order in which they were returned by the server. Contains a single element
     * unless the query was issued with multiple aggregators.
     *
     * @return all values of this sample, never {@code null} or empty
     */
    public List<Double> getValues() {
        return values;
    }

    /**
     * Returns the first value of this sample. Equivalent to {@code getValues().get(0)}.
     *
     * @return the first value of this sample
     */
    public double getValue() {
        return values.get(0);
    }

}
