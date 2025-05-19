/*
 * Copyright 2018-Present, Redis Ltd. and Contributors
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
package io.lettuce.core;

import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;
import io.lettuce.core.vector.QuantizationType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Argument list builder for the Redis <a href="https://redis.io/docs/latest/commands/vadd/">VECTOR ADD</a> command.
 * <p>
 * {@link VAddArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @author Tihomir Mateev
 * @since 6.7
 */
public class VAddArgs implements CompositeArgument {

    private Optional<Boolean> checkAndSet = Optional.empty();

    private Optional<QuantizationType> quantType = Optional.empty();

    private Optional<Integer> explorationFactor = Optional.empty();

    private List<String> attributes = new ArrayList<>();

    private Optional<Integer> maxNodes = Optional.empty();

    /**
     * Builder entry points for {@link VAddArgs}.
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        /**
         * Creates new {@link VAddArgs} and sets the {@literal CAS} flag.
         *
         * @param checkAndSet whether to perform a check and set operation.
         * @return new {@link VAddArgs} with {@literal MAXLEN} set.
         * @see VAddArgs#checkAndSet(boolean)
         */
        public static VAddArgs checkAndSet(boolean checkAndSet) {
            return new VAddArgs().checkAndSet(checkAndSet);
        }

        /**
         * Creates new {@link VAddArgs} and setting {@literal QUANTTYPE}.
         *
         * @param quantType the quantization type for the vector.
         * @return new {@link VAddArgs} with {@literal QUANTTYPE} set.
         * @see VAddArgs#quantizationType(QuantizationType)
         */
        public static VAddArgs quantizationType(QuantizationType quantType) {
            return new VAddArgs().quantizationType(quantType);
        }

        /**
         * Creates new {@link VAddArgs} and setting {@literal EF}.
         *
         * @param explorationFactor the exploration factor for the vector search.
         * @return new {@link VAddArgs} with {@literal EF} set.
         * @see VAddArgs#explorationFactor(int)
         */
        public static VAddArgs explorationFactor(int explorationFactor) {
            return new VAddArgs().explorationFactor(explorationFactor);
        }

        /**
         * Creates new {@link VAddArgs} and setting {@literal MAXNODES}.
         *
         * @param maxNodes the maximum number of nodes to visit during the vector search.
         * @return new {@link VAddArgs} with {@literal MAXNODES} set.
         * @see VAddArgs#maxNodes(int)
         */
        public static VAddArgs maxNodes(int maxNodes) {
            return new VAddArgs().maxNodes(maxNodes);
        }

        /**
         * Creates new {@link VAddArgs} with the specified attributes.
         *
         * @param attributes the attributes for the vector.
         * @return new {@link VAddArgs} with the attributes set.
         * @see VAddArgs#attributes(List)
         */
        @SafeVarargs
        public static VAddArgs attributes(String... attributes) {
            return new VAddArgs().attributes(Arrays.asList(attributes));
        }
    }

    /**
     * Set whether to limit the maximum number of vectors.
     *
     * @param checkAndSet whether to limit the maximum number of vectors.
     * @return {@code this}
     */
    public VAddArgs checkAndSet(boolean checkAndSet) {
        this.checkAndSet = Optional.of(checkAndSet);
        return this;
    }

    /**
     * Set the quantization type for the vector.
     *
     * @param quantType the quantization type for the vector.
     * @return {@code this}
     */
    public VAddArgs quantizationType(QuantizationType quantType) {
        LettuceAssert.notNull(quantType, "QuantType must not be null");
        this.quantType = Optional.of(quantType);
        return this;
    }

    /**
     * Set the exploration factor for the vector search.
     *
     * @param explorationFactor the exploration factor for the vector search.
     * @return {@code this}
     */
    public VAddArgs explorationFactor(int explorationFactor) {
        LettuceAssert.isTrue(explorationFactor > 0, "Exploration factor must be greater than 0");
        this.explorationFactor = Optional.of(explorationFactor);
        return this;
    }

    /**
     * Set the maximum number of nodes to visit during the vector search.
     *
     * @param maxNodes the maximum number of nodes to visit during the vector search.
     * @return {@code this}
     */
    public VAddArgs maxNodes(int maxNodes) {
        LettuceAssert.isTrue(maxNodes > 0, "Max nodes must be greater than 0");
        this.maxNodes = Optional.of(maxNodes);
        return this;
    }

    /**
     * Set the attributes for the vector.
     *
     * @param attributes the attributes for the vector.
     * @return {@code this}
     */
    public VAddArgs attributes(List<String> attributes) {
        LettuceAssert.notNull(attributes, "Attributes must not be null");
        this.attributes = new ArrayList<>(attributes);
        return this;
    }

    public <K, V> void build(CommandArgs<K, V> args) {

        if (checkAndSet.isPresent() && checkAndSet.get()) {
            args.add(CommandKeyword.CAS);
        }

        if (quantType.isPresent()) {
            args.add(quantType.get().getKeyword());
        }

        if (explorationFactor.isPresent()) {
            args.add(CommandKeyword.EF).add(explorationFactor.get());
        }

        // Add attributes if present
        if (!attributes.isEmpty()) {
            args.add(CommandKeyword.SETATTR);
            for (String attribute : attributes) {
                args.add(attribute);
            }
        }

        if (maxNodes.isPresent()) {
            args.add(CommandKeyword.M).add(maxNodes.get());
        }
    }

}
