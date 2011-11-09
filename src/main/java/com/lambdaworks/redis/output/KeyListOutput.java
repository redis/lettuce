// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.output;

import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.protocol.CommandOutput;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link List} of keys output.
 *
 * @param <K> Key type.
 *
 * @author Will Glozer
 */
public class KeyListOutput<K, V> extends CommandOutput<K, V, List<K>> {
    private List<K> keys = new ArrayList<K>();

    public KeyListOutput(RedisCodec<K, V> codec) {
        super(codec);
    }

    @Override
    public List<K> get() {
        errorCheck();
        return keys;
    }

    @Override
    public void set(ByteBuffer bytes) {
        keys.add(codec.decodeKey(bytes));
    }
}
