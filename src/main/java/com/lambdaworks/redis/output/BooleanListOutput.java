// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.output;

import java.util.ArrayList;
import java.util.List;

import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.internal.LettuceAssert;

/**
 * {@link java.util.List} of boolean output.
 *
 * @author Will Glozer
 * @param <K> Key type.
 * @param <V> Value type.
 */
public class BooleanListOutput<K, V> extends CommandOutput<K, V, List<Boolean>> implements StreamingOutput<Boolean> {

    private Subscriber<Boolean> subscriber;

    public BooleanListOutput(RedisCodec<K, V> codec) {
        super(codec, new ArrayList<>());
        setSubscriber(ListSubscriber.of(output));
    }

    @Override
    public void set(long integer) {
        subscriber.onNext((integer == 1) ? Boolean.TRUE : Boolean.FALSE);
    }

    @Override
    public void setSubscriber(Subscriber<Boolean> subscriber) {
        LettuceAssert.notNull(subscriber, "subscriber must not be null");
        this.subscriber = subscriber;
    }

    @Override
    public Subscriber<Boolean> getSubscriber() {
        return subscriber;
    }
}
