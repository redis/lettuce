// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.output;

import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.protocol.CommandOutput;

/**
 * Boolean output. The actual value is returned as an integer
 * where 0 indicates false and 1 indicates true.
 *
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
}
