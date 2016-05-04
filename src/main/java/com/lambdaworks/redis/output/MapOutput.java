// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.output;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;

import com.lambdaworks.redis.codec.RedisCodec;

/**
 * {@link Map} of keys and values output.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 *
 * @author Will Glozer
 */
public class MapOutput<K, V> extends CommandOutput<K, V, Map<K, V>> {
    private K key;

    public MapOutput(RedisCodec<K, V> codec) {
        super(codec, new LinkedHashMap<>());
    }

    @Override
    public void set(ByteBuffer bytes) {
        if (key == null) {
            key = codec.decodeKey(bytes);
            return;
        }

        V value = (bytes == null) ? null : codec.decodeValue(bytes);
        output.put(key, value);
        key = null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void set(long integer) {
        if (key == null) {
            key = (K) Long.valueOf(integer);
            return;
        }

        V value = (V) Long.valueOf(integer);
        output.put(key, value);
        key = null;
    }
}
