/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.search.arguments;

import io.lettuce.core.annotations.Experimental;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;

/**
 * APPLY post-processing operation. Applies a 1-to-1 transformation expression on properties and stores the result as a new
 * property.
 * <p>
 * APPLY can perform arithmetic operations on numeric properties or apply functions depending on property types.
 * </p>
 *
 * <h3>Example Usage:</h3>
 *
 * <pre>
 * 
 * {
 *     &#64;code
 *     // Calculate total value from price and quantity
 *     Apply<String, String> totalValue = Apply.of("@price * @quantity", "total_value");
 *
 *     // Mathematical operations
 *     Apply<String, String> discount = Apply.of("@price * 0.9", "discounted_price");
 * }
 * </pre>
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Aleksandar Todorov
 * @since 7.2
 * @see PostProcessingOperation
 */
@Experimental
public class Apply<K, V> implements PostProcessingOperation<K, V> {

    private final V expression;

    private final K name;

    /**
     * Creates a new APPLY operation.
     *
     * @param expression the expression to apply
     * @param name the result field name
     */
    public Apply(V expression, K name) {
        this.expression = expression;
        this.name = name;
    }

    /**
     * Static factory method to create an Apply instance.
     *
     * @param expression the expression to apply
     * @param name the name of the result field
     * @param <K> Key type
     * @param <V> Value type
     * @return new Apply instance
     */
    public static <K, V> Apply<K, V> of(V expression, K name) {
        return new Apply<>(expression, name);
    }

    @Override
    public void build(CommandArgs<K, V> args) {
        args.add(CommandKeyword.APPLY);
        args.addValue(expression);
        args.add(CommandKeyword.AS);
        args.add(name.toString());
    }

}
