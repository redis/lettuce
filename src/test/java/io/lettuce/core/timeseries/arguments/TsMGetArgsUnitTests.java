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

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TsMGetArgs}: option order, the label-mode conflict and validation (design §3.3).
 */
@Tag(UNIT_TEST)
class TsMGetArgsUnitTests {

    /**
     * Scenario A09 (MGET): no option sends nothing.
     */
    @Test
    void emptyArgsSendNothing() {
        assertThat(tokens(new TsMGetArgs())).isEmpty();
    }

    /**
     * Scenario A09 (MGET): LATEST comes before the label mode, in either setter order.
     */
    @Test
    void latestBeforeLabels() {

        assertThat(tokens(new TsMGetArgs().withLabels().latest())).containsExactly("LATEST", "WITHLABELS");
        assertThat(tokens(new TsMGetArgs().latest().withLabels())).containsExactly("LATEST", "WITHLABELS");
        assertThat(tokens(new TsMGetArgs().selectedLabels("area", "missing").latest())).containsExactly("LATEST",
                "SELECTED_LABELS", "area", "missing");
    }

    /**
     * Scenario A09 (MGET): the {@link TsMGetArgs.Builder} factories set one option each.
     */
    @Test
    void builderFactories() {

        assertThat(tokens(TsMGetArgs.Builder.latest())).containsExactly("LATEST");
        assertThat(tokens(TsMGetArgs.Builder.withLabels())).containsExactly("WITHLABELS");
        assertThat(tokens(TsMGetArgs.Builder.selectedLabels("a", "b"))).containsExactly("SELECTED_LABELS", "a", "b");
    }

    /**
     * Scenario A14 (MGET): WITHLABELS together with SELECTED_LABELS → {@link IllegalArgumentException}, in both setter orders.
     */
    @Test
    void withLabelsAndSelectedLabelsConflict() {

        assertThatThrownBy(() -> new TsMGetArgs().withLabels().selectedLabels("a"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsMGetArgs().selectedLabels("a").withLabels())
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TsMGetArgs.Builder.withLabels().selectedLabels("a"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Scenario A13 (MGET): {@code selectedLabels} with {@code null}, no labels or a {@code null} label →
     * {@link IllegalArgumentException}.
     */
    @Test
    void invalidSelectedLabelsThrow() {

        assertThatThrownBy(() -> new TsMGetArgs().selectedLabels()).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsMGetArgs().selectedLabels((String[]) null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsMGetArgs().selectedLabels("a", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TsMGetArgs.Builder.selectedLabels()).isInstanceOf(IllegalArgumentException.class);
    }

}
