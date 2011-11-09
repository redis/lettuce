// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.output;

import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.protocol.CommandOutput;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link Map} of keys and values output.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 *
 * @author Will Glozer
 */
public class MapOutput<K, V> extends CommandOutput<K, V, Map<K, V>> {
    private Map<K, V> map = new HashMap<K, V>();
    private K key;

    public MapOutput(RedisCodec<K, V> codec) {
        super(codec);
    }

    @Override
    public Map<K, V> get() {
        errorCheck();
        return map;
    }

    @Override
    public void set(ByteBuffer bytes) {
        if (key == null) {
            key = codec.decodeKey(bytes);
            return;
        }

        V value = (bytes == null) ? null : codec.decodeValue(bytes);
        map.put(key, value);
        key = null;
    }
}
