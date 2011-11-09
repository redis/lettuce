// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.output;

import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.protocol.CommandOutput;

import java.nio.ByteBuffer;

/**
 * 64-bit integer output, may be null.
 *
 * @author Will Glozer
 */
public class IntegerOutput<K, V> extends CommandOutput<K, V, Long> {
    private Long value;

    public IntegerOutput(RedisCodec<K, V> codec) {
        super(codec);
    }

    @Override
    public Long get() {
        errorCheck();
        return value;
    }

    @Override
    public void set(long integer) {
        value = integer;
    }

    @Override
    public void set(ByteBuffer bytes) {
        value = null;
    }
}
