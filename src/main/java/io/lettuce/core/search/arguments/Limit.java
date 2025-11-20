/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.search.arguments;

import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;

/**
 * LIMIT post-processing operation. Limits the number of results for pagination.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Aleksandar Todorov
 * @since 7.2
 */
public class Limit<K, V> implements PostProcessingOperation<K, V> {

    final long offset;

    final long num;

    private Limit(long offset, long num) {
        this.offset = offset;
        this.num = num;
    }

    @Override
    public void build(CommandArgs<K, V> args) {
        args.add(CommandKeyword.LIMIT);
        args.add(offset);
        args.add(num);
    }

    /**
     * Static factory method to create a Limit instance.
     *
     * @param offset the zero-based starting index
     * @param num the maximum number of results to return
     * @param <K> Key type
     * @param <V> Value type
     * @return new Limit instance
     */
    public static <K, V> Limit<K, V> of(long offset, long num) {
        return new Limit<>(offset, num);
    }

}
