package com.lambdaworks.redis.codec;

import io.netty.buffer.ByteBuf;

/**
 * Optimized encoder that encodes keys and values directly on a {@link ByteBuf}. This encoder does not allocate buffers, it just
 * encodes data to existing buffers.
 * <p>
 * Classes implementing {@link ToByteBufEncoder} are required to implement {@link RedisCodec} as well. You should implement also
 * the {@link RedisCodec#encodeKey(Object)} and {@link RedisCodec#encodeValue(Object)} methods to ensure compatibility for users
 * that access the {@link RedisCodec} API only.
 * </p>
 * 
 * @author Mark Paluch
 * @since 4.3
 */
public interface ToByteBufEncoder<K, V> {

    /**
     * Encode the key for output to redis.
     *
     * @param key the key, may be {@literal null}.
     * @param target the target buffer, must not be {@literal null}.
     */
    void encodeKey(K key, ByteBuf target);

    /**
     * Encode the value for output to redis.
     *
     * @param value the value, may be {@literal null}.
     * @param target the target buffer, must not be {@literal null}.
     */
    void encodeValue(V value, ByteBuf target);

    /**
     * Estimates the size of the resulting byte stream. This method is called for keys and values to estimate the size for the
     * temporary buffer to allocate.
     *
     * @param keyOrValue the key or value, may be {@link null}.
     * @return the estimated number of bytes in the encoded representation.
     */
    int estimateSize(Object keyOrValue);
}
