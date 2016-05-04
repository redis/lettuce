// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.output;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

import com.lambdaworks.redis.codec.RedisCodec;

/**
 * {@link Set} of value output.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * 
 * @author Will Glozer
 */
public class ValueSetOutput<K, V> extends CommandOutput<K, V, Set<V>> {
    public ValueSetOutput(RedisCodec<K, V> codec) {
        super(codec, new HashSet<>());
    }

    @Override
    public void set(ByteBuffer bytes) {
        output.add(bytes == null ? null : codec.decodeValue(bytes));
    }
}
