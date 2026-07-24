/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TsSample}.
 *
 * @author Gyumin Hwang
 * @since 7.7
 */
class TsSampleUnitTests {

    // ---------------------------------------------------------------------------
    // Given a single-value sample (the common case, one value per aggregator)
    // ---------------------------------------------------------------------------

    @Test
    void singleValueSampleExposesGetValueAndGetValues() {
        TsSample sample = new TsSample(1000L, Collections.singletonList(42.5));

        assertThat(sample.getTimestamp()).isEqualTo(1000L);
        assertThat(sample.getValue()).isEqualTo(42.5);
        assertThat(sample.getValues()).containsExactly(42.5);
    }

    // ---------------------------------------------------------------------------
    // Given a multi-aggregator N-tuple sample (e.g. AGGREGATION avg,min)
    // ---------------------------------------------------------------------------

    @Test
    void multiValueSamplePreservesDeclarationOrder() {
        List<Double> values = Arrays.asList(10.0, 1.0, 20.0);

        TsSample sample = new TsSample(2000L, values);

        assertThat(sample.getTimestamp()).isEqualTo(2000L);
        assertThat(sample.getValue()).isEqualTo(10.0);
        assertThat(sample.getValues()).containsExactly(10.0, 1.0, 20.0);
    }

    // ---------------------------------------------------------------------------
    // Given null or empty values, When constructing, Then reject (invariant: values.size() >= 1)
    // ---------------------------------------------------------------------------

    @Test
    void nullValuesThrows() {
        assertThatThrownBy(() -> new TsSample(1L, null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void emptyValuesThrows() {
        assertThatThrownBy(() -> new TsSample(1L, Collections.emptyList())).isInstanceOf(IllegalArgumentException.class);
    }

    // ---------------------------------------------------------------------------
    // Values list is immutable
    // ---------------------------------------------------------------------------

    @Test
    void valuesListIsUnmodifiable() {
        TsSample sample = new TsSample(1L, Arrays.asList(1.0, 2.0));

        assertThatThrownBy(() -> sample.getValues().add(3.0)).isInstanceOf(UnsupportedOperationException.class);
    }

}
