// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.output;

import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.protocol.CommandOutput;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

/**
 * {@link Set} of value output.
 *
 * @param <V> Value type.
 *
 * @author Will Glozer
 */
public class ValueSetOutput<V> extends CommandOutput<Set<V>> {
    private Set<V> set = new HashSet<V>();

    public ValueSetOutput(RedisCodec<?, V> codec) {
        super(codec);
    }

    @Override
    public Set<V> get() {
        errorCheck();
        return set;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void set(ByteBuffer bytes) {
        set.add(bytes == null ? null : (V) codec.decodeValue(bytes));
    }
}
