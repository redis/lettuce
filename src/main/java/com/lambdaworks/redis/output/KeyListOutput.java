// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.output;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.lambdaworks.redis.codec.RedisCodec;

/**
 * {@link List} of keys output.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * 
 * @author Will Glozer
 */
public class KeyListOutput<K, V> extends CommandOutput<K, V, List<K>> {
    public KeyListOutput(RedisCodec<K, V> codec) {
        super(codec, new ArrayList<K>());
    }

    @Override
    public void set(ByteBuffer bytes) {
        output.add(codec.decodeKey(bytes));
    }
}
