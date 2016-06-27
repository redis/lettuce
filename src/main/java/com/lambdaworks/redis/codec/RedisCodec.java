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
 */
public abstract class RedisCodec<K, V> {
    /**
     * Decode the key output by redis.
     *
     * @param bytes Raw bytes of the key, must not be {@literal null}.
     *
     * @return The decoded key, may be {@literal null}.
     */
    public abstract K decodeKey(ByteBuffer bytes);

    /**
     * Decode the value output by redis.
     *
     * @param bytes Raw bytes of the value, must not be {@literal null}.
     *
     * @return The decoded value, may be {@literal null}.
     */
    public abstract V decodeValue(ByteBuffer bytes);

    /**
     * Encode the key for output to redis.
     *
     * @param key the key, may be {@literal null}.
     *
     * @return The encoded key, never {@literal null}.
     */
    public abstract byte[] encodeKey(K key);

    /**
     * Encode the value for output to redis.
     *
     * @param value the value, may be {@literal null}.
     *
     * @return The encoded value, never {@literal null}.
     */
    public abstract byte[] encodeValue(V value);
}
