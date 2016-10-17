// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis;

import static com.lambdaworks.redis.protocol.CommandKeyword.*;

import java.util.ArrayList;
import java.util.List;

import com.lambdaworks.redis.protocol.CommandArgs;

/**
 * Argument list builder for the redis <a href="http://redis.io/commands/zunionstore">ZUNIONSTORE</a> and
 * <a href="http://redis.io/commands/zinterstore">ZINTERSTORE</a> commands. Static import the methods from {@link Builder} and
 * chain the method calls: {@code weights(1, 2).max()}.
 * 
 * @author Will Glozer
 */
public class ZStoreArgs implements CompositeArgument {

    private static enum Aggregate {
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

        if (weights != null) {

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
