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
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;

/**
 * SORTBY post-processing operation. Sorts results by one or more properties with optional MAX optimization for top-N queries.
 * <p>
 * Sorts the pipeline results up until the point of SORTBY, using a list of properties. By default, sorting is ascending, but
 * ASC or DESC can be specified for each property.
 * </p>
 *
 * <h3>Example Usage:</h3>
 *
 * <pre>
 * 
 * {
 *     &#64;code
 *     // Simple sort by single field
 *     SortBy<String> sortBy = SortBy.of("price", SortDirection.DESC);
 *
 *     // Sort with MAX optimization for top-N queries
 *     SortBy<String> topN = SortBy.of("score", SortDirection.DESC).max(100);
 *
 *     // Multiple sort criteria
 *     SortBy<String> multiSort = SortBy.of(SortProperty.of("category", SortDirection.ASC),
 *             SortProperty.of("price", SortDirection.DESC));
 * }
 * </pre>
 *
 * @param <K> Key type.
 * @author Aleksandar Todorov
 * @since 7.2
 * @see SortProperty
 * @see SortDirection
 * @see PostProcessingOperation
 */
@Experimental
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

    /**
     * Set the MAX parameter for top-N optimization.
     *
     * @param max the maximum number of results to sort
     * @return this SortBy instance
     */
    public SortBy<K> max(long max) {
        this.max = Optional.of(max);
        return this;
    }

    /**
     * Enable WITHCOUNT to return accurate counts.
     *
     * @return this SortBy instance
     */
    public SortBy<K> withCount() {
        this.withCount = true;
        return this;
    }

    @Override
    public void build(CommandArgs<K, Object> args) {
        args.add(CommandKeyword.SORTBY);
        // Count includes property + direction pairs
        args.add(properties.size() * 2L);
        for (SortProperty<K> property : properties) {
            // Add @ prefix if not already present
            String propertyStr = property.getProperty().toString();
            if (!propertyStr.startsWith("@")) {
                args.add("@" + propertyStr);
            } else {
                args.add(propertyStr);
            }
            args.add(property.getDirection().name());
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
