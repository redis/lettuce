// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.output;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.lambdaworks.redis.ScoredValue;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.internal.LettuceAssert;

/**
 * {@link List} of values and their associated scores.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 *
 * @author Will Glozer
 */
public class ScoredValueListOutput<K, V> extends CommandOutput<K, V, List<ScoredValue<V>>>
        implements StreamingOutput<ScoredValue<V>> {
    private V value;
    private Subscriber<ScoredValue<V>> subscriber;

    public ScoredValueListOutput(RedisCodec<K, V> codec) {
        super(codec, new ArrayList<>());
        setSubscriber(ListSubscriber.of(output));
    }

    @Override
    public void set(ByteBuffer bytes) {
        if (value == null) {
            value = codec.decodeValue(bytes);
            return;
        }

        double score = Double.parseDouble(decodeAscii(bytes));
        subscriber.onNext(new ScoredValue<>(score, value));
        value = null;
    }

    @Override
    public void setSubscriber(Subscriber<ScoredValue<V>> subscriber) {
        LettuceAssert.notNull(subscriber, "subscriber must not be null");
        this.subscriber = subscriber;
    }

    @Override
    public Subscriber<ScoredValue<V>> getSubscriber() {
        return subscriber;
    }
}
