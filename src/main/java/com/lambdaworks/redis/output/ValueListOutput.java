// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.output;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.lambdaworks.redis.codec.RedisCodec;

/**
 * {@link List} of values output.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * 
 * @author Will Glozer
 */
public class ValueListOutput<K, V> extends CommandOutput<K, V, List<V>> {
    public ValueListOutput(RedisCodec<K, V> codec) {
        super(codec, new ArrayList<V>());
    }

    @Override
    public void set(ByteBuffer bytes) {
        output.add(bytes == null ? null : codec.decodeValue(bytes));
    }
}
