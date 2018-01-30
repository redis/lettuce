/*
 * Copyright 2011-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

import io.lettuce.core.protocol.CommandArgs;

/**
 * Argument list builder for the redis <a href="http://redis.io/commands/zunionstore">ZUNIONSTORE</a> and <a
 * href="http://redis.io/commands/zinterstore">ZINTERSTORE</a> commands. Static import the methods from {@link Builder} and
 * chain the method calls: {@code weights(1, 2).max()}.
 *
 * @author Will Glozer
 * @author Xy Ma
 */
public class ZStoreArgs implements CompositeArgument {

    private enum Aggregate {
        SUM, MIN, MAX
    }

    private List<Double> weights;
    private Aggregate aggregate;

    /**
     * Static builder methods.
     */
    public static class Builder {

        /**
         * Utility constructor.
         */
        private Builder() {

        }

        @Deprecated
        public static ZStoreArgs weights(long[] weights) {
            return new ZStoreArgs().weights(toDoubleArray(weights));
        }

        public static ZStoreArgs weights(double... weights) {
            return new ZStoreArgs().weights(weights);
        }

        public static ZStoreArgs sum() {
            return new ZStoreArgs().sum();
        }

        public static ZStoreArgs min() {
            return new ZStoreArgs().min();
        }

        public static ZStoreArgs max() {
            return new ZStoreArgs().max();
        }
    }

    @Deprecated
    public static ZStoreArgs weights(long[] weights) {
        return new ZStoreArgs().weights(toDoubleArray(weights));
    }

    public ZStoreArgs weights(double... weights) {
        this.weights = new ArrayList<>(weights.length);

        for (double weight : weights) {
            this.weights.add(weight);
        }
        return this;
    }

    public ZStoreArgs sum() {
        aggregate = Aggregate.SUM;
        return this;
    }

    public ZStoreArgs min() {
        aggregate = Aggregate.MIN;
        return this;
    }

    public ZStoreArgs max() {
        aggregate = Aggregate.MAX;
        return this;
    }

    private static double[] toDoubleArray(long[] weights) {
        double result[] = new double[weights.length];
        for (int i = 0; i < weights.length; i++) {
            result[i] = weights[i];
        }
        return result;
    }

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
