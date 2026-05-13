/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.output;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;

import io.lettuce.core.array.IndexedValue;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.internal.LettuceAssert;

/**
 * {@link List} of {@link IndexedValue} items, parsing alternating index/value pairs from Redis array responses.
 * <p>
 * Used for {@code ARSCAN} and {@code ARGREP WITHVALUES} commands which return flat arrays of
 * {@code [index, value, index, value, ...]}.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Aleksandar Todorov
 * @since 7.6
 */
public class IndexedValueListOutput<K, V> extends CommandOutput<K, V, List<IndexedValue<V>>>
        implements StreamingOutput<IndexedValue<V>> {

    private boolean initialized;

    private Subscriber<IndexedValue<V>> subscriber;

    private long currentIndex;

    private boolean hasIndex;

    public IndexedValueListOutput(RedisCodec<K, V> codec) {
        super(codec, Collections.emptyList());
        setSubscriber(ListSubscriber.instance());
    }

    @Override
    public void set(long integer) {
        currentIndex = integer;
        hasIndex = true;
    }

    @Override
    public void set(ByteBuffer bytes) {
        if (hasIndex) {
            V value = (bytes != null) ? codec.decodeValue(bytes) : null;
            subscriber.onNext(output, new IndexedValue<>(currentIndex, value));
            hasIndex = false;
        }
    }

    @Override
    public void multi(int count) {
        if (!initialized) {
            output = OutputFactory.newList(count / 2);
            initialized = true;
        }
    }

    @Override
    public void setSubscriber(Subscriber<IndexedValue<V>> subscriber) {
        LettuceAssert.notNull(subscriber, "Subscriber must not be null");
        this.subscriber = subscriber;
    }

    @Override
    public Subscriber<IndexedValue<V>> getSubscriber() {
        return subscriber;
    }

}
