// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.output;

import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.protocol.CommandOutput;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link List} of values output.
 *
 * @param <V> Value type.
 *
 * @author Will Glozer
 */
public class ValueListOutput<K, V> extends CommandOutput<K, V, List<V>> {
    List<V> list = new ArrayList<V>();

    public ValueListOutput(RedisCodec<K, V> codec) {
        super(codec);
    }

    @Override
    public List<V> get() {
        errorCheck();
        return list;
    }

    @Override
    public void set(ByteBuffer bytes) {
        list.add(bytes == null ? null : codec.decodeValue(bytes));
    }
}
