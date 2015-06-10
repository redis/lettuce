// Copyright (C) 2012 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.output;

import java.nio.ByteBuffer;

import com.lambdaworks.redis.codec.RedisCodec;

/**
 * Byte array output.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Will Glozer
 */
public class ByteArrayOutput<K, V> extends CommandOutput<K, V, byte[]> {
    public ByteArrayOutput(RedisCodec<K, V> codec) {
        super(codec, null);
    }

    @Override
    public void set(ByteBuffer bytes) {
        if (bytes != null) {
            output = new byte[bytes.remaining()];
            bytes.get(output);
        }
    }
}
