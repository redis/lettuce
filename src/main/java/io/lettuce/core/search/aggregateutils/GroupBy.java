/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.search.aggregateutils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.lettuce.core.annotations.Experimental;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;

/**
 * GROUPBY post-processing operation. Groups results by one or more properties with reducer functions.
 * <p>
 * Groups the results in the pipeline based on one or more properties. Each group should have at least one reducer function that
 * handles the group entries, either counting them or performing multiple aggregate operations.
 * </p>
 *
 * <h3>Example Usage:</h3>
 *
 * <pre>
 * 
 * {
 *     &#64;code
 *     // Group by category and count items
 *     GroupBy<String, String> groupBy = GroupBy.of("category").reduce(Reducers.count().as("item_count"));
 *
 *     // Group by multiple fields with multiple reducers
 *     GroupBy<String, String> complexGroup = GroupBy.of("category", "brand").reduce(Reducers.count().as("count"))
 *             .reduce(Reducers.avg("@price").as("avg_price"));
 * }
 * </pre>
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Aleksandar Todorov
 * @since 7.5
 * @see Reducer
 * @see Reducers
 * @see ReduceFunction
 * @see PostProcessingOperation
 */
@Experimental
public class GroupBy<K, V> implements PostProcessingOperation<K, V> {

    private final List<K> properties;

    private final List<Reducer<K>> reducers;

    /**
     * Creates a new GROUPBY operation.
     *
     * @param properties the properties to group by
     */
    public GroupBy(List<K> properties) {
        this.properties = new ArrayList<>(properties);
        this.reducers = new ArrayList<>();
    }

    /**
     * Static factory method to create a GroupBy instance.
     *
     * @param properties the properties to group by
     * @param <K> Key type
     * @param <V> Value type
     * @return new GroupBy instance
     */
    @SafeVarargs
    public static <K, V> GroupBy<K, V> of(K... properties) {
        return new GroupBy<>(Arrays.asList(properties));
    }

    /**
     * Add a reducer to this GROUPBY operation.
     *
     * @param reducer the reducer to add
     * @return this GroupBy instance
     */
    public GroupBy<K, V> reduce(Reducer<K> reducer) {
        this.reducers.add(reducer);
        return this;
    }

    @Override
    public void build(CommandArgs<K, V> args) {
        args.add(CommandKeyword.GROUPBY);
        args.add(properties.size());
        for (K property : properties) {
            // Add @ prefix if not already present
            String propertyStr = property.toString();
            if (!propertyStr.startsWith("@")) {
                args.add("@" + propertyStr);
            } else {
                args.add(propertyStr);
            }
        }

        for (Reducer<K> reducer : reducers) {
            reducer.build(args);
        }
    }

}
