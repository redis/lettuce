/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.search.arguments;

import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * GROUPBY post-processing operation. Groups results by one or more properties with reducer functions.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Aleksandar Todorov
 * @since 7.2
 * @see Reducer
 * @see ReduceFunction
 */
public class GroupBy<K, V> implements PostProcessingOperation<K, V> {

    private final List<K> properties;

    private final List<Reducer<K, V>> reducers;

    private GroupBy(List<K> properties) {
        this.properties = new ArrayList<>(properties);
        this.reducers = new ArrayList<>();
    }

    /**
     * Add a reducer to this GROUPBY operation.
     *
     * @param reducer the reducer to add
     * @return this GroupBy instance
     */
    public GroupBy<K, V> reduce(Reducer<K, V> reducer) {
        this.reducers.add(reducer);
        return this;
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

        for (Reducer<K, V> reducer : reducers) {
            reducer.build(args);
        }
    }

}
