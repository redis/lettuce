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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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

    private Optional<Long> max = Optional.empty();

    private boolean withCount = false;

    /**
     * Creates a new SORTBY operation.
     *
     * @param properties the properties to sort by
     */
    public SortBy(List<SortProperty<K>> properties) {
        this.properties = new ArrayList<>(properties);
    }

    /**
     * Set the MAX option to optimize sorting by only processing the top N results.
     *
     * @param max the maximum number of results to sort
     * @return this SortBy instance
     */
    public SortBy<K> max(long max) {
        this.max = Optional.of(max);
        return this;
    }

    /**
     * Set the WITHCOUNT option to return accurate counts.
     *
     * @return this SortBy instance
     */
    public SortBy<K> withCount() {
        this.withCount = true;
        return this;
    }

    /**
     * Static factory method to create a SortBy instance with a single property.
     *
     * @param property the property to sort by
     * @param direction the sort direction
     * @param <K> Key type
     * @return new SortBy instance
     */
    public static <K> SortBy<K> of(K property, SortDirection direction) {
        return new SortBy<>(Collections.singletonList(new SortProperty<>(property, direction)));
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

        max.ifPresent(m -> {
            args.add(CommandKeyword.MAX);
            args.add(m);
        });

        if (withCount) {
            args.add(CommandKeyword.WITHCOUNT);
        }
    }

}
