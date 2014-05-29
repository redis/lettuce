// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.output;

import java.util.ArrayList;
import java.util.List;

import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.protocol.CommandOutput;

/**
 * {@link java.util.List} of boolean output.
 * 
 * @author Will Glozer
 * @param <K> Key type.
 * @param <V> Value type.
 */
public class BooleanListOutput<K, V> extends CommandOutput<K, V, List<Boolean>> {
    public BooleanListOutput(RedisCodec<K, V> codec) {
        super(codec, new ArrayList<Boolean>());
    }

    @Override
    public void set(long integer) {
        output.add((integer == 1) ? Boolean.TRUE : Boolean.FALSE);
    }
}
