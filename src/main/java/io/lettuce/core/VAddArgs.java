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
 * Argument list builder for the Redis <a href="https://redis.io/docs/latest/commands/vadd/">VADD</a> command.
 * <p>
 * This class provides options for configuring how vectors are stored and indexed, including quantization type, exploration
 * factor, attributes, and graph connectivity parameters.
 * <p>
 * Example usage:
 * 
 * <pre>
 * {@code
 * VAddArgs args = VAddArgs.checkAndSet(true)
 *                     .quantizationType(QuantizationType.Q8)
 *                     .explorationFactor(200)
 *                     .attributes("{\"name\": \"Point A\", \"description\": \"First point added\"}"));
 * }
 * </pre>
 * <p>
 * {@link VAddArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @author Tihomir Mateev
 * @since 6.7
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class VAddArgs implements CompositeArgument {

    private Optional<Boolean> checkAndSet = Optional.empty();

    private Optional<QuantizationType> quantType = Optional.empty();

    private Optional<Integer> explorationFactor = Optional.empty();

    private Optional<String> attributes = Optional.empty();

    private Optional<Integer> maxNodes = Optional.empty();

    /**
     * Builder entry points for {@link VAddArgs}.
     * <p>
     * These static methods provide a convenient way to create new instances of {@link VAddArgs} with specific options set. Each
     * method creates a new instance and sets the corresponding option for the VADD command.
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        /**
         * Creates new {@link VAddArgs} and sets the {@literal CAS} flag.
         * <p>
         * The CAS option performs the operation partially using threads, in a check-and-set style. The neighbor candidates
         * collection, which is slow, is performed in the background, while the command is executed in the main thread.
         *
         * @param checkAndSet whether to perform a check and set operation.
         * @return new {@link VAddArgs} with {@literal CAS} set.
         * @see VAddArgs#checkAndSet(boolean)
         */
        public static VAddArgs checkAndSet(boolean checkAndSet) {
            return new VAddArgs().checkAndSet(checkAndSet);
        }

        /**
         * Creates new {@link VAddArgs} and setting the quantization type.
         * <p>
         * Quantization affects how vectors are stored and impacts memory usage, performance, and recall quality:
         * <ul>
         * <li>{@code Q8} (default) - Uses signed 8-bit quantization, balancing memory usage and recall quality</li>
         * <li>{@code NOQUANT} - Stores vectors without quantization, using more memory but preserving full precision</li>
         * <li>{@code BIN} - Uses binary quantization, which is faster and uses less memory, but impacts recall quality</li>
         * </ul>
         * <p>
         * Note that these options are mutually exclusive.
         *
         * @param quantType the quantization type for the vector.
         * @return new {@link VAddArgs} with the quantization type set.
         * @see VAddArgs#quantizationType(QuantizationType)
         */
        public static VAddArgs quantizationType(QuantizationType quantType) {
            return new VAddArgs().quantizationType(quantType);
        }

        /**
         * Creates new {@link VAddArgs} and setting {@literal EF} (exploration factor).
         * <p>
         * The EF option plays a role in the effort made to find good candidates when connecting the new node to the existing
         * Hierarchical Navigable Small World (HNSW) graph.
         * <p>
         * The default is 200. Using a larger value may help in achieving a better recall, but will increase the time needed to
         * add vectors to the set.
         *
         * @param explorationFactor the exploration factor for the vector search (default: 200).
         * @return new {@link VAddArgs} with {@literal EF} set.
         * @see VAddArgs#explorationFactor(int)
         */
        public static VAddArgs explorationFactor(int explorationFactor) {
            return new VAddArgs().explorationFactor(explorationFactor);
        }

        /**
         * Creates new {@link VAddArgs} and setting {@literal M} (maximum links).
         * <p>
         * The M option specifies the maximum number of connections that each node of the graph will have with other nodes.
         * <p>
         * The default is 16. More connections means more memory, but provides for more efficient graph exploration. Nodes at
         * layer zero (every node exists at least at layer zero) have M * 2 connections, while the other layers only have M
         * connections.
         * <p>
         * If you don't have a recall quality problem, the default is acceptable and uses a minimal amount of memory.
         *
         * @param maxNodes the maximum number of connections per node (default: 16).
         * @return new {@link VAddArgs} with {@literal M} set.
         * @see VAddArgs#maxNodes(int)
         */
        public static VAddArgs maxNodes(int maxNodes) {
            return new VAddArgs().maxNodes(maxNodes);
        }

        /**
         * Creates new {@link VAddArgs} with the specified attributes (SETATTR).
         * <p>
         * The SETATTR option associates attributes in the form of a JavaScript object to the newly created entry or updates the
         * attributes (if they already exist). It is the same as calling the VSETATTR command separately.
         * <p>
         * Attributes can be used for filtering during similarity searches with the VSIM command.
         *
         * @param attributes the attributes for the vector as JSON string.
         * @return new {@link VAddArgs} with the attributes set.
         * @see VAddArgs#attributes(String)
         */
        public static VAddArgs attributes(String attributes) {
            return new VAddArgs().attributes(attributes);
        }

    }

    /**
     * Set the CAS (Check-And-Set) flag for the vector addition.
     * <p>
     * The CAS option performs the operation partially using threads, in a check-and-set style. The neighbor candidates
     * collection, which is slow, is performed in the background, while the command is executed in the main thread.
     * <p>
     * This can improve performance when adding vectors to large sets.
     *
     * @param checkAndSet whether to enable the check-and-set operation mode.
     * @return {@code this}
     */
    public VAddArgs checkAndSet(boolean checkAndSet) {
        this.checkAndSet = Optional.of(checkAndSet);
        return this;
    }

    /**
     * Set the quantization type for the vector.
     * <p>
     * Quantization affects how vectors are stored and impacts memory usage, performance, and recall quality:
     * <ul>
     * <li>{@code Q8} (default) - Uses signed 8-bit quantization, balancing memory usage and recall quality</li>
     * <li>{@code NOQUANT} - Stores vectors without quantization, using more memory but preserving full precision</li>
     * <li>{@code BIN} - Uses binary quantization, which is faster and uses less memory, but impacts recall quality</li>
     * </ul>
     * <p>
     * Note that these options are mutually exclusive.
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
     * Set the exploration factor (EF) for the vector search.
     * <p>
     * The EF option plays a role in the effort made to find good candidates when connecting the new node to the existing
     * Hierarchical Navigable Small World (HNSW) graph.
     * <p>
     * The default is 200. Using a larger value may help in achieving a better recall, but will increase the time needed to add
     * vectors to the set.
     *
     * @param explorationFactor the exploration factor for the vector search (default: 200).
     * @return {@code this}
     */
    public VAddArgs explorationFactor(int explorationFactor) {
        LettuceAssert.isTrue(explorationFactor > 0, "Exploration factor must be greater than 0");
        this.explorationFactor = Optional.of(explorationFactor);
        return this;
    }

    /**
     * Set the maximum number of connections per node (M).
     * <p>
     * The M option specifies the maximum number of connections that each node of the graph will have with other nodes.
     * <p>
     * The default is 16. More connections means more memory, but provides for more efficient graph exploration. Nodes at layer
     * zero (every node exists at least at layer zero) have M * 2 connections, while the other layers only have M connections.
     * <p>
     * If you don't have a recall quality problem, the default is acceptable and uses a minimal amount of memory.
     *
     * @param maxNodes the maximum number of connections per node (default: 16).
     * @return {@code this}
     */
    public VAddArgs maxNodes(int maxNodes) {
        LettuceAssert.isTrue(maxNodes > 0, "Max nodes must be greater than 0");
        this.maxNodes = Optional.of(maxNodes);
        return this;
    }

    /**
     * Set the attributes for the vector (SETATTR).
     * <p>
     * The SETATTR option associates attributes in the form of a JavaScript object to the newly created entry or updates the
     * attributes (if they already exist). It is the same as calling the VSETATTR command separately.
     * <p>
     * Attributes can be used for filtering during similarity searches with the VSIM command.
     * <p>
     * Example: {@code attributes(Arrays.asList("{\"color\":\"red\",\"price\":25}"))}
     *
     * @param attributes the attributes for the vector as JSON strings.
     * @return {@code this}
     */
    public VAddArgs attributes(String attributes) {
        LettuceAssert.notNull(attributes, "Attributes must not be null");
        this.attributes = Optional.of(attributes);
        return this;
    }

    /**
     * Builds the command arguments based on the options set in this {@link VAddArgs} instance.
     * <p>
     * This method is called internally by the Redis client to construct the VADD command with all the specified options. It
     * adds each option that has been set to the command arguments in the correct format.
     *
     * @param <K> Key type.
     * @param <V> Value type.
     * @param args Command arguments to which the VADD options will be added.
     */
    public <K, V> void build(CommandArgs<K, V> args) {

        if (checkAndSet.isPresent() && checkAndSet.get()) {
            args.add(CommandKeyword.CAS);
        }

        quantType.ifPresent(quantizationType -> args.add(quantizationType.getKeyword()));

        explorationFactor.ifPresent(integer -> args.add(CommandKeyword.EF).add(integer));

        attributes.ifPresent(attr -> args.add(CommandKeyword.SETATTR).add(attr));

        maxNodes.ifPresent(integer -> args.add(CommandKeyword.M).add(integer));
    }

}
