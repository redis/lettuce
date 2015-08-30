// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.codec;

import java.nio.ByteBuffer;

/**
 * A {@link RedisCodec} encodes keys and values sent to Redis, and decodes keys and values in the command output.
 *
 * The methods are called by multiple threads and must be thread-safe.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 *
 * @author Will Glozer
 * @author Mark Paluch
 */
public interface RedisCodec<K, V> {
    /**
     * Decode the key output by redis.
     *
     * @param bytes Raw bytes of the key.
     *
     * @return The decoded key.
     */
    K decodeKey(ByteBuffer bytes);

    /**
     * Decode the value output by redis.
     *
     * @param bytes Raw bytes of the value.
     *
     * @return The decoded value.
     */
    V decodeValue(ByteBuffer bytes);

    /**
     * Encode the key for output to redis.
     *
     * @param key Key.
     *
     * @return The encoded key.
     */
    ByteBuffer encodeKey(K key);

    /**
     * Encode the value for output to redis.
     *
     * @param value Value.
     *
     * @return The encoded value.
     */
    ByteBuffer encodeValue(V value);

}
