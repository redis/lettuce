// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.output;

import java.nio.ByteBuffer;

import com.lambdaworks.redis.codec.RedisCodec;

/**
 * Boolean output. The actual value is returned as an integer where 0 indicates false and 1 indicates true, or as a null bulk
 * reply for script output.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Will Glozer
 */
public class BooleanOutput<K, V> extends CommandOutput<K, V, Boolean> {
    public BooleanOutput(RedisCodec<K, V> codec) {
        super(codec, null);
    }

    @Override
    public void set(long integer) {
        output = (integer == 1) ? Boolean.TRUE : Boolean.FALSE;
    }

    @Override
    public void set(ByteBuffer bytes) {
        output = (bytes != null) ? Boolean.TRUE : Boolean.FALSE;
    }
}
