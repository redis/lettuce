/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.search.arguments;

import io.lettuce.core.protocol.CommandArgs;

/**
 * Interface for post-processing operations applied in user-specified order.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Aleksandar Todorov
 * @since 7.2
 * @see GroupBy
 * @see SortBy
 * @see Apply
 * @see Filter
 * @see Limit
 */
public interface PostProcessingOperation<K, V> {

    /**
     * Build the operation arguments into the command args.
     *
     * @param args the command args to build into
     */
    void build(CommandArgs<K, V> args);

}
