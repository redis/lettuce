/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TsMGetValue}.
 *
 * <p>
 * PLAN (Given/When/Then):
 * <ul>
 * <li>Given two {@link TsMGetValue} instances built from the same key/labels/sample, when compared, then they are {@code equal}
 * and share the same {@code hashCode()}.</li>
 * <li>Given two {@link TsMGetValue} instances that differ in key, labels or sample, when compared, then they are not
 * {@code equal}.</li>
 * <li>Given a {@link TsMGetValue} with a {@code null} sample (series with no samples), when compared to an otherwise identical
 * instance, then they are still {@code equal}.</li>
 * </ul>
 */
@Tag(UNIT_TEST)
class TsMGetValueUnitTests {

    private static Map<String, String> labels(String k, String v) {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put(k, v);
        return labels;
    }

    @Test
    void equalInstancesHaveEqualHashCode() {
        TsSample sample = new TsSample(1000L, Collections.singletonList(1.5));
        TsMGetValue<String> first = new TsMGetValue<>("key1", labels("region", "us"), sample);
        TsMGetValue<String> second = new TsMGetValue<>("key1", labels("region", "us"),
                new TsSample(1000L, Collections.singletonList(1.5)));

        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }

    @Test
    void differingKeyLabelsOrSampleAreNotEqual() {
        TsSample sample = new TsSample(1000L, Collections.singletonList(1.5));
        TsMGetValue<String> base = new TsMGetValue<>("key1", labels("region", "us"), sample);

        assertThat(base).isNotEqualTo(new TsMGetValue<>("key2", labels("region", "us"), sample));
        assertThat(base).isNotEqualTo(new TsMGetValue<>("key1", labels("region", "eu"), sample));
        assertThat(base).isNotEqualTo(
                new TsMGetValue<>("key1", labels("region", "us"), new TsSample(2000L, Collections.singletonList(1.5))));
    }

    @Test
    void nullSampleIsEqualToNullSample() {
        TsMGetValue<String> first = new TsMGetValue<>("key1", labels("region", "us"), null);
        TsMGetValue<String> second = new TsMGetValue<>("key1", labels("region", "us"), null);

        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }

}
