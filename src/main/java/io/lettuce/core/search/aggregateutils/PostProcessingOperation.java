/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.search.aggregateutils;

import io.lettuce.core.annotations.Experimental;
import io.lettuce.core.protocol.CommandArgs;

/**
 * Interface for post-processing operations applied in user-specified order. This includes GROUPBY, SORTBY, APPLY, FILTER, and
 * LIMIT operations used in FT.HYBRID and FT.AGGREGATE commands.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Aleksandar Todorov
 * @since 7.5
 * @see GroupBy
 * @see Apply
 * @see SortBy
 * @see Filter
 * @see Limit
 */
@Experimental
public interface PostProcessingOperation<K, V> {

    /**
     * Build the operation arguments into the command args.
     *
     * @param args the command args to build into
     */
    void build(CommandArgs<K, V> args);

}
