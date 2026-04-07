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
 *     Apply totalValue = Apply.of("@price * @quantity", "total_value");
 *
 *     // Mathematical operations
 *     Apply discount = Apply.of("@price * 0.9", "discounted_price");
 * }
 * </pre>
 *
 * @author Aleksandar Todorov
 * @since 7.5
 * @see PostProcessingOperation
 */
@Experimental
public class Apply implements PostProcessingOperation {

    private final String expression;

    private final String name;

    /**
     * Creates a new APPLY operation.
     *
     * @param expression the expression to apply
     * @param name the result field name
     */
    public Apply(String expression, String name) {
        this.expression = expression;
        this.name = name;
    }

    /**
     * Static factory method to create an Apply instance.
     *
     * @param expression the expression to apply
     * @param name the name of the result field
     * @return new Apply instance
     */
    public static Apply of(String expression, String name) {
        return new Apply(expression, name);
    }

    @Override
    public void build(CommandArgs<?, ?> args) {
        args.add(CommandKeyword.APPLY);
        args.add(expression);
        args.add(CommandKeyword.AS);
        args.add(name);
    }

}
