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
 * FILTER post-processing operation. Filters results using predicate expressions on the current pipeline state.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Aleksandar Todorov
 * @since 7.2
 */
public class Filter<K, V> implements PostProcessingOperation<K, V> {

    private final V expression;

    /**
     * Creates a new FILTER operation.
     *
     * @param expression the filter expression (e.g., "@price > 100", "@category == 'electronics'")
     */
    public Filter(V expression) {
        this.expression = expression;
    }

    @Override
    public void build(CommandArgs<K, V> args) {
        args.add(CommandKeyword.FILTER);
        args.addValue(expression);
    }

    /**
     * Static factory method to create a Filter instance.
     *
     * @param expression the filter expression
     * @param <K> Key type
     * @param <V> Value type
     * @return new Filter instance
     */
    public static <K, V> Filter<K, V> of(V expression) {
        return new Filter<>(expression);
    }

}
