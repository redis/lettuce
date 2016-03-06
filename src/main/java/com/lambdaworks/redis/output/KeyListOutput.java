// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.output;

import static com.google.common.base.Preconditions.*;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.lambdaworks.redis.codec.RedisCodec;

/**
 * {@link List} of keys output.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 *
 * @author Will Glozer
 */
public class KeyListOutput<K, V> extends CommandOutput<K, V, List<K>> implements StreamingOutput<K> {

    private Subscriber<K> subscriber;

    public KeyListOutput(RedisCodec<K, V> codec) {
        super(codec, new ArrayList<K>());
        setSubscriber(ListSubscriber.of(output));
    }

    @Override
    public void set(ByteBuffer bytes) {
        subscriber.onNext(codec.decodeKey(bytes));
    }

    @Override
    public void setSubscriber(Subscriber<K> subscriber) {
        checkArgument(subscriber != null, "subscriber must not be null");
        this.subscriber = subscriber;
    }

    @Override
    public Subscriber<K> getSubscriber() {
        return subscriber;
    }
}
