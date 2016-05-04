// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.output;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.internal.LettuceAssert;

/**
 * {@link List} of string output.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Will Glozer
 */
public class StringListOutput<K, V> extends CommandOutput<K, V, List<String>> implements StreamingOutput<String>{

	private Subscriber<String> subscriber;

	public StringListOutput(RedisCodec<K, V> codec) {
		super(codec, new ArrayList<>());
		setSubscriber(ListSubscriber.of(output));
    }

    @Override
    public void set(ByteBuffer bytes) {
        subscriber.onNext(bytes == null ? null : decodeAscii(bytes));
    }

	@Override
	public void setSubscriber(Subscriber<String> subscriber) {
        LettuceAssert.notNull(subscriber, "subscriber must not be null");
		this.subscriber = subscriber;
	}

	@Override
	public Subscriber<String> getSubscriber() {
		return subscriber;
	}
}
