// Copyright (C) 2013 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.output;

import java.nio.ByteBuffer;

import com.lambdaworks.redis.codec.RedisCodec;

/**
 * Key output.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * 
 * @author Will Glozer
 */
public class KeyOutput<K, V> extends CommandOutput<K, V, K> {
    public KeyOutput(RedisCodec<K, V> codec) {
        super(codec, null);
    }

    @Override
    public void set(ByteBuffer bytes) {
        output = (bytes == null) ? null : codec.decodeKey(bytes);
    }
}
