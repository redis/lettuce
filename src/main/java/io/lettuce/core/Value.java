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

import java.io.Serializable;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import io.lettuce.core.internal.LettuceAssert;

/**
 * A value container object which may or may not contain a non-null value. If a value is present, {@code isPresent()} will
 * return {@code true} and {@code get()} will return the value.
 *
 * <p>
 * Additional methods that depend on the presence or absence of a contained value are provided, such as
 * {@link #getValueOrElse(java.lang.Object) getValueOrElse()} (return a default value if value not present).
 *
 * @param <V> Value type.
 * @author Mark Paluch
 */
@SuppressWarnings("serial")
public class Value<V> implements Serializable {

    private static final Value<Object> EMPTY = new Value<>(null);

    private final V value;

    /**
     * {@link Serializable} constructor.
     */
    protected Value() {
        this.value = null;
    }

    /**
     *
     * @param value the value, may be {@code null}.
     */
    protected Value(V value) {
        this.value = value;
    }

    /**
     * Creates a {@link Value} from an {@link Optional}. The resulting value contains the value from the {@link Optional} if a
     * value is present. Value is empty if the {@link Optional} is empty.
     *
     * @param optional the optional. May be empty but never {@code null}.
     * @param <T>
     * @param <V>
     * @return the {@link Value}.
     */
    public static <T extends V, V> Value<V> from(Optional<T> optional) {

        LettuceAssert.notNull(optional, "Optional must not be null");

        if (optional.isPresent()) {
            return new Value<V>(optional.get());
        }

        return (Value<V>) EMPTY;
    }

    /**
     * Creates a {@link Value} from a {@code value}. The resulting value contains the value if the {@code value} is not null.
     *
     * @param value the value. May be {@code null}.
     * @param <T>
     * @param <V>
     * @return the {@link Value}.
     */
    public static <T extends V, V> Value<V> fromNullable(T value) {

        if (value == null) {
            return empty();
        }

        return new Value<V>(value);
    }

    /**
     * Returns an empty {@code Value} instance. No value is present for this instance.
     *
     * @param <V>
     * @return the {@link Value}.
     */
    public static <V> Value<V> empty() {
        return (Value<V>) EMPTY;
    }

    /**
     * Creates a {@link Value} from a {@code value}. The resulting value contains the value.
     *
     * @param value the value. Must not be {@code null}.
     * @param <T>
     * @param <V>
     * @return the {@link Value}.
     */
    public static <T extends V, V> Value<V> just(T value) {

        LettuceAssert.notNull(value, "Value must not be null");

        return new Value<V>(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Value))
            return false;

        Value<?> value1 = (Value<?>) o;

        return value != null ? value.equals(value1.value) : value1.value == null;

    }

    @Override
    public int hashCode() {
        return value != null ? value.hashCode() : 0;
    }

    @Override
    public String toString() {
        return hasValue() ? String.format("Value[%s]", value) : "Value.empty";
    }

    /**
     * If a value is present in this {@code Value}, returns the value, otherwise throws {@code NoSuchElementException}.
     *
     * @return the non-null value held by this {@code Optional}.
     * @throws NoSuchElementException if there is no value present
     * @see Value#hasValue()
     */
    public V getValue() {

        if (!hasValue()) {
            throw new NoSuchElementException();
        }

        return value;
    }

    /**
     * Return {@code true} if there is a value present, otherwise {@code false}.
     *
     * @return {@code true} if there is a value present, otherwise {@code false}.
     */
    public boolean hasValue() {
        return value != null;
    }

    /**
     * Return the value if present, otherwise invoke {@code other} and return the result of that invocation.
     *
     * @param otherSupplier a {@code Supplier} whose result is returned if no value is present. Must not be {@code null}.
     * @return the value if present otherwise the result of {@code other.get()}.
     * @throws NullPointerException if value is not present and {@code other} is null
     */
    public V getValueOrElseGet(Supplier<V> otherSupplier) {

        LettuceAssert.notNull(otherSupplier, "Supplier must not be null");

        if (hasValue()) {
            return value;
        }
        return otherSupplier.get();
    }

    /**
     * Return the value if present, otherwise return {@code other}.
     *
     * @param other the value to be returned if there is no value present, may be null.
     * @return the value, if present, otherwise {@code other}.
     */
    public V getValueOrElse(V other) {

        if (hasValue()) {
            return this.value;
        }

        return other;
    }

    /**
     * Return the contained value, if present, otherwise throw an exception to be created by the provided supplier.
     *
     * @param <X> Type of the exception to be thrown.
     * @param exceptionSupplier The supplier which will return the exception to be thrown, must not be {@code null}.
     * @return the present value.
     * @throws X if there is no value present
     */
    public <X extends Throwable> V getValueOrElseThrow(Supplier<? extends X> exceptionSupplier) throws X {

        LettuceAssert.notNull(exceptionSupplier, "Supplier function must not be null");

        if (hasValue()) {
            return value;
        }

        throw exceptionSupplier.get();
    }

    /**
     * Returns a {@link Value} consisting of the results of applying the given function to the value of this element. Mapping is
     * performed only if a {@link #hasValue() value is present}.
     *
     * @param <R> The element type of the new value.
     * @param mapper a stateless function to apply to each element.
     * @return the new {@link Value}.
     */
    @SuppressWarnings("unchecked")
    public <R> Value<R> map(Function<? super V, ? extends R> mapper) {

        LettuceAssert.notNull(mapper, "Mapper function must not be null");

        if (hasValue()) {
            return new Value<R>(mapper.apply(getValue()));
        }

        return (Value<R>) this;
    }

    /**
     * If a value is present, invoke the specified {@link java.util.function.Consumer} with the value, otherwise do nothing.
     *
     * @param consumer block to be executed if a value is present, must not be {@code null}.
     */
    public void ifHasValue(Consumer<? super V> consumer) {

        LettuceAssert.notNull(consumer, "Consumer must not be null");

        if (hasValue()) {
            consumer.accept(getValue());
        }
    }

    /**
     * If no value is present, invoke the specified {@link Runnable}, otherwise do nothing.
     *
     * @param runnable block to be executed if no value value is present, must not be {@code null}.
     */
    public void ifEmpty(Runnable runnable) {

        LettuceAssert.notNull(runnable, "Runnable must not be null");

        if (!hasValue()) {
            runnable.run();
        }
    }

    /**
     * Returns an {@link Optional} wrapper for the value.
     *
     * @return {@link Optional} wrapper for the value.
     */
    public Optional<V> optional() {
        return Optional.ofNullable(value);
    }

    /**
     * Returns a {@link Stream} wrapper for the value. The resulting stream contains either the value if a this value
     * {@link #hasValue() has a value} or it is empty if the value is empty.
     *
     * @return {@link Stream} wrapper for the value.
     */
    public Stream<V> stream() {

        if (hasValue()) {
            return Stream.of(value);
        }
        return Stream.empty();
    }

}
