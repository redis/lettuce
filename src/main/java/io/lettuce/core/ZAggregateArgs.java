/*
 * Copyright 2020 the original author or authors.
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

import static io.lettuce.core.protocol.CommandKeyword.*;

import java.util.ArrayList;
import java.util.List;

import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.CommandArgs;

/**
 * Argument list builder for the Redis <a href="http://redis.io/commands/zunion">ZUNION</a>,
 * <a href="http://redis.io/commands/zunionstore">ZUNIONSTORE</a>, <a href="http://redis.io/commands/zinter">ZINTER</a> and
 * <a href="http://redis.io/commands/zinterstore">ZINTERSTORE</a> commands. Static import the methods from {@link Builder} and
 * chain the method calls: {@code weights(1, 2).max()}.
 *
 * @author Will Glozer
 * @author Xy Ma
 * @author Mark Paluch
 * @author Mikhael Sokolov
 * @since 6.1
 */
public class ZAggregateArgs implements CompositeArgument {

    private enum Aggregate {
        SUM, MIN, MAX
    }

    private List<Double> weights;

    private Aggregate aggregate;

    /**
     * Builder entry points for {@link ScanArgs}.
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        Builder() {
        }

        /**
         * Creates new {@link ZAggregateArgs} setting {@literal WEIGHTS}.
         *
         * @return new {@link ZAddArgs} with {@literal WEIGHTS} set.
         * @see ZAggregateArgs#weights(double...)
         */
        public static ZAggregateArgs weights(double... weights) {
            return new ZAggregateArgs().weights(weights);
        }

        /**
         * Creates new {@link ZAggregateArgs} setting {@literal AGGREGATE SUM}.
         *
         * @return new {@link ZAddArgs} with {@literal AGGREGATE SUM} set.
         * @see ZAggregateArgs#sum()
         */
        public static ZAggregateArgs sum() {
            return new ZAggregateArgs().sum();
        }

        /**
         * Creates new {@link ZAggregateArgs} setting {@literal AGGREGATE MIN}.
         *
         * @return new {@link ZAddArgs} with {@literal AGGREGATE MIN} set.
         * @see ZAggregateArgs#sum()
         */
        public static ZAggregateArgs min() {
            return new ZAggregateArgs().min();
        }

        /**
         * Creates new {@link ZAggregateArgs} setting {@literal AGGREGATE MAX}.
         *
         * @return new {@link ZAddArgs} with {@literal AGGREGATE MAX} set.
         * @see ZAggregateArgs#sum()
         */
        public static ZAggregateArgs max() {
            return new ZAggregateArgs().max();
        }

    }

    /**
     * Specify a multiplication factor for each input sorted set.
     *
     * @param weights must not be {@code null}.
     * @return {@code this} {@link ZAggregateArgs}.
     */
    public ZAggregateArgs weights(double... weights) {

        LettuceAssert.notNull(weights, "Weights must not be null");

        this.weights = new ArrayList<>(weights.length);

        for (double weight : weights) {
            this.weights.add(weight);
        }
        return this;
    }

    /**
     * Aggregate scores of elements existing across multiple sets by summing up.
     *
     * @return {@code this} {@link ZAggregateArgs}.
     */
    public ZAggregateArgs sum() {

        this.aggregate = Aggregate.SUM;
        return this;
    }

    /**
     * Aggregate scores of elements existing across multiple sets by using the lowest score.
     *
     * @return {@code this} {@link ZAggregateArgs}.
     */
    public ZAggregateArgs min() {

        this.aggregate = Aggregate.MIN;
        return this;
    }

    /**
     * Aggregate scores of elements existing across multiple sets by using the highest score.
     *
     * @return {@code this} {@link ZAggregateArgs}.
     */
    public ZAggregateArgs max() {

        this.aggregate = Aggregate.MAX;
        return this;
    }

    @Override
    public <K, V> void build(CommandArgs<K, V> args) {

        if (weights != null && !weights.isEmpty()) {

            args.add(WEIGHTS);
            for (double weight : weights) {
                args.add(weight);
            }
        }

        if (aggregate != null) {
            args.add(AGGREGATE);
            switch (aggregate) {
                case SUM:
                    args.add(SUM);
                    break;
                case MIN:
                    args.add(MIN);
                    break;
                case MAX:
                    args.add(MAX);
                    break;
                default:
                    throw new IllegalArgumentException("Aggregation " + aggregate + " not supported");
            }
        }
    }

}
