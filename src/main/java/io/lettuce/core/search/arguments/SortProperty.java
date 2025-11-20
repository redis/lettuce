/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.search.arguments;

/**
 * Represents a sort property with direction for SORTBY operations.
 *
 * @param <K> Key type.
 * @since 6.8
 * @author Tihomir Mateev
 */
public class SortProperty<K> {

    final K property;

    final SortDirection direction;

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
