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
 * FILTER post-processing operation. Filters results using predicate expressions on the current pipeline state.
 * <p>
 * Filters are applied after the query and relate to the current state of the pipeline. This allows filtering on computed fields
 * created by APPLY operations or reducer results.
 * </p>
 *
 * <h3>Example Usage:</h3>
 *
 * <pre>
 * 
 * {
 *     &#64;code
 *     // Filter by numeric comparison
 *     Filter priceFilter = Filter.of("@price > 100");
 *
 *     // Filter by computed field
 *     Filter totalFilter = Filter.of("@total_value > 1000");
 * }
 * </pre>
 *
 * @author Aleksandar Todorov
 * @since 7.5
 * @see PostProcessingOperation
 */
@Experimental
public class Filter implements PostProcessingOperation {

    private final String expression;

    /**
     * Creates a new FILTER operation.
     *
     * @param expression the filter expression (e.g., "@price > 100", "@category == 'electronics'")
     */
    public Filter(String expression) {
        this.expression = expression;
    }

    /**
     * Static factory method to create a Filter instance.
     *
     * @param expression the filter expression
     * @return new Filter instance
     */
    public static Filter of(String expression) {
        return new Filter(expression);
    }

    @Override
    public void build(CommandArgs<?, ?> args) {
        args.add(CommandKeyword.FILTER);
        args.add(expression);
    }

}
