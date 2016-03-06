// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.output;

import static com.google.common.base.Preconditions.*;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.lambdaworks.redis.codec.RedisCodec;

/**
 * {@link List} of values output.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 *
 * @author Will Glozer
 */
public class ValueListOutput<K, V> extends CommandOutput<K, V, List<V>> implements StreamingOutput<V> {

    private Subscriber<V> subscriber;

    public ValueListOutput(RedisCodec<K, V> codec) {
        super(codec, new ArrayList<>());
        setSubscriber(ListSubscriber.of(output));
    }

    @Override
    public void set(ByteBuffer bytes) {
        subscriber.onNext(bytes == null ? null : codec.decodeValue(bytes));
    }

    @Override
    public void setSubscriber(Subscriber<V> subscriber) {
        checkArgument(subscriber != null, "subscriber must not be null");
        this.subscriber = subscriber;
    }

    @Override
    public Subscriber<V> getSubscriber() {
        return subscriber;
    }
}
