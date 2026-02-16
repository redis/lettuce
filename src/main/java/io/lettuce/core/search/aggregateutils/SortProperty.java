/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.search.aggregateutils;

import io.lettuce.core.annotations.Experimental;

/**
 * Represents a sort property with direction for SORTBY operations in FT.HYBRID and FT.AGGREGATE commands.
 *
 * @param <K> Key type.
 * @author Aleksandar Todorov
 * @since 7.5
 * @see SortBy
 * @see SortDirection
 */
@Experimental
public class SortProperty<K> {

    private final K property;

    private final SortDirection direction;

    /**
     * Creates a new sort property.
     *
     * @param property the property to sort by
     * @param direction the sort direction
     */
    public SortProperty(K property, SortDirection direction) {
        this.property = property;
        this.direction = direction;
    }

    /**
     * Static factory method to create a SortProperty instance.
     *
     * @param property the property to sort by
     * @param direction the sort direction
     * @param <K> Key type
     * @return new SortProperty instance
     */
    public static <K> SortProperty<K> of(K property, SortDirection direction) {
        return new SortProperty<>(property, direction);
    }

    /**
     * Get the property to sort by.
     *
     * @return the property
     */
    public K getProperty() {
        return property;
    }

    /**
     * Get the sort direction.
     *
     * @return the sort direction
     */
    public SortDirection getDirection() {
        return direction;
    }

}
