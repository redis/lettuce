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
 * SORTBY post-processing operation. Sorts results by one or more properties with optional MAX optimization for top-N queries.
 *
 * @param <K> Key type.
 * @author Aleksandar Todorov
 * @since 7.2
 * @see SortProperty
 * @see SortDirection
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class SortBy<K> implements PostProcessingOperation<K, Object> {

    private final List<SortProperty<K>> properties;

    /**
     * Creates a new SORTBY operation.
     *
     * @param properties the properties to sort by
     */
    public SortBy(List<SortProperty<K>> properties) {
        this.properties = new ArrayList<>(properties);
    }

    /**
     * Static factory method to create a SortBy instance with multiple properties.
     *
     * @param properties the properties to sort by
     * @param <K> Key type
     * @return new SortBy instance
     */
    @SafeVarargs
    public static <K> SortBy<K> of(SortProperty<K>... properties) {
        return new SortBy<>(Arrays.asList(properties));
    }

    @Override
    public void build(CommandArgs<K, Object> args) {
        args.add(CommandKeyword.SORTBY);
        // Count includes property + direction pairs
        args.add(properties.size() * 2L);
        for (SortProperty<K> property : properties) {
            // Add @ prefix if not already present
            String propertyStr = property.property.toString();
            if (!propertyStr.startsWith("@")) {
                args.add("@" + propertyStr);
            } else {
                args.add(propertyStr);
            }
            args.add(property.direction.name());
        }
    }

}
