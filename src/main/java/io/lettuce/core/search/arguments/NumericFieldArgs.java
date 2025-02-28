/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 * @param <K> Key type
 * @see <a href=
 *      "https://redis.io/docs/latest/develop/interact/search-and-query/basic-constructs/field-and-type-options/#numeric-fields">Numeric
 *      Fields</a>
 * @since 6.8
 * @author Tihomir Mateev
 */
public class NumericFieldArgs<K> extends FieldArgs<K> {

    /**
     * Create a new {@link NumericFieldArgs} using the builder pattern.
     * 
     * @param <K> Key type
     * @return a new {@link Builder}
     */
    public static <K> Builder<K> builder() {
        return new Builder<>();
    }

    @Override
    public String getFieldType() {
        return "NUMERIC";
    }

    @Override
    protected void buildTypeSpecificArgs(CommandArgs<K, ?> args) {
        // Numeric fields have no type-specific arguments beyond the common ones
    }

    /**
     * Builder for {@link NumericFieldArgs}.
     * 
     * @param <K> Key type
     */
    public static class Builder<K> extends FieldArgs.Builder<K, NumericFieldArgs<K>, Builder<K>> {

        public Builder() {
            super(new NumericFieldArgs<>());
        }

    }

}
