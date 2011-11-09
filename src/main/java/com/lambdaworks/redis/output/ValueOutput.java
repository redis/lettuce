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
public class ValueOutput<K, V> extends CommandOutput<K, V, V> {
    private V value;

    public ValueOutput(RedisCodec<K, V> codec) {
        super(codec);
    }

    @Override
    public V get() {
        errorCheck();
        return value;
    }

    @Override
    public void set(ByteBuffer bytes) {
        value = (bytes == null) ? null : codec.decodeValue(bytes);
    }
}
