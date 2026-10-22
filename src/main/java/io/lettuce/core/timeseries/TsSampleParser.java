/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.lettuce.core.output.ComplexData;

/**
 * Decodes samples nested inside a complex reply (TS.MGET, TS.MRANGE, TS.MREVRANGE). A sample is
 * {@code [timestamp, value1, …, valueN]}; RESP2 sends the values as strings ({@code NaN}, {@code Infinity}, {@code -Infinity}
 * included), RESP3 as doubles. Decodes the same cases as the dedicated sample outputs.
 *
 * @since 8.0
 */
final class TsSampleParser {

    private TsSampleParser() {
    }

    /**
     * Decode one sample.
     *
     * @return the sample, or {@code null} for an empty array or a nil value.
     */
    static TsSample sample(Object value) {

        if (!(value instanceof ComplexData)) {
            return null;
        }

        List<Object> row = ((ComplexData) value).getDynamicList();
        if (row.size() < 2) {
            return null;
        }

        List<Double> values = new ArrayList<>(row.size() - 1);
        for (int i = 1; i < row.size(); i++) {
            Double element = TsParserSupport.doubleValue(row.get(i));
            values.add(element == null ? Double.NaN : element);
        }

        return TsSample.of(TsParserSupport.longOrZero(row.get(0)), values);
    }

    /**
     * Decode an array of samples.
     *
     * @return the samples, never {@code null}; empty for an empty array or a nil value.
     */
    static List<TsSample> samples(Object value) {

        if (!(value instanceof ComplexData)) {
            return Collections.emptyList();
        }

        List<Object> rows = ((ComplexData) value).getDynamicList();
        List<TsSample> samples = new ArrayList<>(rows.size());
        for (Object row : rows) {
            TsSample sample = sample(row);
            if (sample != null) {
                samples.add(sample);
            }
        }

        return samples;
    }

}
