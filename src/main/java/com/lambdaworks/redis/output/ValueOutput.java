// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.output;

import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.protocol.CommandOutput;

import java.nio.ByteBuffer;

/**
 * Value output.
 *
 * @param <V> Value type.
 *
 * @author Will Glozer
 */
public class ValueOutput<V> extends CommandOutput<V> {
    private V value;

    public ValueOutput(RedisCodec<?, V> codec) {
        super(codec);
    }

    @Override
    public V get() {
        errorCheck();
        return value;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void set(ByteBuffer bytes) {
        value = (bytes == null) ? null : (V) codec.decodeValue(bytes);
    }
}
