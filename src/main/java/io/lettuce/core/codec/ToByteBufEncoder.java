/*
 * Copyright 2011-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.codec;

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
     * @param keyOrValue the key or value, may be {@literal null}.
     * @return the estimated number of bytes in the encoded representation.
     */
    int estimateSize(Object keyOrValue);
}
