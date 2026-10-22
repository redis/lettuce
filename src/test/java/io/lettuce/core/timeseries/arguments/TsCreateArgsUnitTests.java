/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries.arguments;

import static io.lettuce.TestTags.UNIT_TEST;
import static io.lettuce.core.timeseries.arguments.TsArgsTestSupport.tokens;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.timeseries.TsDuplicatePolicyType;
import io.lettuce.core.timeseries.TsEncodingType;

/**
 * Unit tests for {@link TsCreateArgs}: validation and serialization.
 */
@Tag(UNIT_TEST)
class TsCreateArgsUnitTests {

    /**
     * Scenario A01: every option, set in reverse order, is emitted in the documented order with LABELS last.
     */
    @Test
    void everyOptionInFixedOrder() {

        TsCreateArgs args = new TsCreateArgs().label("a", "1").ignore(7, 0.25).duplicatePolicy(TsDuplicatePolicyType.FIRST)
                .chunkSize(512).encoding(TsEncodingType.COMPRESSED).retention(100);

        assertThat(tokens(args)).containsExactly("RETENTION", "100", "ENCODING", "COMPRESSED", "CHUNK_SIZE", "512",
                "DUPLICATE_POLICY", "FIRST", "IGNORE", "7", "0.25", "LABELS", "a", "1");
    }

    /**
     * Scenario A01: no option set sends nothing.
     */
    @Test
    void noOptionsSendsNothing() {
        assertThat(tokens(new TsCreateArgs())).isEmpty();
    }

    /**
     * Scenario A01: the static builder entry points set the same options as the fluent setters.
     */
    @Test
    void builderEntryPoints() {

        assertThat(tokens(TsCreateArgs.Builder.retention(10))).containsExactly("RETENTION", "10");
        assertThat(tokens(TsCreateArgs.Builder.retention(Duration.ofSeconds(2)))).containsExactly("RETENTION", "2000");
        assertThat(tokens(TsCreateArgs.Builder.encoding(TsEncodingType.UNCOMPRESSED))).containsExactly("ENCODING",
                "UNCOMPRESSED");
        assertThat(tokens(TsCreateArgs.Builder.chunkSize(64))).containsExactly("CHUNK_SIZE", "64");
        assertThat(tokens(TsCreateArgs.Builder.duplicatePolicy(TsDuplicatePolicyType.SUM))).containsExactly("DUPLICATE_POLICY",
                "SUM");
        assertThat(tokens(TsCreateArgs.Builder.ignore(5, 1.5))).containsExactly("IGNORE", "5", "1.5");
        assertThat(tokens(TsCreateArgs.Builder.label("a", "b"))).containsExactly("LABELS", "a", "b");
        assertThat(tokens(TsCreateArgs.Builder.labels(Collections.singletonMap("a", "b")))).containsExactly("LABELS", "a", "b");
    }

    /**
     * Scenario A03: a setter called twice is emitted once with the last value.
     */
    @Test
    void duplicatePolicyEmittedOnce() {

        TsCreateArgs args = new TsCreateArgs().duplicatePolicy(TsDuplicatePolicyType.BLOCK)
                .duplicatePolicy(TsDuplicatePolicyType.MIN);

        assertThat(tokens(args)).containsExactly("DUPLICATE_POLICY", "MIN");
    }

    /**
     * Scenario A04: an empty label map sends a bare LABELS.
     */
    @Test
    void emptyLabelsSendBareLabels() {
        assertThat(tokens(new TsCreateArgs().labels(Collections.emptyMap()))).containsExactly("LABELS");
    }

    /**
     * Design §3.2: labels keep the insertion order of the given map and are copied, so later changes to the map do not leak.
     */
    @Test
    void labelsKeepOrderAndAreCopied() {

        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("z", "1");
        labels.put("a", "2");
        labels.put("m", "3");
        TsCreateArgs args = new TsCreateArgs().labels(labels);
        labels.put("late", "x");

        assertThat(tokens(args)).containsExactly("LABELS", "z", "1", "a", "2", "m", "3");
    }

    /**
     * Scenario A17 (retention): zero is allowed and means never expire.
     */
    @Test
    void retentionZeroAllowed() {

        assertThat(tokens(new TsCreateArgs().retention(0))).containsExactly("RETENTION", "0");
        assertThat(tokens(new TsCreateArgs().retention(Duration.ZERO))).containsExactly("RETENTION", "0");
    }

    /**
     * Scenario A17 (retention): a negative value throws.
     */
    @Test
    void retentionNegativeThrows() {

        assertThatThrownBy(() -> new TsCreateArgs().retention(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsCreateArgs().retention(Duration.ofMillis(-1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TsCreateArgs.Builder.retention(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Scenario A17 (retention): a non-zero duration shorter than one millisecond throws instead of rounding to 0 (never
     * expire).
     */
    @Test
    void retentionDurationBelowOneMillisecondThrows() {

        assertThatThrownBy(() -> new TsCreateArgs().retention(Duration.ofNanos(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsCreateArgs().retention(Duration.ofNanos(999_999)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TsCreateArgs.Builder.retention(Duration.ofNanos(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Scenario A17 (retention): one millisecond is the smallest non-zero duration and is sent in whole milliseconds.
     */
    @Test
    void retentionDurationOneMillisecond() {

        assertThat(tokens(new TsCreateArgs().retention(Duration.ofMillis(1)))).containsExactly("RETENTION", "1");
        assertThat(tokens(new TsCreateArgs().retention(Duration.ofMinutes(1)))).containsExactly("RETENTION", "60000");
    }

    @Test
    void retentionNullDurationThrows() {
        assertThatThrownBy(() -> new TsCreateArgs().retention((Duration) null)).isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Design §3.2: setter validation from the Javadoc.
     */
    @Test
    void setterValidation() {

        assertThatThrownBy(() -> new TsCreateArgs().chunkSize(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsCreateArgs().chunkSize(-8)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsCreateArgs().encoding(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsCreateArgs().duplicatePolicy(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsCreateArgs().ignore(-1, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsCreateArgs().ignore(0, -0.1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsCreateArgs().labels(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsCreateArgs().label(null, "v")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsCreateArgs().label("n", null)).isInstanceOf(IllegalArgumentException.class);

        Map<String, String> nullValue = new HashMap<>();
        nullValue.put("n", null);
        assertThatThrownBy(() -> new TsCreateArgs().labels(nullValue)).isInstanceOf(IllegalArgumentException.class);

        Map<String, String> nullName = new HashMap<>();
        nullName.put(null, "v");
        assertThatThrownBy(() -> new TsCreateArgs().labels(nullName)).isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Design §3.2: zero thresholds for IGNORE are allowed.
     */
    @Test
    void ignoreZeroAllowed() {
        List<String> tokens = tokens(new TsCreateArgs().ignore(0, 0));

        assertThat(tokens).hasSize(3);
        assertThat(tokens.subList(0, 2)).containsExactly("IGNORE", "0");
        assertThat(Double.parseDouble(tokens.get(2))).isEqualTo(0.0);
    }

}
