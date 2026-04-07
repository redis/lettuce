/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.search.aggregateutils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.lettuce.core.annotations.Experimental;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;

/**
 * SORTBY post-processing operation. Sorts results by one or more properties.
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
 *     SortBy sortBy = SortBy.of("price", SortDirection.DESC);
 *
 *     // Multiple sort criteria
 *     SortBy multiSort = SortBy.of(SortProperty.of("category", SortDirection.ASC),
 *             SortProperty.of("price", SortDirection.DESC));
 * }
 * </pre>
 *
 * @author Aleksandar Todorov
 * @since 7.5
 * @see SortProperty
 * @see SortDirection
 * @see PostProcessingOperation
 */
@Experimental
public class SortBy implements PostProcessingOperation {

    private final List<SortProperty> properties;

    /**
     * Creates a new SORTBY operation.
     *
     * @param properties the properties to sort by
     */
    public SortBy(List<SortProperty> properties) {
        this.properties = new ArrayList<>(properties);
    }

    /**
     * Static factory method to create a SortBy instance with a single property.
     *
     * @param property the property to sort by
     * @param direction the sort direction
     * @return new SortBy instance
     */
    public static SortBy of(String property, SortDirection direction) {
        return new SortBy(Collections.singletonList(new SortProperty(property, direction)));
    }

    /**
     * Static factory method to create a SortBy instance with multiple properties.
     *
     * @param properties the properties to sort by
     * @return new SortBy instance
     */
    @SafeVarargs
    public static SortBy of(SortProperty... properties) {
        return new SortBy(Arrays.asList(properties));
    }

    @Override
    public void build(CommandArgs<?, ?> args) {
        args.add(CommandKeyword.SORTBY);
        // Count includes property + direction pairs
        args.add(properties.size() * 2L);
        for (SortProperty property : properties) {
            // Add @ prefix if not already present
            String propertyName = property.getProperty();
            if (!propertyName.startsWith("@")) {
                args.add("@" + propertyName);
            } else {
                args.add(propertyName);
            }
            args.add(property.getDirection().name());
        }
    }

}
