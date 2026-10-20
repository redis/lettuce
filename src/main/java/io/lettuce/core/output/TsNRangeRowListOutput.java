/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.output;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.timeseries.TsNRangeRow;

/**
 * {@link List} of {@link TsNRangeRow} output for a TS.NRANGE or TS.NREVRANGE reply: an array of rows
 * {@code [timestamp, [value1, …, valueN]]}. RESP2 sends the values as strings ({@code NaN}, {@code Infinity}, {@code -Infinity}
 * included), RESP3 as doubles. Each row is emitted to the subscriber as soon as it is decoded.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @since 8.0
 */
public class TsNRangeRowListOutput<K, V> extends CommandOutput<K, V, List<TsNRangeRow>>
        implements StreamingOutput<TsNRangeRow> {

    private final List<Integer> columnCounts;

    private boolean initialized;

    private Subscriber<TsNRangeRow> subscriber;

    private Long timestamp;

    private List<Double> values;

    /**
     * Create a new output.
     *
     * @param codec the codec, must not be {@code null}.
     * @param columnCounts the number of values of each key, in key order, must not be {@code null} or empty.
     */
    public TsNRangeRowListOutput(RedisCodec<K, V> codec, List<Integer> columnCounts) {

        super(codec, Collections.emptyList());

        LettuceAssert.notNull(columnCounts, "Column counts must not be null");
        LettuceAssert.isTrue(!columnCounts.isEmpty(), "Column counts must not be empty");

        this.columnCounts = columnCounts;
        setSubscriber(ListSubscriber.instance());
    }

    @Override
    public void set(long integer) {
        timestamp = integer;
    }

    @Override
    public void set(double number) {
        values().add(number);
    }

    @Override
    public void set(ByteBuffer bytes) {
        values().add(bytes == null ? Double.NaN : TsSampleRowDecoder.parseDouble(bytes));
    }

    @Override
    public void multi(int count) {

        if (!initialized) {
            output = OutputFactory.newList(count);
            initialized = true;
        }
    }

    @Override
    public void complete(int depth) {

        // depth 2: the value array of a row ends; depth 1: the row ends
        if (depth == 1 && timestamp != null) {
            if (values != null && !values.isEmpty()) {
                subscriber.onNext(output, TsNRangeRow.of(timestamp, values, columnCounts(values.size())));
            }
            timestamp = null;
            values = null;
        }
    }

    // the server sends the value count of the request; never fail decoding if a row does not match it
    private List<Integer> columnCounts(int valueCount) {

        int expected = 0;
        for (Integer count : columnCounts) {
            expected += count;
        }

        return expected == valueCount ? columnCounts : Collections.singletonList(valueCount);
    }

    @Override
    public void setSubscriber(Subscriber<TsNRangeRow> subscriber) {
        LettuceAssert.notNull(subscriber, "Subscriber must not be null");
        this.subscriber = subscriber;
    }

    @Override
    public Subscriber<TsNRangeRow> getSubscriber() {
        return subscriber;
    }

    private List<Double> values() {
        if (values == null) {
            values = new ArrayList<>();
        }
        return values;
    }

}
