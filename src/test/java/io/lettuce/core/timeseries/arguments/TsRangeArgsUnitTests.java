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

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.timeseries.TsAggregation;
import io.lettuce.core.timeseries.TsAggregationType;
import io.lettuce.core.timeseries.TsBucketTimestamp;

/**
 * Unit tests for {@link TsRangeArgs}: option order, the aggregation token, the options that require an aggregation, and setter
 * validation (design §3.3).
 */
@Tag(UNIT_TEST)
class TsRangeArgsUnitTests {

    /**
     * Scenario A08: no option set sends nothing.
     */
    @Test
    void emptyArgsSendNothing() {
        assertThat(tokens(new TsRangeArgs())).isEmpty();
    }

    /**
     * Scenario A08: every option, set in reverse order, is sent in the documented order: LATEST, FILTER_BY_TS, FILTER_BY_VALUE,
     * COUNT, ALIGN, AGGREGATION, BUCKETTIMESTAMP, EMPTY.
     */
    @Test
    void everyOptionInDocumentedOrder() {

        TsRangeArgs args = new TsRangeArgs().empty().bucketTimestamp(TsBucketTimestamp.MID)
                .aggregation(3000, TsAggregationType.AVG).align(1500).count(3).filterByValue(4.5, 5.5)
                .filterByTimestamps(1000, 3000, 5000).latest();

        assertThat(tokens(args)).containsExactly("LATEST", "FILTER_BY_TS", "1000", "3000", "5000", "FILTER_BY_VALUE", "4.5",
                "5.5", "COUNT", "3", "ALIGN", "1500", "AGGREGATION", "AVG", "3000", "BUCKETTIMESTAMP", "~", "EMPTY");
    }

    /**
     * Scenario A08: the same options in setter order give the same tokens.
     */
    @Test
    void setterOrderDoesNotMatter() {

        TsRangeArgs forward = new TsRangeArgs().latest().filterByTimestamps(1000, 2000).filterByValue(0.5, 1.5).count(2)
                .alignStart().aggregation(1000, TsAggregationType.SUM).bucketTimestamp(TsBucketTimestamp.END).empty();
        TsRangeArgs backward = new TsRangeArgs().empty().bucketTimestamp(TsBucketTimestamp.END)
                .aggregation(1000, TsAggregationType.SUM).alignStart().count(2).filterByValue(0.5, 1.5)
                .filterByTimestamps(1000, 2000).latest();

        assertThat(tokens(forward)).isEqualTo(tokens(backward)).containsExactly("LATEST", "FILTER_BY_TS", "1000", "2000",
                "FILTER_BY_VALUE", "0.5", "1.5", "COUNT", "2", "ALIGN", "start", "AGGREGATION", "SUM", "1000",
                "BUCKETTIMESTAMP", "+", "EMPTY");
    }

    /**
     * Scenario A08: each option on its own.
     */
    @Test
    void singleOptions() {

        assertThat(tokens(new TsRangeArgs().latest())).containsExactly("LATEST");
        assertThat(tokens(new TsRangeArgs().filterByTimestamps(7))).containsExactly("FILTER_BY_TS", "7");
        assertThat(tokens(new TsRangeArgs().filterByValue(-1.5, 2.5))).containsExactly("FILTER_BY_VALUE", "-1.5", "2.5");
        assertThat(tokens(new TsRangeArgs().count(10))).containsExactly("COUNT", "10");
        assertThat(tokens(new TsRangeArgs().aggregation(60000, TsAggregationType.MAX))).containsExactly("AGGREGATION", "MAX",
                "60000");
    }

    /**
     * Scenario A08: the ALIGN forms. {@code align(n)} sends the number, {@code alignStart()} and {@code alignEnd()} the bare
     * words {@code start} and {@code end} (design §3.4); the last call wins.
     */
    @Test
    void alignForms() {

        assertThat(tokens(new TsRangeArgs().aggregation(1000, TsAggregationType.AVG).align(0))).containsExactly("ALIGN", "0",
                "AGGREGATION", "AVG", "1000");
        assertThat(tokens(new TsRangeArgs().aggregation(1000, TsAggregationType.AVG).alignStart())).containsExactly("ALIGN",
                "start", "AGGREGATION", "AVG", "1000");
        assertThat(tokens(new TsRangeArgs().aggregation(1000, TsAggregationType.AVG).alignEnd())).containsExactly("ALIGN",
                "end", "AGGREGATION", "AVG", "1000");
        assertThat(tokens(new TsRangeArgs().aggregation(1000, TsAggregationType.AVG).alignStart().align(250)))
                .containsExactly("ALIGN", "250", "AGGREGATION", "AVG", "1000");
    }

    /**
     * Scenario A08 / C09: the BUCKETTIMESTAMP tokens {@code -}, {@code +}, {@code ~} (design §3.1).
     */
    @Test
    void bucketTimestampTokens() {

        assertThat(tokens(new TsRangeArgs().aggregation(1000, TsAggregationType.AVG).bucketTimestamp(TsBucketTimestamp.START)))
                .containsExactly("AGGREGATION", "AVG", "1000", "BUCKETTIMESTAMP", "-");
        assertThat(tokens(new TsRangeArgs().aggregation(1000, TsAggregationType.AVG).bucketTimestamp(TsBucketTimestamp.END)))
                .containsExactly("AGGREGATION", "AVG", "1000", "BUCKETTIMESTAMP", "+");
        assertThat(tokens(new TsRangeArgs().aggregation(1000, TsAggregationType.AVG).bucketTimestamp(TsBucketTimestamp.MID)))
                .containsExactly("AGGREGATION", "AVG", "1000", "BUCKETTIMESTAMP", "~");
    }

    /**
     * Scenario A07: several aggregations are one argument, joined by commas without spaces, followed by the bucket duration.
     */
    @Test
    void severalAggregationsAreOneArgument() {

        TsRangeArgs args = new TsRangeArgs().aggregation(1000, TsAggregationType.MIN, TsAggregationType.AVG,
                TsAggregationType.MAX);

        assertThat(tokens(args)).containsExactly("AGGREGATION", "MIN,AVG,MAX", "1000");
    }

    /**
     * Scenario A07: the tokens of design §3.1 ({@code STD.P}, {@code VAR.S}, {@code COUNTNAN}, {@code COUNTALL}, {@code TWA}),
     * and a raw value from {@code TsAggregation.of} sent exactly as given.
     */
    @Test
    void aggregationTokens() {

        assertThat(tokens(new TsRangeArgs().aggregation(1000, TsAggregationType.STD_P, TsAggregationType.VAR_S)))
                .containsExactly("AGGREGATION", "STD.P,VAR.S", "1000");
        assertThat(tokens(new TsRangeArgs().aggregation(1000, TsAggregationType.COUNTNAN, TsAggregationType.COUNTALL)))
                .containsExactly("AGGREGATION", "COUNTNAN,COUNTALL", "1000");
        assertThat(tokens(new TsRangeArgs().aggregation(1000, TsAggregationType.TWA))).containsExactly("AGGREGATION", "TWA",
                "1000");
        assertThat(tokens(new TsRangeArgs().aggregation(1000, TsAggregation.of("median"), TsAggregationType.AVG)))
                .containsExactly("AGGREGATION", "median,AVG", "1000");
        assertThat(tokens(new TsRangeArgs().aggregation(1000, TsAggregation.of("std.s")))).containsExactly("AGGREGATION",
                "STD.S", "1000");
    }

    /**
     * Scenario A07: the {@code Duration} overload sends whole milliseconds; zero is allowed, a non-zero duration under one
     * millisecond, a negative duration and {@code null} throw (Javadoc of {@code aggregation(Duration, …)}).
     */
    @Test
    void aggregationWithDuration() {

        assertThat(tokens(new TsRangeArgs().aggregation(Duration.ofSeconds(3), TsAggregationType.AVG)))
                .containsExactly("AGGREGATION", "AVG", "3000");
        assertThat(tokens(new TsRangeArgs().aggregation(Duration.ofMillis(1), TsAggregationType.AVG)))
                .containsExactly("AGGREGATION", "AVG", "1");
        assertThat(tokens(new TsRangeArgs().aggregation(Duration.ZERO, TsAggregationType.AVG))).containsExactly("AGGREGATION",
                "AVG", "0");

        assertThatThrownBy(() -> new TsRangeArgs().aggregation(Duration.ofNanos(1), TsAggregationType.AVG))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsRangeArgs().aggregation(Duration.ofMillis(-1), TsAggregationType.AVG))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsRangeArgs().aggregation((Duration) null, TsAggregationType.AVG))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Scenario A07: at least one aggregation and no {@code null} element.
     */
    @Test
    void aggregationValidation() {

        assertThatThrownBy(() -> new TsRangeArgs().aggregation(1000)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsRangeArgs().aggregation(1000, (TsAggregation[]) null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsRangeArgs().aggregation(1000, TsAggregationType.AVG, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TsRangeArgs.Builder.aggregation(1000)).isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Scenario A07: a second {@code aggregation} call replaces the first; AGGREGATION is sent once.
     */
    @Test
    void aggregationLastCallWins() {

        TsRangeArgs args = new TsRangeArgs().aggregation(1000, TsAggregationType.MIN).aggregation(2000, TsAggregationType.MAX,
                TsAggregationType.SUM);

        assertThat(tokens(args)).containsExactly("AGGREGATION", "MAX,SUM", "2000");
    }

    /**
     * Scenario A15: ALIGN, BUCKETTIMESTAMP or EMPTY without AGGREGATION throws {@link IllegalArgumentException} when the
     * arguments are built (design §3.3).
     */
    @Test
    void aggregationOnlyOptionsWithoutAggregation() {

        assertThatThrownBy(() -> tokens(new TsRangeArgs().align(1000))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tokens(new TsRangeArgs().alignStart())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tokens(new TsRangeArgs().alignEnd())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tokens(new TsRangeArgs().bucketTimestamp(TsBucketTimestamp.END)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tokens(new TsRangeArgs().empty())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tokens(new TsRangeArgs().latest().count(3).empty()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tokens(TsRangeArgs.Builder.empty())).isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Scenario A15: the aggregation may be set after the options that require it.
     */
    @Test
    void aggregationSetAfterDependentOptions() {

        TsRangeArgs args = TsRangeArgs.Builder.empty().alignEnd().bucketTimestamp(TsBucketTimestamp.START).aggregation(1000,
                TsAggregationType.LAST);

        assertThat(tokens(args)).containsExactly("ALIGN", "end", "AGGREGATION", "LAST", "1000", "BUCKETTIMESTAMP", "-",
                "EMPTY");
    }

    /**
     * Scenario A08: the options that do not require an aggregation are sent without one.
     */
    @Test
    void filtersAndCountWithoutAggregation() {

        assertThat(tokens(new TsRangeArgs().latest().filterByTimestamps(1).filterByValue(1.5, 2.5).count(1)))
                .containsExactly("LATEST", "FILTER_BY_TS", "1", "FILTER_BY_VALUE", "1.5", "2.5", "COUNT", "1");
    }

    /**
     * Scenario A08: setter validation from the Javadoc: {@code filterByTimestamps} must not be {@code null} or empty,
     * {@code bucketTimestamp} must not be {@code null}.
     */
    @Test
    void setterValidation() {

        assertThatThrownBy(() -> new TsRangeArgs().filterByTimestamps()).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsRangeArgs().filterByTimestamps((long[]) null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsRangeArgs().bucketTimestamp(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TsRangeArgs.Builder.filterByTimestamps()).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TsRangeArgs.Builder.bucketTimestamp(null)).isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Scenario A08: every builder factory gives the same tokens as the matching setter.
     */
    @Test
    void builderFactories() {

        assertThat(tokens(TsRangeArgs.Builder.latest())).isEqualTo(tokens(new TsRangeArgs().latest()));
        assertThat(tokens(TsRangeArgs.Builder.filterByTimestamps(1, 2)))
                .isEqualTo(tokens(new TsRangeArgs().filterByTimestamps(1, 2)));
        assertThat(tokens(TsRangeArgs.Builder.filterByValue(1.5, 2.5)))
                .isEqualTo(tokens(new TsRangeArgs().filterByValue(1.5, 2.5)));
        assertThat(tokens(TsRangeArgs.Builder.count(4))).isEqualTo(tokens(new TsRangeArgs().count(4)));
        assertThat(tokens(TsRangeArgs.Builder.aggregation(1000, TsAggregationType.AVG)))
                .isEqualTo(tokens(new TsRangeArgs().aggregation(1000, TsAggregationType.AVG)));
        assertThat(tokens(TsRangeArgs.Builder.aggregation(Duration.ofSeconds(1), TsAggregationType.AVG)))
                .isEqualTo(tokens(new TsRangeArgs().aggregation(1000, TsAggregationType.AVG)));
        assertThat(tokens(TsRangeArgs.Builder.align(5).aggregation(1000, TsAggregationType.AVG)))
                .isEqualTo(tokens(new TsRangeArgs().align(5).aggregation(1000, TsAggregationType.AVG)));
        assertThat(tokens(TsRangeArgs.Builder.alignStart().aggregation(1000, TsAggregationType.AVG)))
                .isEqualTo(tokens(new TsRangeArgs().alignStart().aggregation(1000, TsAggregationType.AVG)));
        assertThat(tokens(TsRangeArgs.Builder.alignEnd().aggregation(1000, TsAggregationType.AVG)))
                .isEqualTo(tokens(new TsRangeArgs().alignEnd().aggregation(1000, TsAggregationType.AVG)));
        assertThat(tokens(TsRangeArgs.Builder.bucketTimestamp(TsBucketTimestamp.MID).aggregation(1000, TsAggregationType.AVG)))
                .isEqualTo(tokens(
                        new TsRangeArgs().bucketTimestamp(TsBucketTimestamp.MID).aggregation(1000, TsAggregationType.AVG)));
        assertThat(tokens(TsRangeArgs.Builder.empty().aggregation(1000, TsAggregationType.AVG)))
                .isEqualTo(tokens(new TsRangeArgs().empty().aggregation(1000, TsAggregationType.AVG)));
    }

}
