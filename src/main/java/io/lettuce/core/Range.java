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

import java.util.Objects;

import io.lettuce.core.internal.LettuceAssert;

/**
 * {@link Range} defines {@literal lower} and {@literal upper} boundaries to retrieve items from a sorted set.
 *
 * @author Mark Paluch
 * @since 4.3
 */
public class Range<T> {

    private Boundary<T> lower;

    private Boundary<T> upper;

    private Range(Boundary<T> lower, Boundary<T> upper) {

        LettuceAssert.notNull(lower, "Lower boundary must not be null");
        LettuceAssert.notNull(upper, "Upper boundary must not be null");

        this.lower = lower;
        this.upper = upper;
    }

    /**
     * Create a new range from {@code lower} and {@code upper} boundary values. Both values are included (greater than or equals
     * and less than or equals).
     *
     * @param lower lower boundary, must not be {@code null}.
     * @param upper upper boundary, must not be {@code null}.
     * @param <T> value type
     * @return new {@link Range}
     */
    public static <T> Range<T> create(T lower, T upper) {

        LettuceAssert.isTrue(!(lower instanceof Boundary),
                "Lower must not be a Boundary. Use #from(Boundary, Boundary) instead");
        LettuceAssert.isTrue(!(upper instanceof Boundary),
                "Upper must not be a Boundary. Use #from(Boundary, Boundary) instead");

        return new Range<T>(Boundary.including(lower), Boundary.including(upper));
    }

    /**
     * Create a new range from {@code lower} and {@code upper} boundaries.
     *
     * @param lower lower boundary, must not be {@code null}.
     * @param upper upper boundary, must not be {@code null}.
     * @param <T> value type.
     * @return new {@link Range}
     */
    public static <T> Range<T> from(Boundary<T> lower, Boundary<T> upper) {
        return new Range<T>(lower, upper);
    }

    /**
     * @param <T> value type.
     * @return new {@link Range} with {@code lower} and {@code upper} set to {@link Boundary#unbounded()}.
     */
    public static <T> Range<T> unbounded() {
        return new Range<T>(Boundary.unbounded(), Boundary.unbounded());
    }

    /**
     * Greater than or equals {@code lower}.
     *
     * @param lower the lower boundary value.
     * @return {@code this} {@link Range} with {@code lower} applied.
     */
    public Range<T> gte(T lower) {

        this.lower = Boundary.including(lower);
        return this;
    }

    /**
     * Greater than {@code lower}.
     *
     * @param lower the lower boundary value.
     * @return {@code this} {@link Range} with {@code lower} applied.
     */
    public Range<T> gt(T lower) {

        this.lower = Boundary.excluding(lower);
        return this;
    }

    /**
     * Less than or equals {@code lower}.
     *
     * @param upper the upper boundary value.
     * @return {@code this} {@link Range} with {@code upper} applied.
     */
    public Range<T> lte(T upper) {

        this.upper = Boundary.including(upper);
        return this;
    }

    /**
     * Less than {@code lower}.
     *
     * @param upper the upper boundary value.
     * @return {@code this} {@link Range} with {@code upper} applied.
     */
    public Range<T> lt(T upper) {

        this.upper = Boundary.excluding(upper);
        return this;
    }

    /**
     * Return whether this {@link Range} is unbounded (i.e. upper and lower bounds are unbounded).
     *
     * @return whether this {@link Range} is unbounded
     * @since 6.0
     */
    public boolean isUnbounded() {
        return lower.isUnbounded() && upper.isUnbounded();
    }

    /**
     * @return the lower boundary.
     */
    public Boundary<T> getLower() {
        return lower;
    }

    /**
     * @return the upper boundary.
     */
    public Boundary<T> getUpper() {
        return upper;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Range))
            return false;
        Range<?> range = (Range<?>) o;
        return Objects.equals(lower, range.lower) && Objects.equals(upper, range.upper);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lower, upper);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName()).append(" [");
        sb.append(lower).append(" to ").append(upper).append("]");
        return sb.toString();
    }

    /**
     * @author Mark Paluch
     */
    public static class Boundary<T> {

        private static final Boundary<?> UNBOUNDED = new Boundary<>(null, true);

        private final T value;

        private final boolean including;

        private Boundary(T value, boolean including) {
            this.value = value;
            this.including = including;
        }

        /**
         * Creates an unbounded (infinite) boundary that marks the beginning/end of the range.
         *
         * @return the unbounded boundary.
         * @param <T> inferred type.
         */
        @SuppressWarnings("unchecked")
        public static <T> Boundary<T> unbounded() {
            return (Boundary<T>) UNBOUNDED;
        }

        /**
         * Create a {@link Boundary} based on the {@code value} that includes the value when comparing ranges. Greater or
         * equals, less or equals. but not Greater or equal, less or equal to {@code value}.
         *
         * @param value must not be {@code null}.
         * @param <T> value type.
         * @return the {@link Boundary}.
         */
        public static <T> Boundary<T> including(T value) {

            LettuceAssert.notNull(value, "Value must not be null");

            return new Boundary<>(value, true);
        }

        /**
         * Create a {@link Boundary} based on the {@code value} that excludes the value when comparing ranges. Greater or less
         * to {@code value} but not greater or equal, less or equal.
         *
         * @param value must not be {@code null}.
         * @param <T> value type.
         * @return the {@link Boundary}.
         */
        public static <T> Boundary<T> excluding(T value) {

            LettuceAssert.notNull(value, "Value must not be null");

            return new Boundary<>(value, false);
        }

        /**
         * @return the value
         */
        public T getValue() {
            return value;
        }

        /**
         * @return {@code true} if the boundary includes the value.
         */
        public boolean isIncluding() {
            return including;
        }

        /**
         * @return {@code true} if the bound is unbounded.
         * @since 6.0
         */
        public boolean isUnbounded() {
            return this == UNBOUNDED;
        }

        /**
         * @return {@code true} if the bound is unbounded.
         * @since 6.0
         */
        public boolean isBounded() {
            return this != UNBOUNDED;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof Boundary))
                return false;
            Boundary<?> boundary = (Boundary<?>) o;
            return including == boundary.including && Objects.equals(value, boundary.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value, including);
        }


        @Override
        public String toString() {

            if (value == null) {
                return "[unbounded]";
            }

            StringBuilder sb = new StringBuilder();
            if (including) {
                sb.append('[');
            } else {
                sb.append('(');
            }

            sb.append(value);
            return sb.toString();
        }

    }

}
