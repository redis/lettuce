/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TsSampleParser}.
 * <p>
 * PLAN (Given/When/Then):
 * <ul>
 * <li>Given a {@code null} top-level reply, when {@code parse}, then reject (a {@code null} reply should never occur for
 * {@code TS.GET}; an empty series is an empty array, not nil, per the server source).</li>
 * <li>Given a 2-element {@code [timestamp, value]} tuple, when {@code parse}, then a single-value {@link TsSample} is
 * produced.</li>
 * <li>Given an empty array (no samples), when {@code parse}, then {@code null} is returned.</li>
 * <li>Given an N-tuple {@code [timestamp, v0, v1, ...]} (multiple aggregators), when {@code parse}, then all values are
 * preserved in declaration order.</li>
 * </ul>
 */
class TsSampleParserUnitTests {

    private final TsSampleParser parser = TsSampleParser.INSTANCE;

    private static ByteBuffer buf(String s) {
        return ByteBuffer.wrap(s.getBytes(StandardCharsets.UTF_8));
    }

    private static io.lettuce.core.output.ComplexData listData(Object... items) {
        List<Object> list = Arrays.asList(items);
        return new io.lettuce.core.output.ComplexData() {

            @Override
            public void storeObject(Object value) {
            }

            @Override
            public List<Object> getDynamicList() {
                return list;
            }

            @Override
            public boolean isList() {
                return true;
            }

        };
    }

    @Test
    void parseNullThrows() {
        assertThatThrownBy(() -> parser.parse(null)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("TS.GET");
    }

    @Test
    void parsesSingleValueSample() {
        TsSample sample = parser.parse(listData(1000L, buf("1.5")));

        assertThat(sample).isNotNull();
        assertThat(sample.getTimestamp()).isEqualTo(1000L);
        assertThat(sample.getValue()).isEqualTo(1.5);
    }

    @Test
    void emptyArrayYieldsNull() {
        assertThat(parser.parse(listData())).isNull();
    }

    @Test
    void multiAggregatorSampleIsPreserved() {
        TsSample sample = parser.parse(listData(1000L, 10.0d, 1.0d, 20.0d));

        assertThat(sample.getValues()).containsExactly(10.0, 1.0, 20.0);
    }

}
