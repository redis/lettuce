// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.output;

import java.nio.ByteBuffer;

import com.lambdaworks.redis.codec.RedisCodec;

/**
 * Streaming-Output of Key Value Pairs. Returns the count of all Key-Value pairs (including null).
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * 
 * @author Mark Paluch
 */
public class KeyValueStreamingOutput<K, V> extends CommandOutput<K, V, Long> {
    private K key;
    private KeyValueStreamingChannel<K, V> channel;

    public KeyValueStreamingOutput(RedisCodec<K, V> codec, KeyValueStreamingChannel<K, V> channel) {
        super(codec, Long.valueOf(0));
        this.channel = channel;
    }

    @Override
    public void set(ByteBuffer bytes) {
        if (key == null) {
            key = codec.decodeKey(bytes);
            return;
        }

        V value = (bytes == null) ? null : codec.decodeValue(bytes);
        channel.onKeyValue(key, value);
        output = output.longValue() + 1;
        key = null;
    }
}
