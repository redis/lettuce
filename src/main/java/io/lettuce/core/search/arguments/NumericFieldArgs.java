/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.search.arguments;

import io.lettuce.core.protocol.CommandArgs;

/**
 * Field arguments for NUMERIC fields in a RediSearch index.
 * <p>
 * Numeric fields are used to store non-textual, countable values. They can hold integer or floating-point values. Numeric
 * fields are sortable, meaning you can perform range-based queries and retrieve documents based on specific numeric conditions.
 * For example, you can search for documents with a price between a certain range or retrieve documents with a specific rating
 * value.
 *
 * @see <a href=
 *      "https://redis.io/docs/latest/develop/interact/search-and-query/basic-constructs/field-and-type-options/#numeric-fields">Numeric
 *      Fields</a>
 * @since 6.8
 * @author Tihomir Mateev
 */
public class NumericFieldArgs extends FieldArgs {

    /**
     * Create a new {@link NumericFieldArgs} using the builder pattern.
     * 
     * @return a new {@link Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String getFieldType() {
        return "NUMERIC";
    }

    @Override
    protected void buildTypeSpecificArgs(CommandArgs<?, ?> args) {
        // Numeric fields have no type-specific arguments beyond the common ones
    }

    /**
     * Builder for {@link NumericFieldArgs}.
     * 
     */
    public static class Builder extends FieldArgs.Builder<NumericFieldArgs, Builder> {

        public Builder() {
            super(new NumericFieldArgs());
        }

    }

}
