/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.search.aggregateutils;

import io.lettuce.core.annotations.Experimental;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;

/**
 * LIMIT post-processing operation. Limits the number of results for pagination.
 *
 * <h3>Example Usage:</h3>
 *
 * <pre>
 * 
 * {
 *     &#64;code
 *     // Get first 10 results
 *     Limit<String, String> limit = Limit.of(0, 10);
 *
 *     // Get results 50-100
 *     Limit<String, String> paginated = Limit.of(50, 50);
 * }
 * </pre>
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Aleksandar Todorov
 * @since 7.5
 * @see PostProcessingOperation
 */
@Experimental
public class Limit<K, V> implements PostProcessingOperation<K, V> {

    private final long offset;

    private final long num;

    /**
     * Creates a new LIMIT operation.
     *
     * @param offset the zero-based starting index
     * @param num the maximum number of results to return
     */
    public Limit(long offset, long num) {
        this.offset = offset;
        this.num = num;
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

    /**
     * Get the offset.
     *
     * @return the offset
     */
    public long getOffset() {
        return offset;
    }

    /**
     * Get the number of results.
     *
     * @return the number of results
     */
    public long getNum() {
        return num;
    }

    @Override
    public void build(CommandArgs<K, V> args) {
        args.add(CommandKeyword.LIMIT);
        args.add(offset);
        args.add(num);
    }

}
