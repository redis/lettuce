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
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.timeseries.TsDuplicatePolicyType;
import io.lettuce.core.timeseries.TsEncodingType;

/**
 * Unit tests for {@link TsIncrByArgs}: validation and serialization.
 */
@Tag(UNIT_TEST)
class TsIncrByArgsUnitTests {

    /**
     * Scenario A02b: every option, set in reverse order, is emitted in the documented order with TIMESTAMP first and LABELS
     * last.
     */
    @Test
    void everyOptionInFixedOrderTimestampFirst() {

        TsIncrByArgs args = new TsIncrByArgs().label("a", "1").ignore(7, 0.25).duplicatePolicy(TsDuplicatePolicyType.LAST)
                .chunkSize(512).encoding(TsEncodingType.UNCOMPRESSED).retention(100).timestamp(1000);

        assertThat(tokens(args)).containsExactly("TIMESTAMP", "1000", "RETENTION", "100", "ENCODING", "UNCOMPRESSED",
                "CHUNK_SIZE", "512", "DUPLICATE_POLICY", "LAST", "IGNORE", "7", "0.25", "LABELS", "a", "1");
    }

    /**
     * Scenario A02b: no option set sends nothing; the server clock sets the timestamp.
     */
    @Test
    void noOptionsSendsNothing() {
        assertThat(tokens(new TsIncrByArgs())).isEmpty();
    }

    /**
     * Scenario A02b: TIMESTAMP alone, and TIMESTAMP 0 is allowed.
     */
    @Test
    void timestampAlone() {

        assertThat(tokens(new TsIncrByArgs().timestamp(1000))).containsExactly("TIMESTAMP", "1000");
        assertThat(tokens(new TsIncrByArgs().timestamp(0))).containsExactly("TIMESTAMP", "0");
        assertThat(tokens(TsIncrByArgs.Builder.timestamp(5))).containsExactly("TIMESTAMP", "5");
    }

    /**
     * Scenario A02b / A03: TIMESTAMP is emitted once; the last call wins.
     */
    @Test
    void timestampEmittedOnce() {
        assertThat(tokens(new TsIncrByArgs().timestamp(1).timestamp(2))).containsExactly("TIMESTAMP", "2");
    }

    /**
     * Scenario A02b: ON_DUPLICATE exists only on ADD.
     */
    @Test
    void noOnDuplicateOption() {

        assertThat(TsIncrByArgs.class.getMethods()).noneMatch(m -> m.getName().equalsIgnoreCase("onDuplicate"));
        assertThat(TsIncrByArgs.Builder.class.getMethods()).noneMatch(m -> m.getName().equalsIgnoreCase("onDuplicate"));
    }

    /**
     * Scenario A02b: the static builder entry points set the same options as the fluent setters.
     */
    @Test
    void builderEntryPoints() {

        assertThat(tokens(TsIncrByArgs.Builder.retention(10))).containsExactly("RETENTION", "10");
        assertThat(tokens(TsIncrByArgs.Builder.retention(Duration.ofSeconds(2)))).containsExactly("RETENTION", "2000");
        assertThat(tokens(TsIncrByArgs.Builder.encoding(TsEncodingType.COMPRESSED))).containsExactly("ENCODING", "COMPRESSED");
        assertThat(tokens(TsIncrByArgs.Builder.chunkSize(64))).containsExactly("CHUNK_SIZE", "64");
        assertThat(tokens(TsIncrByArgs.Builder.duplicatePolicy(TsDuplicatePolicyType.SUM))).containsExactly("DUPLICATE_POLICY",
                "SUM");
        assertThat(tokens(TsIncrByArgs.Builder.ignore(5, 1.5))).containsExactly("IGNORE", "5", "1.5");
        assertThat(tokens(TsIncrByArgs.Builder.label("a", "b"))).containsExactly("LABELS", "a", "b");
        assertThat(tokens(TsIncrByArgs.Builder.labels(Collections.singletonMap("a", "b")))).containsExactly("LABELS", "a", "b");
        assertThat(tokens(TsIncrByArgs.Builder.labels(Collections.emptyMap()))).containsExactly("LABELS");
    }

    /**
     * Design §3.2: {@code timestamp(long)} must be ≥ 0; the other setters validate as on CREATE.
     */
    @Test
    void setterValidation() {

        assertThatThrownBy(() -> new TsIncrByArgs().timestamp(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TsIncrByArgs.Builder.timestamp(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsIncrByArgs().duplicatePolicy(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsIncrByArgs().encoding(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsIncrByArgs().retention(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsIncrByArgs().retention(Duration.ofNanos(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsIncrByArgs().chunkSize(-8)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsIncrByArgs().ignore(-1, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsIncrByArgs().labels(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsIncrByArgs().label(null, "v")).isInstanceOf(IllegalArgumentException.class);

        Map<String, String> nullName = new HashMap<>();
        nullName.put(null, "v");
        assertThatThrownBy(() -> new TsIncrByArgs().labels(nullName)).isInstanceOf(IllegalArgumentException.class);
    }

}
