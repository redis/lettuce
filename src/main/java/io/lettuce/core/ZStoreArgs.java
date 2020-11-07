/*
 * Copyright 2011-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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

/**
 * Argument list builder for the Redis <a href="http://redis.io/commands/zunionstore">ZUNIONSTORE</a> and
 * <a href="http://redis.io/commands/zinterstore">ZINTERSTORE</a> commands. Static import the methods from {@link Builder} and
 * chain the method calls: {@code weights(1, 2).max()}.
 *
 * <p>
 * {@link ZAddArgs} is a mutable object and instances should be used only once to avoid shared mutable state.
 *
 * @author Will Glozer
 * @author Xy Ma
 * @author Mark Paluch
 * @author Mikhael Sokolov
 */
public class ZStoreArgs extends ZAggregateArgs {

    /**
     * Builder entry points for {@link ScanArgs}.
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {
        }

        /**
         * Creates new {@link ZStoreArgs} setting {@literal WEIGHTS} using long.
         *
         * @return new {@link ZAddArgs} with {@literal WEIGHTS} set.
         * @see ZStoreArgs#weights(long[])
         * @deprecated use {@link #weights(double...)}.
         */
        @Deprecated
        public static ZStoreArgs weights(long[] weights) {
            return new ZStoreArgs().weights(toDoubleArray(weights));
        }

        /**
         * Creates new {@link ZStoreArgs} setting {@literal WEIGHTS}.
         *
         * @return new {@link ZAddArgs} with {@literal WEIGHTS} set.
         * @see ZStoreArgs#weights(double...)
         */
        public static ZStoreArgs weights(double... weights) {
            return new ZStoreArgs().weights(weights);
        }

        /**
         * Creates new {@link ZStoreArgs} setting {@literal AGGREGATE SUM}.
         *
         * @return new {@link ZAddArgs} with {@literal AGGREGATE SUM} set.
         * @see ZStoreArgs#sum()
         */
        public static ZStoreArgs sum() {
            return new ZStoreArgs().sum();
        }

        /**
         * Creates new {@link ZStoreArgs} setting {@literal AGGREGATE MIN}.
         *
         * @return new {@link ZAddArgs} with {@literal AGGREGATE MIN} set.
         * @see ZStoreArgs#sum()
         */
        public static ZStoreArgs min() {
            return new ZStoreArgs().min();
        }

        /**
         * Creates new {@link ZStoreArgs} setting {@literal AGGREGATE MAX}.
         *
         * @return new {@link ZAddArgs} with {@literal AGGREGATE MAX} set.
         * @see ZStoreArgs#sum()
         */
        public static ZStoreArgs max() {
            return new ZStoreArgs().max();
        }

    }

    /**
     * Specify a multiplication factor for each input sorted set.
     *
     * @param weights must not be {@code null}.
     * @return {@code this} {@link ZStoreArgs}.
     * @deprecated use {@link #weights(double...)}
     */
    @Deprecated
    public static ZStoreArgs weights(long[] weights) {

        LettuceAssert.notNull(weights, "Weights must not be null");

        return new ZStoreArgs().weights(toDoubleArray(weights));
    }

    /**
     * Specify a multiplication factor for each input sorted set.
     *
     * @param weights must not be {@code null}.
     * @return {@code this} {@link ZStoreArgs}.
     */
    @Override
    public ZStoreArgs weights(double... weights) {

        super.weights(weights);
        return this;
    }

    /**
     * Aggregate scores of elements existing across multiple sets by summing up.
     *
     * @return {@code this} {@link ZStoreArgs}.
     */
    @Override
    public ZStoreArgs sum() {

        super.sum();
        return this;
    }

    /**
     * Aggregate scores of elements existing across multiple sets by using the lowest score.
     *
     * @return {@code this} {@link ZStoreArgs}.
     */
    @Override
    public ZStoreArgs min() {

        super.min();
        return this;
    }

    /**
     * Aggregate scores of elements existing across multiple sets by using the highest score.
     *
     * @return {@code this} {@link ZStoreArgs}.
     */
    @Override
    public ZStoreArgs max() {

        super.max();
        return this;
    }

    private static double[] toDoubleArray(long[] weights) {

        double[] result = new double[weights.length];
        for (int i = 0; i < weights.length; i++) {
            result[i] = weights[i];
        }
        return result;
    }

}
