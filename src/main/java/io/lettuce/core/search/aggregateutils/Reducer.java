/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.search.aggregateutils;

import java.util.List;

import io.lettuce.core.annotations.Experimental;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;

/**
 * Abstract reducer for GROUPBY operations in FT.HYBRID and FT.AGGREGATE commands. Instances are created via {@link Reducers}.
 * <p>
 * Reducers handle group entries in a GROUPBY operation, performing aggregate operations like counting, summing, averaging, or
 * finding min/max values. Each reducer can have an optional alias using the AS keyword.
 * </p>
 *
 * <h3>Example Usage:</h3>
 *
 * <pre>
 * {@code
 * // Count items in each group
 * Reducers.count().as("item_count")
 *
 * // Sum numeric values
 * Reducers.sum("@sales").as("total_sales")
 *
 * // Calculate average
 * Reducers.avg("@price").as("average_price")
 *
 * // Quantile with percentile parameter
 * Reducers.quantile("@price", 0.5).as("median_price")
 *
 * // First value with optional sort by
 * Reducers.firstValue("@name").by("@timestamp").as("first_name")
 *
 * // Random sample
 * Reducers.randomSample("@id", 10).as("sample_ids")
 * }
 * </pre>
 *
 * @param <K> Key type.
 * @author Aleksandar Todorov
 * @since 7.2
 * @see Reducers
 * @see ReduceFunction
 * @see GroupBy
 * @see <a href="https://redis.io/docs/latest/commands/ft.aggregate/">FT.AGGREGATE</a>
 * @see <a href="https://redis.io/docs/latest/commands/ft.hybrid/">FT.HYBRID</a>
 */
@Experimental
public abstract class Reducer<K> {

    private final String function;

    private K alias;

    /**
     * Creates a new reducer with the specified function.
     *
     * @param function the reduce function enum
     */
    protected Reducer(ReduceFunction function) {
        LettuceAssert.notNull(function, "Function must not be null");
        this.function = function.name();
    }

    /**
     * Creates a new reducer with the specified function name. Use this constructor for custom or future reduce functions not
     * yet defined in {@link ReduceFunction}.
     *
     * @param function the reduce function name (e.g., "COUNT", "SUM", "MY_CUSTOM_REDUCER")
     */
    protected Reducer(String function) {
        LettuceAssert.notNull(function, "Function must not be null");
        LettuceAssert.notEmpty(function, "Function must not be empty");
        this.function = function;
    }

    /**
     * Get the reduce function name.
     *
     * @return the reduce function name
     */
    public final String getFunction() {
        return function;
    }

    /**
     * Get the reducer-specific arguments.
     *
     * @return list of reducer-specific arguments
     */
    protected abstract List<Object> getOwnArgs();

    /**
     * Set an alias for the reducer result using AS keyword.
     *
     * @param alias the alias name
     * @return this reducer
     */
    @SuppressWarnings("unchecked")
    public <T extends Reducer<K>> T as(K alias) {
        LettuceAssert.notNull(alias, "Alias must not be null");
        this.alias = alias;
        return (T) this;
    }

    /**
     * Build the reducer into command args.
     * <p>
     * Format: REDUCE function nargs arg [arg ...] [AS name]
     * </p>
     *
     * @param args the command args to build into
     * @param <V> value type
     */
    public final <V> void build(CommandArgs<K, V> args) {
        args.add(CommandKeyword.REDUCE);
        args.add(function);

        List<Object> ownArgs = getOwnArgs();
        args.add(ownArgs.size());
        for (Object arg : ownArgs) {
            args.add(arg);
        }

        if (alias != null) {
            args.add(CommandKeyword.AS);
            args.addKey(alias);
        }
    }

}
