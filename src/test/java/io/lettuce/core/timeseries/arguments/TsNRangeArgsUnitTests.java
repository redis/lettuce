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

/**
 * Unit tests for {@link TsNRangeArgs}: one aggregation argument per key, {@code aggregationForEachKey}, the key-count check,
 * the option order (design §3.3) and the per-key column counts the reply output uses.
 */
@Tag(UNIT_TEST)
class TsNRangeArgsUnitTests {

    private static List<String> tokens(TsNRangeArgs args, int keyCount) {

        return TsArgsTestSupport.tokens(new CompositeArgument() {

            @Override
            public <K, V> void build(CommandArgs<K, V> commandArgs) {
                args.build(commandArgs, keyCount);
            }

        });
    }

    @SafeVarargs
    private static List<List<TsAggregation>> perKey(List<TsAggregation>... lists) {
        return Arrays.asList(lists);
    }

    private static List<TsAggregation> aggs(TsAggregation... aggregations) {
        return Arrays.asList(aggregations);
    }

    /**
     * Scenario A10: no option set sends nothing; the command builder emits {@code numkeys}, the keys and the range.
     */
    @Test
    void noOptionsSendNothing() {
        assertThat(tokens(new TsNRangeArgs(), 2)).isEmpty();
    }

    /**
     * Scenario A10: one spec per key, in key order. Per-key lists are joined by commas: {@code [[MIN,MAX],[SUM]]} →
     * {@code AGGREGATION MIN,MAX SUM bucket}.
     */
    @Test
    void oneAggregationArgumentPerKey() {

        TsNRangeArgs args = new TsNRangeArgs().aggregation(10,
                perKey(aggs(TsAggregationType.MIN, TsAggregationType.MAX), aggs(TsAggregationType.SUM)));

        assertThat(tokens(args, 2)).containsExactly("AGGREGATION", "MIN,MAX", "SUM", "10");
    }

    /**
     * Scenario A10 / E04: the server HLD's flow case {@code AGGREGATION sum sum,count 10} for two keys.
     */
    @Test
    void flowCaseSumThenSumCount() {

        TsNRangeArgs args = new TsNRangeArgs().aggregation(10,
                perKey(aggs(TsAggregationType.SUM), aggs(TsAggregationType.SUM, TsAggregationType.COUNT)));

        assertThat(tokens(args, 2)).containsExactly("AGGREGATION", "SUM", "SUM,COUNT", "10");
    }

    /**
     * Scenario A10 (H24): {@code aggregationForEachKey(min,max)} for 2 keys → {@code MIN,MAX MIN,MAX}; for 3 keys three times.
     * The server never broadcasts one spec to every key.
     */
    @Test
    void aggregationForEachKeyRepeatsTheSpecPerKey() {

        TsNRangeArgs args = new TsNRangeArgs().aggregationForEachKey(1000, TsAggregationType.MIN, TsAggregationType.MAX);

        assertThat(tokens(args, 2)).containsExactly("AGGREGATION", "MIN,MAX", "MIN,MAX", "1000");
        assertThat(tokens(args, 3)).containsExactly("AGGREGATION", "MIN,MAX", "MIN,MAX", "MIN,MAX", "1000");
        assertThat(tokens(new TsNRangeArgs().aggregationForEachKey(5, TsAggregationType.AVG), 1)).containsExactly("AGGREGATION",
                "AVG", "5");
    }

    /**
     * Scenario E06 (H24): {@code aggregationForEachKey} sends the same tokens as the explicitly repeated list.
     */
    @Test
    void aggregationForEachKeyEqualsTheRepeatedList() {

        TsNRangeArgs forEach = new TsNRangeArgs().aggregationForEachKey(10, TsAggregationType.SUM, TsAggregationType.COUNT);
        TsNRangeArgs explicit = new TsNRangeArgs().aggregation(10, perKey(aggs(TsAggregationType.SUM, TsAggregationType.COUNT),
                aggs(TsAggregationType.SUM, TsAggregationType.COUNT)));

        assertThat(tokens(forEach, 2)).isEqualTo(tokens(explicit, 2));
        assertThat(forEach.getColumnCounts(2)).isEqualTo(explicit.getColumnCounts(2)).containsExactly(2, 2);
    }

    /**
     * Scenario A10: the tokens of design §3.1 and a raw value from {@code TsAggregation.of}, sent exactly as given.
     */
    @Test
    void aggregationTokens() {

        TsNRangeArgs args = new TsNRangeArgs().aggregation(1000,
                perKey(aggs(TsAggregationType.STD_P, TsAggregationType.COUNTNAN),
                        aggs(TsAggregation.of("median"), TsAggregationType.TWA)));

        assertThat(tokens(args, 2)).containsExactly("AGGREGATION", "STD.P,COUNTNAN", "median,TWA", "1000");
    }

    /**
     * Scenario A10: every option, set in reverse order, is sent in the documented order LATEST, FILTER_BY_TS, FILTER_BY_VALUE,
     * COUNT, ALIGN, AGGREGATION (one argument per key, then the bucket), BUCKETTIMESTAMP, EMPTY (design §3.3; Javadoc of
     * {@code build}).
     */
    @Test
    void everyOptionInDocumentedOrder() {

        TsNRangeArgs args = new TsNRangeArgs().empty().bucketTimestamp(TsBucketTimestamp.MID)
                .aggregation(3000, perKey(aggs(TsAggregationType.AVG), aggs(TsAggregationType.MAX))).align(1500).count(3)
                .filterByValue(4.5, 5.5).filterByTimestamps(1000, 3000).latest();

        assertThat(tokens(args, 2)).containsExactly("LATEST", "FILTER_BY_TS", "1000", "3000", "FILTER_BY_VALUE", "4.5", "5.5",
                "COUNT", "3", "ALIGN", "1500", "AGGREGATION", "AVG", "MAX", "3000", "BUCKETTIMESTAMP", "~", "EMPTY");
    }

    /**
     * Scenario A10: the ALIGN forms {@code start} and {@code end} are bare words.
     */
    @Test
    void alignForms() {

        assertThat(tokens(new TsNRangeArgs().aggregationForEachKey(10, TsAggregationType.AVG).alignStart(), 2))
                .containsExactly("ALIGN", "start", "AGGREGATION", "AVG", "AVG", "10");
        assertThat(tokens(new TsNRangeArgs().aggregationForEachKey(10, TsAggregationType.AVG).alignEnd(), 2))
                .containsExactly("ALIGN", "end", "AGGREGATION", "AVG", "AVG", "10");
    }

    /**
     * Scenario A10: the options that need no aggregation are sent without one.
     */
    @Test
    void filtersAndCountWithoutAggregation() {

        assertThat(tokens(new TsNRangeArgs().latest().filterByTimestamps(0, 12).filterByValue(1.5, 27.5).count(3), 2))
                .containsExactly("LATEST", "FILTER_BY_TS", "0", "12", "FILTER_BY_VALUE", "1.5", "27.5", "COUNT", "3");
    }

    /**
     * Scenario A10: the {@code Duration} overloads send whole milliseconds and validate like the other range arguments.
     */
    @Test
    void aggregationWithDuration() {

        assertThat(tokens(new TsNRangeArgs().aggregation(Duration.ofSeconds(1),
                perKey(aggs(TsAggregationType.SUM), aggs(TsAggregationType.SUM))), 2)).containsExactly("AGGREGATION", "SUM",
                        "SUM", "1000");
        assertThat(tokens(new TsNRangeArgs().aggregationForEachKey(Duration.ofMillis(10), TsAggregationType.SUM), 2))
                .containsExactly("AGGREGATION", "SUM", "SUM", "10");

        assertThatThrownBy(() -> new TsNRangeArgs().aggregationForEachKey(Duration.ofNanos(1), TsAggregationType.SUM))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsNRangeArgs().aggregationForEachKey(Duration.ofMillis(-1), TsAggregationType.SUM))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsNRangeArgs().aggregationForEachKey((Duration) null, TsAggregationType.SUM))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsNRangeArgs().aggregation(Duration.ofMillis(-1), perKey(aggs(TsAggregationType.SUM))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsNRangeArgs().aggregation((Duration) null, perKey(aggs(TsAggregationType.SUM))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Scenario A17: the aggregation bucket duration must be positive. A {@code long} of 0 or less and a {@code Duration} of
     * zero, under one millisecond (counts as 0) or negative throw {@link IllegalArgumentException} before anything is sent, on
     * the setters and the {@code Builder} factories (design §2.2, H19).
     */
    @Test
    void bucketDurationMustBePositive() {

        assertThatThrownBy(() -> new TsNRangeArgs().aggregation(0, perKey(aggs(TsAggregationType.SUM))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsNRangeArgs().aggregation(-1, perKey(aggs(TsAggregationType.SUM))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsNRangeArgs().aggregation(Duration.ZERO, perKey(aggs(TsAggregationType.SUM))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsNRangeArgs().aggregation(Duration.ofNanos(999_999), perKey(aggs(TsAggregationType.SUM))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsNRangeArgs().aggregationForEachKey(0, TsAggregationType.SUM))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsNRangeArgs().aggregationForEachKey(-1, TsAggregationType.SUM))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsNRangeArgs().aggregationForEachKey(Duration.ZERO, TsAggregationType.SUM))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TsNRangeArgs.Builder.aggregation(0, perKey(aggs(TsAggregationType.SUM))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TsNRangeArgs.Builder.aggregation(Duration.ZERO, perKey(aggs(TsAggregationType.SUM))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TsNRangeArgs.Builder.aggregationForEachKey(0, TsAggregationType.SUM))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TsNRangeArgs.Builder.aggregationForEachKey(Duration.ZERO, TsAggregationType.SUM))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Scenario A16: spec count ≠ key count → {@link IllegalArgumentException} when the arguments are built (design §3.3:
     * {@code build(CommandArgs, int keyCount)}).
     */
    @Test
    void specCountMustMatchKeyCount() {

        TsNRangeArgs two = new TsNRangeArgs().aggregation(10, perKey(aggs(TsAggregationType.SUM), aggs(TsAggregationType.MAX)));

        assertThatThrownBy(() -> tokens(two, 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tokens(two, 3)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> two.getColumnCounts(3)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tokens(new TsNRangeArgs().aggregation(10, perKey(aggs(TsAggregationType.SUM))), 2))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Scenario A16: an empty inner list, a {@code null} inner list, a {@code null} aggregation, a {@code null} or empty outer
     * list → {@link IllegalArgumentException} from the setter (Javadoc of {@code aggregation(long, List)}).
     */
    @Test
    void perKeyListValidation() {

        assertThatThrownBy(() -> new TsNRangeArgs().aggregation(10,
                perKey(aggs(TsAggregationType.SUM), Collections.<TsAggregation> emptyList())))
                        .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsNRangeArgs().aggregation(10, perKey(aggs(TsAggregationType.SUM), null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsNRangeArgs().aggregation(10, perKey(aggs(TsAggregationType.SUM, null))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsNRangeArgs().aggregation(10, Collections.<List<TsAggregation>> emptyList()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsNRangeArgs().aggregation(10, (List<List<TsAggregation>>) null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TsNRangeArgs.Builder.aggregation(10, Collections.<List<TsAggregation>> emptyList()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Scenario A16: {@code aggregationForEachKey} needs at least one aggregation and no {@code null} element.
     */
    @Test
    void aggregationForEachKeyValidation() {

        assertThatThrownBy(() -> new TsNRangeArgs().aggregationForEachKey(10)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsNRangeArgs().aggregationForEachKey(10, (TsAggregation[]) null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsNRangeArgs().aggregationForEachKey(10, TsAggregationType.SUM, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TsNRangeArgs.Builder.aggregationForEachKey(10)).isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Scenario A10: {@code aggregation} and {@code aggregationForEachKey} replace each other (Javadoc: "Replaces …"); the last
     * call wins and AGGREGATION is sent once.
     */
    @Test
    void lastAggregationCallWins() {

        TsNRangeArgs forEachLast = new TsNRangeArgs()
                .aggregation(10, perKey(aggs(TsAggregationType.SUM), aggs(TsAggregationType.MAX)))
                .aggregationForEachKey(20, TsAggregationType.AVG);
        assertThat(tokens(forEachLast, 3)).containsExactly("AGGREGATION", "AVG", "AVG", "AVG", "20");

        TsNRangeArgs listLast = new TsNRangeArgs().aggregationForEachKey(20, TsAggregationType.AVG).aggregation(10,
                perKey(aggs(TsAggregationType.SUM), aggs(TsAggregationType.MAX)));
        assertThat(tokens(listLast, 2)).containsExactly("AGGREGATION", "SUM", "MAX", "10");
        assertThatThrownBy(() -> tokens(listLast, 3)).isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Scenario A16 (H14): ALIGN, BUCKETTIMESTAMP or EMPTY without AGGREGATION throws {@link IllegalArgumentException} when the
     * arguments are built.
     */
    @Test
    void aggregationOnlyOptionsWithoutAggregation() {

        assertThatThrownBy(() -> tokens(new TsNRangeArgs().align(0), 2)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tokens(new TsNRangeArgs().alignStart(), 2)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tokens(new TsNRangeArgs().bucketTimestamp(TsBucketTimestamp.END), 2))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tokens(new TsNRangeArgs().empty(), 2)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tokens(TsNRangeArgs.Builder.empty(), 2)).isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Scenario A16: a key count that is not positive → {@link IllegalArgumentException} (Javadoc of {@code build} and
     * {@code getColumnCounts}).
     */
    @Test
    void keyCountMustBePositive() {

        assertThatThrownBy(() -> tokens(new TsNRangeArgs(), 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsNRangeArgs().getColumnCounts(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TsNRangeArgs().getColumnCounts(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Scenario U05: the column count per key: 1 for every key without an aggregation, the size of each key's list with
     * {@code aggregation}, the repeated size with {@code aggregationForEachKey}.
     */
    @Test
    void columnCounts() {

        assertThat(new TsNRangeArgs().getColumnCounts(3)).containsExactly(1, 1, 1);
        assertThat(new TsNRangeArgs().latest().count(2).getColumnCounts(2)).containsExactly(1, 1);
        assertThat(new TsNRangeArgs()
                .aggregation(10, perKey(aggs(TsAggregationType.SUM), aggs(TsAggregationType.SUM, TsAggregationType.COUNT)))
                .getColumnCounts(2)).containsExactly(1, 2);
        assertThat(new TsNRangeArgs()
                .aggregationForEachKey(10, TsAggregationType.MIN, TsAggregationType.AVG, TsAggregationType.MAX)
                .getColumnCounts(2)).containsExactly(3, 3);
    }

    /**
     * Scenario A10: every builder factory gives the same tokens as the matching setter.
     */
    @Test
    void builderFactories() {

        List<List<TsAggregation>> lists = perKey(aggs(TsAggregationType.SUM), aggs(TsAggregationType.MAX));

        assertThat(tokens(TsNRangeArgs.Builder.latest(), 2)).isEqualTo(tokens(new TsNRangeArgs().latest(), 2));
        assertThat(tokens(TsNRangeArgs.Builder.filterByTimestamps(1, 2), 2))
                .isEqualTo(tokens(new TsNRangeArgs().filterByTimestamps(1, 2), 2));
        assertThat(tokens(TsNRangeArgs.Builder.filterByValue(1.5, 2.5), 2))
                .isEqualTo(tokens(new TsNRangeArgs().filterByValue(1.5, 2.5), 2));
        assertThat(tokens(TsNRangeArgs.Builder.count(4), 2)).isEqualTo(tokens(new TsNRangeArgs().count(4), 2));
        assertThat(tokens(TsNRangeArgs.Builder.aggregation(10, lists), 2))
                .isEqualTo(tokens(new TsNRangeArgs().aggregation(10, lists), 2));
        assertThat(tokens(TsNRangeArgs.Builder.aggregation(Duration.ofMillis(10), lists), 2))
                .isEqualTo(tokens(new TsNRangeArgs().aggregation(10, lists), 2));
        assertThat(tokens(TsNRangeArgs.Builder.aggregationForEachKey(10, TsAggregationType.AVG), 2))
                .isEqualTo(tokens(new TsNRangeArgs().aggregationForEachKey(10, TsAggregationType.AVG), 2));
        assertThat(tokens(TsNRangeArgs.Builder.aggregationForEachKey(Duration.ofMillis(10), TsAggregationType.AVG), 2))
                .isEqualTo(tokens(new TsNRangeArgs().aggregationForEachKey(10, TsAggregationType.AVG), 2));
        assertThat(tokens(TsNRangeArgs.Builder.align(5).aggregation(10, lists), 2))
                .isEqualTo(tokens(new TsNRangeArgs().align(5).aggregation(10, lists), 2));
        assertThat(tokens(TsNRangeArgs.Builder.alignStart().aggregation(10, lists), 2))
                .isEqualTo(tokens(new TsNRangeArgs().alignStart().aggregation(10, lists), 2));
        assertThat(tokens(TsNRangeArgs.Builder.alignEnd().aggregation(10, lists), 2))
                .isEqualTo(tokens(new TsNRangeArgs().alignEnd().aggregation(10, lists), 2));
        assertThat(tokens(TsNRangeArgs.Builder.bucketTimestamp(TsBucketTimestamp.END).aggregation(10, lists), 2))
                .isEqualTo(tokens(new TsNRangeArgs().bucketTimestamp(TsBucketTimestamp.END).aggregation(10, lists), 2));
        assertThat(tokens(TsNRangeArgs.Builder.empty().aggregation(10, lists), 2))
                .isEqualTo(tokens(new TsNRangeArgs().empty().aggregation(10, lists), 2));
    }

}
