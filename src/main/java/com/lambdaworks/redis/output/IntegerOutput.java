// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.output;

import java.nio.ByteBuffer;

import com.lambdaworks.redis.codec.RedisCodec;

/**
 * 64-bit integer output, may be null.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Will Glozer
 */
public class IntegerOutput<K, V> extends CommandOutput<K, V, Long> {
    public IntegerOutput(RedisCodec<K, V> codec) {
        super(codec, null);
    }

    @Override
    public void set(long integer) {
        output = integer;
    }

    @Override
    public void set(ByteBuffer bytes) {
        output = null;
    }
}
