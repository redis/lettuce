/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.search.arguments;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import io.lettuce.core.annotations.Experimental;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;

/**
 * REDUCE function for GROUPBY operations. Performs aggregate operations on grouped results with optional aliasing.
 * <p>
 * Reducers handle group entries in a GROUPBY operation, performing aggregate operations like counting, summing, averaging, or
 * finding min/max values. Each reducer can have an optional alias using the AS keyword.
 * </p>
 *
 * <h3>Example Usage:</h3>
 *
 * <pre>
 * 
 * {
 *     &#64;code
 *     // Count items in each group
 *     Reducer<String, String> count = Reducer.count().as("item_count");
 *
 *     // Sum numeric values
 *     Reducer<String, String> totalSales = Reducer.sum("@sales").as("total_sales");
 *
 *     // Calculate average
 *     Reducer<String, String> avgPrice = Reducer.avg("@price").as("average_price");
 * }
 * </pre>
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Aleksandar Todorov
 * @since 7.2
 * @see ReduceFunction
 * @see GroupBy
 */
@Experimental
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class Reducer<K, V> {

    private final ReduceFunction function;

    private final List<V> args;

    private Optional<K> alias = Optional.empty();

    /**
     * Creates a new reducer.
     *
     * @param function the reducer function
     * @param args the arguments to the reducer function
     */
    private Reducer(ReduceFunction function, List<V> args) {
        this.function = function;
        this.args = new ArrayList<>(args);
    }

    /**
     * Static factory method to create a Reducer with a function and optional arguments.
     *
     * @param function the reducer function
     * @param args the arguments to the reducer function (optional, can be empty for COUNT)
     * @param <K> Key type
     * @param <V> Value type
     * @return new Reducer instance
     */
    @SafeVarargs
    public static <K, V> Reducer<K, V> of(ReduceFunction function, V... args) {
        LettuceAssert.notNull(function, "ReduceFunction must not be null");
        return new Reducer<>(function, args.length == 0 ? Collections.emptyList() : Arrays.asList(args));
    }

    /**
     * Static factory method to create a COUNT reducer.
     *
     * @param <K> Key type
     * @param <V> Value type
     * @return new COUNT Reducer instance
     */
    public static <K, V> Reducer<K, V> count() {
        return new Reducer<>(ReduceFunction.COUNT, Collections.emptyList());
    }

    /**
     * Static factory method to create a SUM reducer.
     *
     * @param field the field to sum
     * @param <K> Key type
     * @param <V> Value type
     * @return new SUM Reducer instance
     */
    public static <K, V> Reducer<K, V> sum(V field) {
        return new Reducer<>(ReduceFunction.SUM, Collections.singletonList(field));
    }

    /**
     * Static factory method to create an AVG reducer.
     *
     * @param field the field to average
     * @param <K> Key type
     * @param <V> Value type
     * @return new AVG Reducer instance
     */
    public static <K, V> Reducer<K, V> avg(V field) {
        return new Reducer<>(ReduceFunction.AVG, Collections.singletonList(field));
    }

    /**
     * Static factory method to create a MIN reducer.
     *
     * @param field the field to find minimum
     * @param <K> Key type
     * @param <V> Value type
     * @return new MIN Reducer instance
     */
    public static <K, V> Reducer<K, V> min(V field) {
        return new Reducer<>(ReduceFunction.MIN, Collections.singletonList(field));
    }

    /**
     * Static factory method to create a MAX reducer.
     *
     * @param field the field to find maximum
     * @param <K> Key type
     * @param <V> Value type
     * @return new MAX Reducer instance
     */
    public static <K, V> Reducer<K, V> max(V field) {
        return new Reducer<>(ReduceFunction.MAX, Collections.singletonList(field));
    }

    /**
     * Static factory method to create a COUNT_DISTINCT reducer.
     *
     * @param field the field to count distinct values
     * @param <K> Key type
     * @param <V> Value type
     * @return new COUNT_DISTINCT Reducer instance
     */
    public static <K, V> Reducer<K, V> countDistinct(V field) {
        return new Reducer<>(ReduceFunction.COUNT_DISTINCT, Collections.singletonList(field));
    }

    /**
     * Set an alias for the reducer result.
     *
     * @param alias the alias name
     * @return this reducer
     */
    public Reducer<K, V> as(K alias) {
        LettuceAssert.notNull(alias, "Alias must not be null");
        this.alias = Optional.of(alias);
        return this;
    }

    /**
     * Build the reducer into command args.
     *
     * @param args the command args to build into
     */
    public void build(CommandArgs<K, V> args) {
        args.add(CommandKeyword.REDUCE);
        args.add(function);
        args.add(this.args.size());
        for (V arg : this.args) {
            args.addValue(arg);
        }

        alias.ifPresent(a -> {
            args.add(CommandKeyword.AS);
            args.add(a.toString());
        });
    }

}
