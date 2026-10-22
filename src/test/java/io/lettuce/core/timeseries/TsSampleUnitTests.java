/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TsSample}: equality with {@code Double.compare} semantics, single- and multi-value samples (design
 * §3.5).
 */
@Tag(UNIT_TEST)
class TsSampleUnitTests {

    /**
     * Scenario U01: NaN equals NaN, with the same hash code.
     */
    @Test
    void nanEqualsNan() {

        assertThat(TsSample.of(1000, Double.NaN)).isEqualTo(TsSample.of(1000, Double.NaN))
                .hasSameHashCodeAs(TsSample.of(1000, Double.NaN));
        assertThat(TsSample.of(1000, Arrays.asList(1.0, Double.NaN)))
                .isEqualTo(TsSample.of(1000, Arrays.asList(1.0, Double.NaN)));
    }

    /**
     * Scenario U01: +0.0 is not equal to −0.0 ({@code Double.compare} semantics).
     */
    @Test
    void positiveAndNegativeZeroDiffer() {
        assertThat(TsSample.of(1000, 0.0)).isNotEqualTo(TsSample.of(1000, -0.0));
    }

    /**
     * Scenario U01: a single-value sample equals the same sample built with a one-element list, symmetrically and with the same
     * hash code (Jedis #4537).
     */
    @Test
    void singleValueEqualsOneElementList() {

        TsSample single = TsSample.of(1000, 2.5);
        TsSample list = TsSample.of(1000, Collections.singletonList(2.5));

        assertThat(single).isEqualTo(list);
        assertThat(list).isEqualTo(single);
        assertThat(single.hashCode()).isEqualTo(list.hashCode());
        assertThat(single.getValues()).containsExactly(2.5);
    }

    /**
     * Scenario U01: {@code getValue()} is the first of {@code getValues()}.
     */
    @Test
    void getValueIsFirstValue() {

        TsSample sample = TsSample.of(3000, Arrays.asList(4.0, 4.333333333333333, 5.0));

        assertThat(sample.getTimestamp()).isEqualTo(3000);
        assertThat(sample.getValue()).isEqualTo(4.0);
        assertThat(sample.getValues()).containsExactly(4.0, 4.333333333333333, 5.0);
        assertThat(TsSample.of(1, -7.5).getValue()).isEqualTo(-7.5);
    }

    /**
     * Scenario U01: samples that differ in timestamp, in a value, in the order of values or in the number of values are not
     * equal.
     */
    @Test
    void inequality() {

        TsSample sample = TsSample.of(1000, Arrays.asList(1.0, 2.0));

        assertThat(sample).isNotEqualTo(TsSample.of(1001, Arrays.asList(1.0, 2.0)));
        assertThat(sample).isNotEqualTo(TsSample.of(1000, Arrays.asList(1.0, 3.0)));
        assertThat(sample).isNotEqualTo(TsSample.of(1000, Arrays.asList(2.0, 1.0)));
        assertThat(sample).isNotEqualTo(TsSample.of(1000, 1.0));
        assertThat(sample).isNotEqualTo(TsSample.of(1000, Arrays.asList(1.0, 2.0, 3.0)));
        assertThat(sample).isNotEqualTo(null);
        assertThat(sample).isNotEqualTo("1000");
    }

    /**
     * Scenario U01: infinities compare by value.
     */
    @Test
    void infinities() {

        assertThat(TsSample.of(1, Double.POSITIVE_INFINITY)).isEqualTo(TsSample.of(1, Double.POSITIVE_INFINITY));
        assertThat(TsSample.of(1, Double.POSITIVE_INFINITY)).isNotEqualTo(TsSample.of(1, Double.NEGATIVE_INFINITY));
    }

    /**
     * Scenario U01: the sample is immutable. The values list cannot be modified, and a later change to the list passed to
     * {@code of} does not change the sample (design §3.5: all result types are immutable).
     */
    @Test
    void immutable() {

        List<Double> values = new ArrayList<>(Arrays.asList(1.0, 2.0));
        TsSample sample = TsSample.of(1000, values);
        values.set(0, 99.0);
        values.add(3.0);

        assertThat(sample.getValues()).containsExactly(1.0, 2.0);
        assertThatThrownBy(() -> sample.getValues().add(4.0)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> TsSample.of(1, 1.0).getValues().set(0, 2.0)).isInstanceOf(UnsupportedOperationException.class);
    }

    /**
     * Scenario U01: {@code of(ts, values)} rejects {@code null}, an empty list and {@code null} elements (Javadoc).
     */
    @Test
    void validation() {

        assertThatThrownBy(() -> TsSample.of(1, (List<Double>) null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TsSample.of(1, Collections.emptyList())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TsSample.of(1, Arrays.asList(1.0, null))).isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Scenario U01: {@code toString()} names the timestamp and the values.
     */
    @Test
    void toStringContainsTimestampAndValues() {
        assertThat(TsSample.of(1000, Arrays.asList(1.5, Double.NaN)).toString()).contains("1000").contains("1.5")
                .contains("NaN");
    }

}
