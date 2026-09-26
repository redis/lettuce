/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link TsMAddValue}.
 *
 * <p>
 * PLAN (Given/When/Then):
 * <ul>
 * <li>Given a key, timestamp and value, when {@code TsMAddValue.of(...)} is called, then {@code getKey()},
 * {@code getTimestamp()} and {@code getValue()} return exactly what was passed in.</li>
 * <li>Given two {@link TsMAddValue} instances built from the same key/timestamp/value, when compared, then they are
 * {@code equal} and share the same {@code hashCode()}.</li>
 * <li>Given two {@link TsMAddValue} instances that differ in key, timestamp or value, when compared, then they are not
 * {@code equal}.</li>
 * <li>Given a {@code null} key, when {@code TsMAddValue.of(...)} is called, then an {@link IllegalArgumentException} is thrown
 * instead of constructing an invalid instance.</li>
 * </ul>
 */
@Tag(UNIT_TEST)
class TsMAddValueUnitTests {

    @Test
    void ofExposesKeyTimestampAndValue() {
        TsMAddValue<String> value = TsMAddValue.of("temperature:raw", 1000, 23.5);

        assertThat(value.getKey()).isEqualTo("temperature:raw");
        assertThat(value.getTimestamp()).isEqualTo(1000L);
        assertThat(value.getValue()).isEqualTo(23.5);
    }

    @Test
    void equalInstancesHaveEqualHashCode() {
        TsMAddValue<String> first = TsMAddValue.of("temperature:raw", 1000, 23.5);
        TsMAddValue<String> second = TsMAddValue.of("temperature:raw", 1000, 23.5);

        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }

    @Test
    void differingKeyTimestampOrValueAreNotEqual() {
        TsMAddValue<String> base = TsMAddValue.of("temperature:raw", 1000, 23.5);

        assertThat(base).isNotEqualTo(TsMAddValue.of("temperature:hourly", 1000, 23.5));
        assertThat(base).isNotEqualTo(TsMAddValue.of("temperature:raw", 2000, 23.5));
        assertThat(base).isNotEqualTo(TsMAddValue.of("temperature:raw", 1000, 24.5));
    }

    @Test
    void ofRejectsNullKey() {
        assertThatThrownBy(() -> TsMAddValue.of(null, 1000, 23.5)).isInstanceOf(IllegalArgumentException.class);
    }

}
