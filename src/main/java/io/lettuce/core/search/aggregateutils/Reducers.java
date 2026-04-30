/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.search.aggregateutils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.lettuce.core.annotations.Experimental;
import io.lettuce.core.internal.LettuceAssert;

/**
 * Factory class for creating {@link Reducer} instances.
 * <p>
 * Example usage:
 * </p>
 *
 * <pre>
 * {@code
 * // Count items in each group
 * Reducers.count().as("item_count")
 *
 * // Sum numeric values
 * Reducers.sum("@sales").as("total_sales")
 *
 * // Calculate average
 * Reducers.avg("@price").as("average_price")
 *
 * // Quantile with percentile parameter
 * Reducers.quantile("@price", 0.5).as("median_price")
 *
 * // First value with optional sort by
 * Reducers.firstValue("@name").by("@timestamp").as("first_name")
 *
 * // Random sample
 * Reducers.randomSample("@id", 10).as("sample_ids")
 * }
 * </pre>
 *
 * @author Aleksandar Todorov
 * @since 7.5
 * @see Reducer
 * @see ReduceFunction
 */
@Experimental
public final class Reducers {

    private Reducers() {
    }

    public static <K> Count<K> count() {
        return new Count<>();
    }

    public static <K> CountDistinct<K> countDistinct(K field) {
        return new CountDistinct<>(field);
    }

    public static <K> CountDistinctish<K> countDistinctish(K field) {
        return new CountDistinctish<>(field);
    }

    public static <K> Sum<K> sum(K field) {
        return new Sum<>(field);
    }

    public static <K> Avg<K> avg(K field) {
        return new Avg<>(field);
    }

    public static <K> Min<K> min(K field) {
        return new Min<>(field);
    }

    public static <K> Max<K> max(K field) {
        return new Max<>(field);
    }

    public static <K> Stddev<K> stddev(K field) {
        return new Stddev<>(field);
    }

    public static <K> Quantile<K> quantile(K field, double quantile) {
        return new Quantile<>(field, quantile);
    }

    public static <K> ToList<K> toList(K field) {
        return new ToList<>(field);
    }

    public static <K> FirstValue<K> firstValue(K field) {
        return new FirstValue<>(field);
    }

    public static <K> RandomSample<K> randomSample(K field, int sampleSize) {
        return new RandomSample<>(field, sampleSize);
    }

    // ==================== Concrete Reducer Implementations ====================

    public static class Count<K> extends Reducer<K> {

        Count() {
            super(ReduceFunction.COUNT);
        }

        @Override
        protected List<Object> getOwnArgs() {
            return Collections.emptyList();
        }

    }

    public static class CountDistinct<K> extends Reducer<K> {

        private final K field;

        CountDistinct(K field) {
            super(ReduceFunction.COUNT_DISTINCT);
            LettuceAssert.notNull(field, "Field must not be null");
            this.field = field;
        }

        @Override
        protected List<Object> getOwnArgs() {
            return Collections.singletonList(field);
        }

    }

    public static class CountDistinctish<K> extends Reducer<K> {

        private final K field;

        CountDistinctish(K field) {
            super(ReduceFunction.COUNT_DISTINCTISH);
            LettuceAssert.notNull(field, "Field must not be null");
            this.field = field;
        }

        @Override
        protected List<Object> getOwnArgs() {
            return Collections.singletonList(field);
        }

    }

    public static class Sum<K> extends Reducer<K> {

        private final K field;

        Sum(K field) {
            super(ReduceFunction.SUM);
            LettuceAssert.notNull(field, "Field must not be null");
            this.field = field;
        }

        @Override
        protected List<Object> getOwnArgs() {
            return Collections.singletonList(field);
        }

    }

    public static class Avg<K> extends Reducer<K> {

        private final K field;

        Avg(K field) {
            super(ReduceFunction.AVG);
            LettuceAssert.notNull(field, "Field must not be null");
            this.field = field;
        }

        @Override
        protected List<Object> getOwnArgs() {
            return Collections.singletonList(field);
        }

    }

    public static class Min<K> extends Reducer<K> {

        private final K field;

        Min(K field) {
            super(ReduceFunction.MIN);
            LettuceAssert.notNull(field, "Field must not be null");
            this.field = field;
        }

        @Override
        protected List<Object> getOwnArgs() {
            return Collections.singletonList(field);
        }

    }

    public static class Max<K> extends Reducer<K> {

        private final K field;

        Max(K field) {
            super(ReduceFunction.MAX);
            LettuceAssert.notNull(field, "Field must not be null");
            this.field = field;
        }

        @Override
        protected List<Object> getOwnArgs() {
            return Collections.singletonList(field);
        }

    }

    public static class Stddev<K> extends Reducer<K> {

        private final K field;

        Stddev(K field) {
            super(ReduceFunction.STDDEV);
            LettuceAssert.notNull(field, "Field must not be null");
            this.field = field;
        }

        @Override
        protected List<Object> getOwnArgs() {
            return Collections.singletonList(field);
        }

    }

    public static class Quantile<K> extends Reducer<K> {

        private final K field;

        private final double quantile;

        Quantile(K field, double quantile) {
            super(ReduceFunction.QUANTILE);
            LettuceAssert.notNull(field, "Field must not be null");
            LettuceAssert.isTrue(quantile >= 0 && quantile <= 1, "Quantile must be between 0 and 1");
            this.field = field;
            this.quantile = quantile;
        }

        @Override
        protected List<Object> getOwnArgs() {
            List<Object> args = new ArrayList<>();
            args.add(field);
            args.add(quantile);
            return args;
        }

    }

    public static class ToList<K> extends Reducer<K> {

        private final K field;

        ToList(K field) {
            super(ReduceFunction.TOLIST);
            LettuceAssert.notNull(field, "Field must not be null");
            this.field = field;
        }

        @Override
        protected List<Object> getOwnArgs() {
            return Collections.singletonList(field);
        }

    }

    public static class FirstValue<K> extends Reducer<K> {

        private final K field;

        private K byField;

        private SortDirection byDirection;

        FirstValue(K field) {
            super(ReduceFunction.FIRST_VALUE);
            LettuceAssert.notNull(field, "Field must not be null");
            this.field = field;
        }

        public FirstValue<K> by(K byField) {
            this.byField = byField;
            return this;
        }

        public FirstValue<K> by(K byField, SortDirection direction) {
            this.byField = byField;
            this.byDirection = direction;
            return this;
        }

        @Override
        protected List<Object> getOwnArgs() {
            List<Object> args = new ArrayList<>();
            args.add(field);
            if (byField != null) {
                args.add("BY");
                args.add(byField);
                if (byDirection != null) {
                    args.add(byDirection.name());
                }
            }
            return args;
        }

    }

    public static class RandomSample<K> extends Reducer<K> {

        private final K field;

        private final int sampleSize;

        RandomSample(K field, int sampleSize) {
            super(ReduceFunction.RANDOM_SAMPLE);
            LettuceAssert.notNull(field, "Field must not be null");
            LettuceAssert.isTrue(sampleSize > 0, "Sample size must be positive");
            this.field = field;
            this.sampleSize = sampleSize;
        }

        @Override
        protected List<Object> getOwnArgs() {
            List<Object> args = new ArrayList<>();
            args.add(field);
            args.add(sampleSize);
            return args;
        }

    }

}
