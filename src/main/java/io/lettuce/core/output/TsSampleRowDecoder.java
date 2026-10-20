/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.output;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import io.lettuce.core.internal.LettuceStrings;
import io.lettuce.core.timeseries.TsSample;

/**
 * Decodes one TimeSeries sample row {@code [timestamp, value1, …, valueN]}. RESP2 sends the values as simple strings
 * ({@code 4.5}, {@code NaN}, {@code Infinity}, {@code -Infinity}), RESP3 as doubles.
 *
 * @since 8.0
 */
final class TsSampleRowDecoder {

    private Long timestamp;

    private List<Double> values;

    void timestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    void value(double value) {
        values().add(value);
    }

    void value(ByteBuffer bytes) {
        values().add(bytes == null ? Double.NaN : parseDouble(bytes));
    }

    /**
     * @return the sample of the current row, or {@code null} if the row is empty; resets the decoder.
     */
    TsSample complete() {

        TsSample sample = null;
        if (timestamp != null && values != null && !values.isEmpty()) {
            sample = TsSample.of(timestamp, values);
        }

        timestamp = null;
        values = null;
        return sample;
    }

    static double parseDouble(ByteBuffer bytes) {
        return LettuceStrings.toDouble(StandardCharsets.US_ASCII.decode(bytes.duplicate()).toString());
    }

    private List<Double> values() {
        if (values == null) {
            values = new ArrayList<>(1);
        }
        return values;
    }

}
