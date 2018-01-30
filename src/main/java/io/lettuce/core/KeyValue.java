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

import java.util.Optional;
import java.util.function.Function;

import io.lettuce.core.internal.LettuceAssert;

/**
 * A key-value container extension to {@link Value}. A {@link KeyValue} requires always a non-null key on construction.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Will Glozer
 * @author Mark Paluch
 */
public class KeyValue<K, V> extends Value<V> {

    private final K key;

    /**
     * Serializable constructor.
     */
    protected KeyValue() {
        super(null);
        this.key = null;
    }

    private KeyValue(K key, V value) {

        super(value);

        LettuceAssert.notNull(key, "Key must not be null");
        this.key = key;
    }

    /**
     * Creates a {@link KeyValue} from a {@code key} and an {@link Optional}. The resulting value contains the value from the
     * {@link Optional} if a value is present. Value is empty if the {@link Optional} is empty.
     *
     * @param key the key, must not be {@literal null}.
     * @param optional the optional. May be empty but never {@literal null}.
     * @param <K>
     * @param <T>
     * @param <V>
     * @return the {@link KeyValue}
     */
    public static <K, T extends V, V> KeyValue<K, V> from(K key, Optional<T> optional) {

        LettuceAssert.notNull(optional, "Optional must not be null");

        if (optional.isPresent()) {
            return new KeyValue<K, V>(key, optional.get());
        }

        return empty(key);
    }

    /**
     * Creates a {@link KeyValue} from a {@code key} and{@code value}. The resulting value contains the value if the
     * {@code value} is not null.
     *
     * @param key the key, must not be {@literal null}.
     * @param value the value. May be {@literal null}.
     * @param <K>
     * @param <T>
     * @param <V>
     * @return the {@link KeyValue}
     */
    public static <K, T extends V, V> KeyValue<K, V> fromNullable(K key, T value) {

        if (value == null) {
            return empty(key);
        }

        return new KeyValue<K, V>(key, value);
    }

    /**
     * Returns an empty {@code KeyValue} instance with the {@code key} set. No value is present for this instance.
     *
     * @param key the key, must not be {@literal null}.
     * @param <K>
     * @param <V>
     * @return the {@link KeyValue}
     */
    public static <K, V> KeyValue<K, V> empty(K key) {
        return new KeyValue<K, V>(key, null);
    }

    /**
     * Creates a {@link KeyValue} from a {@code key} and {@code value}. The resulting value contains the value.
     *
     * @param key the key. Must not be {@literal null}.
     * @param value the value. Must not be {@literal null}.
     * @param <K>
     * @param <T>
     * @param <V>
     * @return the {@link KeyValue}
     */
    public static <K, T extends V, V> KeyValue<K, V> just(K key, T value) {

        LettuceAssert.notNull(value, "Value must not be null");

        return new KeyValue<K, V>(key, value);
    }

    @Override
    public boolean equals(Object o) {

        if (this == o)
            return true;
        if (!(o instanceof KeyValue))
            return false;

        if (!super.equals(o))
            return false;

        KeyValue<?, ?> keyValue = (KeyValue<?, ?>) o;

        return key.equals(keyValue.key);
    }

    @Override
    public int hashCode() {
        int result = key.hashCode();
        result = 31 * result + (hasValue() ? getValue().hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return hasValue() ? String.format("KeyValue[%s, %s]", key, getValue()) : String.format("KeyValue[%s].empty", key);
    }

    /**
     *
     * @return the key
     */
    public K getKey() {
        return key;
    }

    /**
     * Returns a {@link KeyValue} consisting of the results of applying the given function to the value of this element. Mapping
     * is performed only if a {@link #hasValue() value is present}.
     *
     * @param <R> The element type of the new {@link KeyValue}
     * @param mapper a stateless function to apply to each element
     * @return the new {@link KeyValue}
     */
    @SuppressWarnings("unchecked")
    public <R> KeyValue<K, R> map(Function<? super V, ? extends R> mapper) {

        LettuceAssert.notNull(mapper, "Mapper function must not be null");

        if (hasValue()) {
            return new KeyValue<>(getKey(), mapper.apply(getValue()));
        }

        return (KeyValue<K, R>) this;
    }
}
