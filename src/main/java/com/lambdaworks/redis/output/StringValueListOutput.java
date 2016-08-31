// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.output;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.lambdaworks.redis.Value;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.internal.LettuceAssert;

/**
 * {@link List} of {@link com.lambdaworks.redis.Value} output.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 5.0
 */
public class StringValueListOutput<K, V> extends CommandOutput<K, V, List<Value<String>>>
        implements StreamingOutput<Value<String>> {

    private Subscriber<Value<String>> subscriber;

    public StringValueListOutput(RedisCodec<K, V> codec) {
        super(codec, new ArrayList<>());
        setSubscriber(ListSubscriber.of(output));
    }

    @Override
    public void set(ByteBuffer bytes) {
        subscriber.onNext(bytes == null ? Value.empty() : Value.fromNullable(decodeAscii(bytes)));
    }

    @Override
    public void setSubscriber(Subscriber<Value<String>> subscriber) {
        LettuceAssert.notNull(subscriber, "Subscriber must not be null");
        this.subscriber = subscriber;
    }

    @Override
    public Subscriber<Value<String>> getSubscriber() {
        return subscriber;
    }
}
