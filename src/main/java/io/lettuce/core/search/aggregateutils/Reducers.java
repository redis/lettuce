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

    public static Count count() {
        return new Count();
    }

    public static CountDistinct countDistinct(String field) {
        return new CountDistinct(field);
    }

    public static CountDistinctish countDistinctish(String field) {
        return new CountDistinctish(field);
    }

    public static Sum sum(String field) {
        return new Sum(field);
    }

    public static Avg avg(String field) {
        return new Avg(field);
    }

    public static Min min(String field) {
        return new Min(field);
    }

    public static Max max(String field) {
        return new Max(field);
    }

    public static Stddev stddev(String field) {
        return new Stddev(field);
    }

    public static Quantile quantile(String field, double quantile) {
        return new Quantile(field, quantile);
    }

    public static ToList toList(String field) {
        return new ToList(field);
    }

    public static FirstValue firstValue(String field) {
        return new FirstValue(field);
    }

    public static RandomSample randomSample(String field, int sampleSize) {
        return new RandomSample(field, sampleSize);
    }

    // ==================== Concrete Reducer Implementations ====================

    public static class Count extends Reducer {

        Count() {
            super(ReduceFunction.COUNT);
        }

        @Override
        protected List<Object> getOwnArgs() {
            return Collections.emptyList();
        }

    }

    public static class CountDistinct extends Reducer {

        private final String field;

        CountDistinct(String field) {
            super(ReduceFunction.COUNT_DISTINCT);
            LettuceAssert.notNull(field, "Field must not be null");
            this.field = field;
        }

        @Override
        protected List<Object> getOwnArgs() {
            return Collections.singletonList(field);
        }

    }

    public static class CountDistinctish extends Reducer {

        private final String field;

        CountDistinctish(String field) {
            super(ReduceFunction.COUNT_DISTINCTISH);
            LettuceAssert.notNull(field, "Field must not be null");
            this.field = field;
        }

        @Override
        protected List<Object> getOwnArgs() {
            return Collections.singletonList(field);
        }

    }

    public static class Sum extends Reducer {

        private final String field;

        Sum(String field) {
            super(ReduceFunction.SUM);
            LettuceAssert.notNull(field, "Field must not be null");
            this.field = field;
        }

        @Override
        protected List<Object> getOwnArgs() {
            return Collections.singletonList(field);
        }

    }

    public static class Avg extends Reducer {

        private final String field;

        Avg(String field) {
            super(ReduceFunction.AVG);
            LettuceAssert.notNull(field, "Field must not be null");
            this.field = field;
        }

        @Override
        protected List<Object> getOwnArgs() {
            return Collections.singletonList(field);
        }

    }

    public static class Min extends Reducer {

        private final String field;

        Min(String field) {
            super(ReduceFunction.MIN);
            LettuceAssert.notNull(field, "Field must not be null");
            this.field = field;
        }

        @Override
        protected List<Object> getOwnArgs() {
            return Collections.singletonList(field);
        }

    }

    public static class Max extends Reducer {

        private final String field;

        Max(String field) {
            super(ReduceFunction.MAX);
            LettuceAssert.notNull(field, "Field must not be null");
            this.field = field;
        }

        @Override
        protected List<Object> getOwnArgs() {
            return Collections.singletonList(field);
        }

    }

    public static class Stddev extends Reducer {

        private final String field;

        Stddev(String field) {
            super(ReduceFunction.STDDEV);
            LettuceAssert.notNull(field, "Field must not be null");
            this.field = field;
        }

        @Override
        protected List<Object> getOwnArgs() {
            return Collections.singletonList(field);
        }

    }

    public static class Quantile extends Reducer {

        private final String field;

        private final double quantile;

        Quantile(String field, double quantile) {
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

    public static class ToList extends Reducer {

        private final String field;

        ToList(String field) {
            super(ReduceFunction.TOLIST);
            LettuceAssert.notNull(field, "Field must not be null");
            this.field = field;
        }

        @Override
        protected List<Object> getOwnArgs() {
            return Collections.singletonList(field);
        }

    }

    public static class FirstValue extends Reducer {

        private final String field;

        private String byField;

        private SortDirection byDirection;

        FirstValue(String field) {
            super(ReduceFunction.FIRST_VALUE);
            LettuceAssert.notNull(field, "Field must not be null");
            this.field = field;
        }

        public FirstValue by(String byField) {
            this.byField = byField;
            return this;
        }

        public FirstValue by(String byField, SortDirection direction) {
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

    public static class RandomSample extends Reducer {

        private final String field;

        private final int sampleSize;

        RandomSample(String field, int sampleSize) {
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
