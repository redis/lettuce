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
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.timeseries.TsDuplicatePolicy;
import io.lettuce.core.timeseries.TsDuplicatePolicyType;
import io.lettuce.core.timeseries.TsEncodingType;

/**
 * Unit tests for {@link TsAddArgs}: validation and serialization.
 */
@Tag(UNIT_TEST)
class TsAddArgsUnitTests {

    /**
     * Scenario A02b: every option, set in reverse order, is emitted in the documented order (design §3.2: RETENTION, ENCODING,
     * CHUNK_SIZE, DUPLICATE_POLICY, ON_DUPLICATE, IGNORE, LABELS) with LABELS last.
     */
    @Test
    void everyOptionInFixedOrder() {

        TsAddArgs args = new TsAddArgs().label("a", "1").ignore(7, 0.25).onDuplicate(TsDuplicatePolicyType.SUM)
                .duplicatePolicy(TsDuplicatePolicyType.FIRST).chunkSize(512).encoding(TsEncodingType.UNCOMPRESSED)
                .retention(100);

        assertThat(tokens(args)).containsExactly("RETENTION", "100", "ENCODING", "UNCOMPRESSED", "CHUNK_SIZE", "512",
                "DUPLICATE_POLICY", "FIRST", "ON_DUPLICATE", "SUM", "IGNORE", "7", "0.25", "LABELS", "a", "1");
    }

    /**
     * Scenario A02b: no option set sends nothing.
     */
    @Test
    void noOptionsSendsNothing() {
        assertThat(tokens(new TsAddArgs())).isEmpty();
    }

    /**
     * Scenario A02b: ON_DUPLICATE alone.
     */
    @Test
    void onDuplicateAlone() {

        assertThat(tokens(new TsAddArgs().onDuplicate(TsDuplicatePolicyType.MAX))).containsExactly("ON_DUPLICATE", "MAX");
        assertThat(tokens(new TsAddArgs().onDuplicate(TsDuplicatePolicy.of("keep")))).containsExactly("ON_DUPLICATE", "keep");
    }

    /**
     * Scenario A02b / A03: ON_DUPLICATE and DUPLICATE_POLICY are each emitted once; the last call wins.
     */
    @Test
    void eachOptionEmittedOnce() {

        TsAddArgs args = new TsAddArgs().onDuplicate(TsDuplicatePolicyType.MIN).onDuplicate(TsDuplicatePolicyType.LAST)
                .duplicatePolicy(TsDuplicatePolicyType.BLOCK).duplicatePolicy(TsDuplicatePolicyType.MAX).retention(1)
                .retention(2);

        assertThat(tokens(args)).containsExactly("RETENTION", "2", "DUPLICATE_POLICY", "MAX", "ON_DUPLICATE", "LAST");
    }

    /**
     * Scenario A02b: the static builder entry points set the same options as the fluent setters.
     */
    @Test
    void builderEntryPoints() {

        assertThat(tokens(TsAddArgs.Builder.retention(10))).containsExactly("RETENTION", "10");
        assertThat(tokens(TsAddArgs.Builder.retention(Duration.ofSeconds(2)))).containsExactly("RETENTION", "2000");
        assertThat(tokens(TsAddArgs.Builder.encoding(TsEncodingType.COMPRESSED))).containsExactly("ENCODING", "COMPRESSED");
        assertThat(tokens(TsAddArgs.Builder.chunkSize(64))).containsExactly("CHUNK_SIZE", "64");
        assertThat(tokens(TsAddArgs.Builder.duplicatePolicy(TsDuplicatePolicyType.SUM))).containsExactly("DUPLICATE_POLICY",
                "SUM");
        assertThat(tokens(TsAddArgs.Builder.onDuplicate(TsDuplicatePolicyType.FIRST))).containsExactly("ON_DUPLICATE", "FIRST");
        assertThat(tokens(TsAddArgs.Builder.ignore(5, 1.5))).containsExactly("IGNORE", "5", "1.5");
        assertThat(tokens(TsAddArgs.Builder.label("a", "b"))).containsExactly("LABELS", "a", "b");
        assertThat(tokens(TsAddArgs.Builder.labels(Collections.singletonMap("a", "b")))).containsExactly("LABELS", "a", "b");
    }

    /**
     * Scenario A02b: ADD arguments have no TIMESTAMP option (the timestamp is a positional argument of TS.ADD).
     */
    @Test
    void noTimestampOption() {

        assertThat(TsAddArgs.class.getMethods()).noneMatch(m -> m.getName().equals("timestamp"));
        assertThat(TsAddArgs.Builder.class.getMethods()).noneMatch(m -> m.getName().equals("timestamp"));
    }

    /**
     * Design §3.2: an empty label map sends a bare LABELS; labels keep insertion order and are copied.
     */
    @Test
    void labels() {

        assertThat(tokens(new TsAddArgs().labels(Collections.emptyMap()))).containsExactly("LABELS");

        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("z", "1");
        labels.put("a", "2");
        TsAddArgs args = new TsAddArgs().labels(labels);
        labels.put("late", "x");

        assertThat(tokens(args)).containsExactly("LABELS", "z", "1", "a", "2");
        assertThat(tokens(new TsAddArgs().labels(Collections.singletonMap("a", "1")).label("b", "2"))).containsExactly("LABELS",
                "a", "1", "b", "2");
    }

    /**
     * Design §3.2: setter validation from the Javadoc.
     */
    @Test
    void setterValidation() {

        assertThatThrownBy(() -> new TsAddArgs().onDuplicate(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TsAddArgs.Builder.onDuplicate(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsAddArgs().duplicatePolicy(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsAddArgs().encoding(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsAddArgs().retention(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsAddArgs().retention(Duration.ofNanos(1))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsAddArgs().retention((Duration) null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsAddArgs().chunkSize(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsAddArgs().ignore(-1, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsAddArgs().ignore(0, -0.5)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsAddArgs().labels(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsAddArgs().label(null, "v")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsAddArgs().label("n", null)).isInstanceOf(IllegalArgumentException.class);

        Map<String, String> nullValue = new HashMap<>();
        nullValue.put("n", null);
        assertThatThrownBy(() -> new TsAddArgs().labels(nullValue)).isInstanceOf(IllegalArgumentException.class);
    }

}
