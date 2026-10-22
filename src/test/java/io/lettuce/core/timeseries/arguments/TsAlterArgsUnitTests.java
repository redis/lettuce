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

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.timeseries.TsDuplicatePolicy;
import io.lettuce.core.timeseries.TsDuplicatePolicyType;

/**
 * Unit tests for {@link TsAlterArgs}: validation and serialization.
 */
@Tag(UNIT_TEST)
class TsAlterArgsUnitTests {

    /**
     * Scenario A02a: every ALTER option, set in reverse order, is emitted as RETENTION, CHUNK_SIZE, DUPLICATE_POLICY, IGNORE,
     * LABELS.
     */
    @Test
    void everyOptionInFixedOrder() {

        TsAlterArgs args = new TsAlterArgs().label("a", "1").label("b", "2").ignore(3, 4.5)
                .duplicatePolicy(TsDuplicatePolicyType.MAX).chunkSize(1024).retention(Duration.ofSeconds(1));

        assertThat(tokens(args)).containsExactly("RETENTION", "1000", "CHUNK_SIZE", "1024", "DUPLICATE_POLICY", "MAX", "IGNORE",
                "3", "4.5", "LABELS", "a", "1", "b", "2");
    }

    /**
     * Scenario A02a: ALTER has no ENCODING option.
     */
    @Test
    void noEncodingOption() {
        assertThat(TsAlterArgs.class.getMethods()).noneMatch(m -> m.getName().equalsIgnoreCase("encoding"));
    }

    /**
     * Scenario A02a: the static builder entry points.
     */
    @Test
    void builderEntryPoints() {

        assertThat(tokens(TsAlterArgs.Builder.retention(10))).containsExactly("RETENTION", "10");
        assertThat(tokens(TsAlterArgs.Builder.retention(Duration.ofMillis(10)))).containsExactly("RETENTION", "10");
        assertThat(tokens(TsAlterArgs.Builder.chunkSize(64))).containsExactly("CHUNK_SIZE", "64");
        assertThat(tokens(TsAlterArgs.Builder.duplicatePolicy(TsDuplicatePolicy.of("keep"))))
                .containsExactly("DUPLICATE_POLICY", "keep");
        assertThat(tokens(TsAlterArgs.Builder.ignore(5, 1.5))).containsExactly("IGNORE", "5", "1.5");
        assertThat(tokens(TsAlterArgs.Builder.label("a", "b"))).containsExactly("LABELS", "a", "b");
        assertThat(tokens(TsAlterArgs.Builder.labels(Collections.emptyMap()))).containsExactly("LABELS");
    }

    /**
     * Scenario A03: each option is emitted once; the last call wins.
     */
    @Test
    void eachOptionEmittedOnce() {

        TsAlterArgs args = new TsAlterArgs().duplicatePolicy(TsDuplicatePolicyType.LAST)
                .duplicatePolicy(TsDuplicatePolicyType.FIRST).retention(5).retention(6);

        assertThat(tokens(args)).containsExactly("RETENTION", "6", "DUPLICATE_POLICY", "FIRST");
    }

    /**
     * Scenario A04: an empty label map sends a bare LABELS (removes all labels on ALTER).
     */
    @Test
    void emptyLabelsSendBareLabels() {

        assertThat(tokens(new TsAlterArgs().labels(Collections.emptyMap()))).containsExactly("LABELS");
        assertThat(tokens(new TsAlterArgs().retention(1).labels(Collections.emptyMap()))).containsExactly("RETENTION", "1",
                "LABELS");
    }

    /**
     * Scenario A04: {@code labels(emptyMap)} after {@code label(...)} replaces the earlier labels.
     */
    @Test
    void emptyLabelsReplaceEarlierLabels() {
        assertThat(tokens(new TsAlterArgs().label("a", "1").labels(Collections.emptyMap()))).containsExactly("LABELS");
    }

    /**
     * Scenario A17 (retention): validation on ALTER.
     */
    @Test
    void retentionValidation() {

        assertThat(tokens(new TsAlterArgs().retention(0))).containsExactly("RETENTION", "0");
        assertThat(tokens(new TsAlterArgs().retention(Duration.ZERO))).containsExactly("RETENTION", "0");
        assertThatThrownBy(() -> new TsAlterArgs().retention(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsAlterArgs().retention(Duration.ofNanos(500)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TsAlterArgs.Builder.retention(Duration.ofNanos(500)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void setterValidation() {

        assertThatThrownBy(() -> new TsAlterArgs().chunkSize(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsAlterArgs().duplicatePolicy(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsAlterArgs().ignore(-1, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsAlterArgs().ignore(1, -1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsAlterArgs().labels(null)).isInstanceOf(IllegalArgumentException.class);
    }

}
