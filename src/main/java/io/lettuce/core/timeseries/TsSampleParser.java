/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.ComplexData;
import io.lettuce.core.output.ComplexDataParser;

/**
 * Parser for Redis <a href="https://redis.io/commands/ts.get/">TS.GET</a> command output.
 * <p>
 * The reply is a flat {@code [timestamp, value]} tuple, or an empty array (not {@code nil}) when the series has no samples.
 * {@link #decode(ComplexData)} carries the actual tuple-to-{@link TsSample} conversion as a package-visible static method so
 * that a future list-returning parser (for {@code TS.RANGE}/{@code TS.REVRANGE}, which reuse the same per-sample tuple shape
 * nested inside an outer array) can call it per element instead of duplicating this logic; {@link TsSampleParser} itself only
 * ever produces a single, possibly {@code null}, {@link TsSample} for the single-sample {@code TS.GET} reply.
 *
 * @author Gyumin Hwang
 * @since 7.7
 */
public final class TsSampleParser implements ComplexDataParser<TsSample> {

    public static final TsSampleParser INSTANCE = new TsSampleParser();

    private TsSampleParser() {
    }

    @Override
    public TsSample parse(ComplexData data) {
        if (data == null) {
            throw new IllegalArgumentException("Failed parsing TS.GET: data must not be null");
        }
        return decode(data);
    }

    /**
     * Decodes a single {@code [timestamp, v0, v1, ...]} tuple into a {@link TsSample}, or {@code null} if the tuple is empty.
     *
     * @param data the tuple to decode, must not be {@code null}.
     * @return the decoded {@link TsSample}, or {@code null} if {@code data} is an empty array.
     */
    static TsSample decode(ComplexData data) {
        List<Object> tuple = data.getDynamicList();
        if (tuple == null || tuple.isEmpty()) {
            return null;
        }

        long timestamp = ((Number) tuple.get(0)).longValue();
        List<Double> values = new ArrayList<>(tuple.size() - 1);
        for (int i = 1; i < tuple.size(); i++) {
            values.add(toDouble(tuple.get(i)));
        }
        return new TsSample(timestamp, values);
    }

    private static Double toDouble(Object value) {
        if (value instanceof Double) {
            return (Double) value;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return Double.valueOf(StringCodec.UTF8.decodeValue((ByteBuffer) value));
    }

}
