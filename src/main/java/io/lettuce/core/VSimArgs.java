/*
 * Copyright 2018-2025, Redis Ltd. and Contributors
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

import java.util.Optional;

/**
 * Argument list builder for the Redis <a href="https://redis.io/commands/vector-sim">VECTOR SIM</a> command. Static import the methods from
 * {@link Builder} and call the methods: {@code count(…)} .
 * <p>
 * {@link VSimArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @author Tihomir Mateev
 * @since 6.3
 */
public class VSimArgs implements CompositeArgument {

    private Optional<Integer> count = Optional.empty();

    private Optional<Integer> explorationFactor = Optional.empty();

    private Optional<String> filter = Optional.empty();

    private Optional<Integer> filterEfficiency = Optional.empty();

    private Optional<Boolean> truth = Optional.empty();

    private Optional<Boolean> noThread = Optional.empty();

    /**
     * Builder entry points for {@link VSimArgs}.
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        /**
         * Creates new {@link VSimArgs} and setting {@literal COUNT}.
         *
         * @param count the number of results to return.
         * @return new {@link VSimArgs} with {@literal COUNT} set.
         * @see VSimArgs#count(int)
         */
        public static VSimArgs count(int count) {
            return new VSimArgs().count(count);
        }

        /**
         * Creates new {@link VSimArgs} and setting {@literal EF}.
         *
         * @param explorationFactor the exploration factor for the vector search.
         * @return new {@link VSimArgs} with {@literal EF} set.
         * @see VSimArgs#explorationFactor(int)
         */
        public static VSimArgs explorationFactor(int explorationFactor) {
            return new VSimArgs().explorationFactor(explorationFactor);
        }

        /**
         * Creates new {@link VSimArgs} and setting {@literal FILTER}.
         *
         * @param filter the filter expression to apply.
         * @return new {@link VSimArgs} with {@literal FILTER} set.
         * @see VSimArgs#filter(String)
         */
        public static VSimArgs filter(String filter) {
            return new VSimArgs().filter(filter);
        }

        /**
         * Creates new {@link VSimArgs} and setting {@literal FILTEREFFICIENCY}.
         *
         * @param filterEfficiency the filter efficiency to use.
         * @return new {@link VSimArgs} with {@literal FILTEREFFICIENCY} set.
         * @see VSimArgs#filterEfficiency(int)
         */
        public static VSimArgs filterEfficiency(int filterEfficiency) {
            return new VSimArgs().filterEfficiency(filterEfficiency);
        }

        /**
         * Creates new {@link VSimArgs} and setting {@literal TRUTH}.
         *
         * @return new {@link VSimArgs} with {@literal TRUTH} set.
         * @see VSimArgs#truth()
         */
        public static VSimArgs truth() {
            return new VSimArgs().truth();
        }

        /**
         * Creates new {@link VSimArgs} and setting {@literal NOTHREAD}.
         *
         * @return new {@link VSimArgs} with {@literal NOTHREAD} set.
         * @see VSimArgs#noThread()
         */
        public static VSimArgs noThread() {
            return new VSimArgs().noThread();
        }
    }

    /**
     * Set the number of results to return.
     *
     * @param count the number of results to return.
     * @return {@code this}
     */
    public VSimArgs count(int count) {
        LettuceAssert.isTrue(count > 0, "Count must be greater than 0");
        this.count = Optional.of(count);
        return this;
    }

    /**
     * Set the exploration factor for the vector search.
     *
     * @param explorationFactor the exploration factor for the vector search.
     * @return {@code this}
     */
    public VSimArgs explorationFactor(int explorationFactor) {
        LettuceAssert.isTrue(explorationFactor > 0, "Exploration factor must be greater than 0");
        this.explorationFactor = Optional.of(explorationFactor);
        return this;
    }

    /**
     * Set the filter expression to apply.
     *
     * @param filter the filter expression to apply.
     * @return {@code this}
     */
    public VSimArgs filter(String filter) {
        LettuceAssert.notNull(filter, "Filter must not be null");
        LettuceAssert.notEmpty(filter, "Filter must not be empty");
        this.filter = Optional.of(filter);
        return this;
    }

    /**
     * Set the filter efficiency to use.
     *
     * @param filterEfficiency the filter efficiency to use.
     * @return {@code this}
     */
    public VSimArgs filterEfficiency(int filterEfficiency) {
        LettuceAssert.isTrue(filterEfficiency >= 0, "Filter efficiency must be greater than or equal to 0");
        this.filterEfficiency = Optional.of(filterEfficiency);
        return this;
    }

    /**
     * Enable truth mode for the vector search.
     *
     * @return {@code this}
     */
    public VSimArgs truth() {
        return truth(true);
    }

    /**
     * Enable or disable truth mode for the vector search.
     *
     * @param truth whether to enable truth mode.
     * @return {@code this}
     */
    public VSimArgs truth(boolean truth) {
        this.truth = Optional.of(truth);
        return this;
    }

    /**
     * Disable threading for the vector search.
     *
     * @return {@code this}
     */
    public VSimArgs noThread() {
        return noThread(true);
    }

    /**
     * Enable or disable threading for the vector search.
     *
     * @param noThread whether to disable threading.
     * @return {@code this}
     */
    public VSimArgs noThread(boolean noThread) {
        this.noThread = Optional.of(noThread);
        return this;
    }

    @Override
    public <K, V> void build(CommandArgs<K, V> args) {
        if (count.isPresent()) {
            args.add(CommandKeyword.COUNT).add(count.get());
        }

        if (explorationFactor.isPresent()) {
            args.add(CommandKeyword.EF).add(explorationFactor.get());
        }

        if (filter.isPresent()) {
            args.add(CommandKeyword.FILTER).add(filter.get());
        }

        if (filterEfficiency.isPresent()) {
            args.add(CommandKeyword.FILTER_EF).add(filterEfficiency.get());
        }

        if (truth.isPresent() && truth.get()) {
            args.add(CommandKeyword.TRUTH);
        }

        if (noThread.isPresent() && noThread.get()) {
            args.add(CommandKeyword.NOTHREAD);
        }
    }
}
