/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.search.arguments;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.search.arguments.AggregateArgs.GroupBy;
import io.lettuce.core.search.arguments.AggregateArgs.Reducer;
import io.lettuce.core.search.arguments.AggregateArgs.SortBy;
import io.lettuce.core.search.arguments.AggregateArgs.SortDirection;

/**
 * Unit tests for {@link AggregateArgs} focusing on {@code @} prefix normalisation in {@link GroupBy}, {@link SortBy}, and
 * {@link Reducer}.
 *
 * @author Viktoriya Kutsarova
 */
class AggregateArgsTest {

    // -------------------------------------------------------------------------
    // GroupBy
    // -------------------------------------------------------------------------

    @Test
    void groupByFieldWithoutAtPrefixShouldAddPrefix() {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);

        GroupBy.of("category").build(args);

        assertThat(args.toString()).contains("@category");
    }

    @Test
    void groupByFieldWithAtPrefixShouldNotDoublePrefix() {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);

        GroupBy.of("@category").build(args);

        String output = args.toString();
        assertThat(output).contains("@category");
        assertThat(output).doesNotContain("@@category");
    }

    @Test
    void groupByMultipleFieldsMixedPrefixShouldNormalise() {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);

        GroupBy.of("brand", "@price").build(args);

        String output = args.toString();
        assertThat(output).contains("@brand");
        assertThat(output).contains("@price");
        assertThat(output).doesNotContain("@@price");
    }

    // -------------------------------------------------------------------------
    // SortBy
    // -------------------------------------------------------------------------

    @Test
    void sortByFieldWithoutAtPrefixShouldAddPrefix() {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);

        SortBy.of("price", SortDirection.ASC).build(args);

        assertThat(args.toString()).contains("@price");
    }

    @Test
    void sortByFieldWithAtPrefixShouldNotDoublePrefix() {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);

        SortBy.of("@price", SortDirection.DESC).build(args);

        String output = args.toString();
        assertThat(output).contains("@price");
        assertThat(output).doesNotContain("@@price");
    }

    // -------------------------------------------------------------------------
    // Reducer
    // -------------------------------------------------------------------------

    @Test
    void reducerAvgWithAtPrefixShouldWork() {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);

        Reducer.avg("@price").as("avg_price").build(args);

        assertThat(args.toString()).contains("@price");
    }

    @Test
    void reducerAvgWithoutAtPrefixShouldAddPrefix() {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);

        Reducer.avg("price").as("avg_price").build(args);

        assertThat(args.toString()).contains("@price");
    }

    @Test
    void reducerSumWithoutAtPrefixShouldAddPrefix() {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);

        Reducer.sum("stock").as("total_stock").build(args);

        assertThat(args.toString()).contains("@stock");
    }

    @Test
    void reducerMinWithoutAtPrefixShouldAddPrefix() {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);

        Reducer.min("price").build(args);

        assertThat(args.toString()).contains("@price");
    }

    @Test
    void reducerMaxWithoutAtPrefixShouldAddPrefix() {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);

        Reducer.max("price").build(args);

        assertThat(args.toString()).contains("@price");
    }

    @Test
    void reducerCountDistinctWithAtPrefixShouldNotDoublePrefix() {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);

        Reducer.countDistinct("@role").as("role_diversity").build(args);

        String output = args.toString();
        assertThat(output).contains("@role");
        assertThat(output).doesNotContain("@@role");
    }

    @Test
    void reducerCountDistinctWithoutAtPrefixShouldAddPrefix() {
        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8);

        Reducer.countDistinct("role").as("role_diversity").build(args);

        assertThat(args.toString()).contains("@role");
    }

}
