/*
 * Copyright (c) 2026-Present, Redis Ltd.
 * All rights reserved.
 *
 * SPDX-License-Identifier: MIT
 */
package io.lettuce.core.timeseries.arguments;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.CompositeArgument;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.timeseries.TsAggregation;
import io.lettuce.core.timeseries.TsAggregationType;
import io.lettuce.core.timeseries.TsBucketTimestamp;
import io.lettuce.core.timeseries.TsFilter;
import io.lettuce.core.timeseries.TsReducer;
import io.lettuce.core.timeseries.TsReducerType;

/**
 * Unit tests for {@link TsMRangeArgs}: option order with the filters and GROUPBY (design §3.3), conflicts, validation and the
 * getters the reply parser uses.
 */
@Tag(UNIT_TEST)
class TsMRangeArgsUnitTests {

    private static final List<TsFilter> FILTERS = Arrays.asList(TsFilter.equal("t", "x"), TsFilter.exists("area"));

    private static List<String> tokens(TsMRangeArgs args, List<TsFilter> filters) {

        return TsArgsTestSupport.tokens(new CompositeArgument() {

            @Override
            public <K, V> void build(CommandArgs<K, V> commandArgs) {
                args.build(commandArgs, filters);
            }

        });
    }

    private static List<String> tokens(TsMRangeArgs args) {
        return tokens(args, FILTERS);
    }

    /**
     * Scenario A09: no option set sends only the filters.
     */
    @Test
    void noOptionsSendOnlyFilters() {
        assertThat(tokens(new TsMRangeArgs())).containsExactly("FILTER", "t=x", "area!=");
    }

    /**
     * Scenario A09: every option except GROUPBY, set in reverse order, is sent in the documented order: LATEST, FILTER_BY_TS,
     * FILTER_BY_VALUE, WITHLABELS, COUNT, ALIGN, AGGREGATION, BUCKETTIMESTAMP, EMPTY, EXCLUDEEMPTY, then FILTER (EXCLUDEEMPTY
     * before FILTER, design §3.3).
     */
    @Test
    void everyOptionWithExcludeEmptyInDocumentedOrder() {

        TsMRangeArgs args = new TsMRangeArgs().excludeEmpty().empty().bucketTimestamp(TsBucketTimestamp.MID)
                .aggregation(3000, TsAggregationType.AVG).align(1500).count(3).withLabels().filterByValue(4.5, 5.5)
                .filterByTimestamps(1000, 3000).latest();

        assertThat(tokens(args)).containsExactly("LATEST", "FILTER_BY_TS", "1000", "3000", "FILTER_BY_VALUE", "4.5", "5.5",
                "WITHLABELS", "COUNT", "3", "ALIGN", "1500", "AGGREGATION", "AVG", "3000", "BUCKETTIMESTAMP", "~", "EMPTY",
                "EXCLUDEEMPTY", "FILTER", "t=x", "area!=");
    }

    /**
     * Scenario A09: every option except EXCLUDEEMPTY, in reverse order, with SELECTED_LABELS and GROUPBY: GROUPBY l REDUCE r
     * comes after the filters.
     */
    @Test
    void everyOptionWithGroupByInDocumentedOrder() {

        TsMRangeArgs args = new TsMRangeArgs().groupBy("grp", TsReducerType.STD_P).empty()
                .bucketTimestamp(TsBucketTimestamp.END).aggregation(1000, TsAggregationType.SUM).alignStart().count(2)
                .selectedLabels("area", "grp").filterByValue(0.5, 1.5).filterByTimestamps(1000).latest();

        assertThat(tokens(args)).containsExactly("LATEST", "FILTER_BY_TS", "1000", "FILTER_BY_VALUE", "0.5", "1.5",
                "SELECTED_LABELS", "area", "grp", "COUNT", "2", "ALIGN", "start", "AGGREGATION", "SUM", "1000",
                "BUCKETTIMESTAMP", "+", "EMPTY", "FILTER", "t=x", "area!=", "GROUPBY", "grp", "REDUCE", "STD.P");
    }

    /**
     * Scenario A09: the setter order does not change the tokens.
     */
    @Test
    void setterOrderDoesNotMatter() {

        TsMRangeArgs forward = new TsMRangeArgs().latest().withLabels().count(5).aggregation(1000, TsAggregationType.MAX)
                .excludeEmpty();
        TsMRangeArgs backward = new TsMRangeArgs().excludeEmpty().aggregation(1000, TsAggregationType.MAX).count(5).withLabels()
                .latest();

        assertThat(tokens(forward)).isEqualTo(tokens(backward));
    }

    /**
     * Scenario A09: EXCLUDEEMPTY is emitted once even if the setter is called twice.
     */
    @Test
    void excludeEmptyEmittedOnce() {
        assertThat(
                tokens(new TsMRangeArgs().excludeEmpty().excludeEmpty(), Collections.singletonList(TsFilter.equal("t", "x"))))
                        .containsExactly("EXCLUDEEMPTY", "FILTER", "t=x");
    }

    /**
     * Scenario A09: several aggregations are one comma-joined argument; ALIGN end and the Duration bucket.
     */
    @Test
    void severalAggregationsOneArgument() {

        assertThat(tokens(new TsMRangeArgs().aggregation(Duration.ofSeconds(3), TsAggregationType.MIN, TsAggregationType.AVG,
                TsAggregationType.MAX))).containsExactly("AGGREGATION", "MIN,AVG,MAX", "3000", "FILTER", "t=x", "area!=");
        assertThat(tokens(new TsMRangeArgs().aggregation(1000, TsAggregationType.STD_S, TsAggregation.of("median")).alignEnd()))
                .containsExactly("ALIGN", "end", "AGGREGATION", "STD.S,median", "1000", "FILTER", "t=x", "area!=");
    }

    /**
     * Scenario A09: GROUPBY with a reducer that Lettuce does not know is sent as given.
     */
    @Test
    void groupByRawReducer() {
        assertThat(tokens(new TsMRangeArgs().groupBy("grp", TsReducer.of("median")),
                Collections.singletonList(TsFilter.equal("t", "x")))).containsExactly("FILTER", "t=x", "GROUPBY", "grp",
                        "REDUCE", "median");
    }

    /**
     * Scenario A09: the {@link TsMRangeArgs.Builder} factories set one option each.
     */
    @Test
    void builderFactories() {

        List<TsFilter> one = Collections.singletonList(TsFilter.equal("t", "x"));
        assertThat(tokens(TsMRangeArgs.Builder.latest(), one)).containsExactly("LATEST", "FILTER", "t=x");
        assertThat(tokens(TsMRangeArgs.Builder.withLabels(), one)).containsExactly("WITHLABELS", "FILTER", "t=x");
        assertThat(tokens(TsMRangeArgs.Builder.selectedLabels("a"), one)).containsExactly("SELECTED_LABELS", "a", "FILTER",
                "t=x");
        assertThat(tokens(TsMRangeArgs.Builder.count(7), one)).containsExactly("COUNT", "7", "FILTER", "t=x");
        assertThat(tokens(TsMRangeArgs.Builder.excludeEmpty(), one)).containsExactly("EXCLUDEEMPTY", "FILTER", "t=x");
        assertThat(tokens(TsMRangeArgs.Builder.groupBy("g", TsReducerType.SUM), one)).containsExactly("FILTER", "t=x",
                "GROUPBY", "g", "REDUCE", "SUM");
        assertThat(tokens(TsMRangeArgs.Builder.aggregation(500, TsAggregationType.COUNT), one)).containsExactly("AGGREGATION",
                "COUNT", "500", "FILTER", "t=x");
        assertThat(tokens(TsMRangeArgs.Builder.filterByTimestamps(1, 2), one)).containsExactly("FILTER_BY_TS", "1", "2",
                "FILTER", "t=x");
        assertThat(tokens(TsMRangeArgs.Builder.filterByValue(1.5, 2.5), one)).containsExactly("FILTER_BY_VALUE", "1.5", "2.5",
                "FILTER", "t=x");
    }

    // --- A14: conflicts ----------------------------------------------------------------------------------------------

    /**
     * Scenario A14: WITHLABELS together with SELECTED_LABELS → {@link IllegalArgumentException}, in both setter orders.
     */
    @Test
    void withLabelsAndSelectedLabelsConflict() {

        assertThatThrownBy(() -> new TsMRangeArgs().withLabels().selectedLabels("a"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsMRangeArgs().selectedLabels("a").withLabels())
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Scenario A14: GROUPBY together with several aggregations → {@link IllegalArgumentException}, in both setter orders; one
     * aggregation is allowed.
     */
    @Test
    void groupByAndSeveralAggregationsConflict() {

        assertThatThrownBy(() -> new TsMRangeArgs().aggregation(1000, TsAggregationType.MIN, TsAggregationType.MAX)
                .groupBy("grp", TsReducerType.SUM)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsMRangeArgs().groupBy("grp", TsReducerType.SUM).aggregation(1000, TsAggregationType.MIN,
                TsAggregationType.MAX)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsMRangeArgs().groupBy("grp", TsReducerType.SUM).aggregation(Duration.ofSeconds(1),
                TsAggregationType.MIN, TsAggregationType.MAX)).isInstanceOf(IllegalArgumentException.class);

        assertThat(tokens(new TsMRangeArgs().groupBy("grp", TsReducerType.SUM).aggregation(1000, TsAggregationType.MAX),
                Collections.singletonList(TsFilter.equal("t", "x")))).containsExactly("AGGREGATION", "MAX", "1000", "FILTER",
                        "t=x", "GROUPBY", "grp", "REDUCE", "SUM");
    }

    /**
     * Scenario A14: GROUPBY together with EXCLUDEEMPTY → {@link IllegalArgumentException}, in both setter orders.
     */
    @Test
    void groupByAndExcludeEmptyConflict() {

        assertThatThrownBy(() -> new TsMRangeArgs().excludeEmpty().groupBy("grp", TsReducerType.SUM))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsMRangeArgs().groupBy("grp", TsReducerType.SUM).excludeEmpty())
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Scenario A15 (MRANGE): ALIGN, BUCKETTIMESTAMP and EMPTY without AGGREGATION → {@link IllegalArgumentException} when the
     * arguments are built.
     */
    @Test
    void aggregationOnlyOptionsWithoutAggregation() {

        assertThatThrownBy(() -> tokens(new TsMRangeArgs().align(0))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tokens(new TsMRangeArgs().alignStart())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tokens(new TsMRangeArgs().bucketTimestamp(TsBucketTimestamp.END)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tokens(new TsMRangeArgs().empty())).isInstanceOf(IllegalArgumentException.class);
    }

    // --- validation --------------------------------------------------------------------------------------------------

    /**
     * Scenario A13 (filters): {@code build} with {@code null}, empty or {@code null}-containing filters →
     * {@link IllegalArgumentException}.
     */
    @Test
    void invalidFiltersThrow() {

        assertThatThrownBy(() -> tokens(new TsMRangeArgs(), null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tokens(new TsMRangeArgs(), Collections.emptyList()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tokens(new TsMRangeArgs(), Arrays.asList(TsFilter.equal("t", "x"), null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Scenario A13: invalid setter arguments → {@link IllegalArgumentException}, per the Javadoc.
     */
    @Test
    void invalidSetterArgumentsThrow() {

        assertThatThrownBy(() -> new TsMRangeArgs().selectedLabels()).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsMRangeArgs().selectedLabels((String[]) null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsMRangeArgs().selectedLabels("a", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsMRangeArgs().groupBy(null, TsReducerType.SUM))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsMRangeArgs().groupBy("", TsReducerType.SUM))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsMRangeArgs().groupBy("grp", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsMRangeArgs().aggregation(1000)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsMRangeArgs().aggregation(1000, (TsAggregation[]) null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsMRangeArgs().aggregation(1000, TsAggregationType.AVG, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsMRangeArgs().aggregation((Duration) null, TsAggregationType.AVG))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsMRangeArgs().aggregation(Duration.ofNanos(10), TsAggregationType.AVG))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- getters -----------------------------------------------------------------------------------------------------

    /**
     * Design §3.3: the getters return the request's aggregations, group label and reducer; empty / {@code null} when unset.
     */
    @Test
    void getters() {

        TsMRangeArgs none = new TsMRangeArgs();
        assertThat(none.getAggregations()).isEmpty();
        assertThat(none.getGroupByLabel()).isNull();
        assertThat(none.getReducer()).isNull();

        TsMRangeArgs several = new TsMRangeArgs().aggregation(1000, TsAggregationType.MIN, TsAggregationType.AVG);
        assertThat(several.getAggregations()).containsExactly(TsAggregationType.MIN, TsAggregationType.AVG);

        TsMRangeArgs grouped = new TsMRangeArgs().groupBy("grp", TsReducerType.VAR_S).aggregation(1000, TsAggregationType.AVG);
        assertThat(grouped.getGroupByLabel()).isEqualTo("grp");
        assertThat(grouped.getReducer()).isSameAs(TsReducerType.VAR_S);
        assertThat(grouped.getAggregations()).containsExactly(TsAggregationType.AVG);
    }

}
