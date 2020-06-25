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

import java.util.Optional;
import java.util.function.Function;

import io.lettuce.core.internal.LettuceAssert;

/**
 * A scored-value extension to {@link Value}.
 *
 * @param <V> Value type.
 * @author Will Glozer
 * @author Mark Paluch
 */
@SuppressWarnings("serial")
public class ScoredValue<V> extends Value<V> {

    private static final ScoredValue<Object> EMPTY = new ScoredValue<>(0, null);

    private final double score;

    /**
     * Serializable constructor.
     */
    protected ScoredValue() {
        super(null);
        this.score = 0;
    }

    private ScoredValue(double score, V value) {
        super(value);
        this.score = score;
    }

    /**
     * Creates a {@link ScoredValue} from a {@code key} and an {@link Optional}. The resulting value contains the value from the
     * {@link Optional} if a value is present. Value is empty if the {@link Optional} is empty.
     *
     * @param score the score.
     * @param optional the optional. May be empty but never {@code null}.
     * @param <T>
     * @param <V>
     * @return the {@link ScoredValue}.
     */
    public static <T extends V, V> ScoredValue<V> from(double score, Optional<T> optional) {

        LettuceAssert.notNull(optional, "Optional must not be null");

        if (optional.isPresent()) {
            return new ScoredValue<V>(score, optional.get());
        }

        return fromNullable(score, null);
    }

    /**
     * Creates a {@link ScoredValue} from a {@code score} and {@code value}. The resulting value contains the value if the
     * {@code value} is not null.
     *
     * @param score the score.
     * @param value the value. May be {@code null}.
     * @param <T>
     * @param <V>
     * @return the {@link ScoredValue}.
     */
    public static <T extends V, V> ScoredValue<V> fromNullable(double score, T value) {

        if (value == null) {
            return new ScoredValue<V>(score, null);
        }

        return new ScoredValue<V>(score, value);
    }

    /**
     * Returns an empty {@code ScoredValue} instance. No value is present for this instance.
     *
     * @param <V>
     * @return the {@link ScoredValue}.
     */
    public static <V> ScoredValue<V> empty() {
        return (ScoredValue<V>) EMPTY;
    }

    /**
     * Creates a {@link ScoredValue} from a {@code key} and {@code value}. The resulting value contains the value.
     *
     * @param score the score.
     * @param value the value. Must not be {@code null}.
     * @param <T>
     * @param <V>
     * @return the {@link ScoredValue}.
     */
    public static <T extends V, V> ScoredValue<V> just(double score, T value) {

        LettuceAssert.notNull(value, "Value must not be null");

        return new ScoredValue<V>(score, value);
    }

    public double getScore() {
        return score;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ScoredValue))
            return false;
        if (!super.equals(o))
            return false;

        ScoredValue<?> that = (ScoredValue<?>) o;

        return Double.compare(that.score, score) == 0;
    }

    @Override
    public int hashCode() {

        long temp = Double.doubleToLongBits(score);
        int result = (int) (temp ^ (temp >>> 32));
        result = 31 * result + (hasValue() ? getValue().hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return hasValue() ? String.format("ScoredValue[%f, %s]", score, getValue())
                : String.format("ScoredValue[%f].empty", score);
    }

    /**
     * Returns a {@link ScoredValue} consisting of the results of applying the given function to the value of this element.
     * Mapping is performed only if a {@link #hasValue() value is present}.
     *
     * @param <R> The element type of the new stream.
     * @param mapper a stateless function to apply to each element.
     * @return the new {@link ScoredValue}.
     */
    @SuppressWarnings("unchecked")
    public <R> ScoredValue<R> map(Function<? super V, ? extends R> mapper) {

        LettuceAssert.notNull(mapper, "Mapper function must not be null");

        if (hasValue()) {
            return new ScoredValue<>(score, mapper.apply(getValue()));
        }

        return (ScoredValue<R>) this;
    }

    /**
     * Returns a {@link ScoredValue} consisting of the results of applying the given function to the score of this element.
     * Mapping is performed only if a {@link #hasValue() value is present}.
     *
     * @param mapper a stateless function to apply to each element.
     * @return the new {@link ScoredValue}.
     */
    @SuppressWarnings("unchecked")
    public ScoredValue<V> mapScore(Function<? super Number, ? extends Number> mapper) {

        LettuceAssert.notNull(mapper, "Mapper function must not be null");

        if (hasValue()) {
            return new ScoredValue<V>(mapper.apply(score).doubleValue(), getValue());
        }

        return this;
    }

}
