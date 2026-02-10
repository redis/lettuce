/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.search.arguments;

import io.lettuce.core.annotations.Experimental;

/**
 * Sort direction enumeration for SORTBY operations in FT.HYBRID and FT.AGGREGATE commands.
 *
 * @author Aleksandar Todorov
 * @since 7.2
 * @see SortBy
 * @see SortProperty
 */
@Experimental
public enum SortDirection {

    /**
     * Ascending sort order.
     */
    ASC,

    /**
     * Descending sort order.
     */
    DESC

}
