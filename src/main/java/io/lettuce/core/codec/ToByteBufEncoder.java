/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
 * @author shikharid
 * @since 4.3
 */
public interface ToByteBufEncoder<K, V> {

    /**
     * Encode the key for output to redis.
     *
     * @param key the key, may be {@code null}.
     * @param target the target buffer, must not be {@code null}.
     */
    void encodeKey(K key, ByteBuf target);

    /**
     * Encode the value for output to redis.
     *
     * @param value the value, may be {@code null}.
     * @param target the target buffer, must not be {@code null}.
     */
    void encodeValue(V value, ByteBuf target);

    /**
     * Estimates the size of the resulting byte stream. This method is called for keys and values to estimate the size for the
     * temporary buffer to allocate.
     *
     * @param keyOrValue the key or value, may be {@code null}.
     * @return the estimated number of bytes in the encoded representation.
     */
    int estimateSize(Object keyOrValue);

    /**
     * Returns {@code true} if {@link #estimateSize(Object)} returns exact size This is used as an optimization to reduce memory
     * allocations when encoding data.
     *
     * @return {@code true} if {@link #estimateSize(Object)} returns exact size.
     * @since 6.3.2
     */
    default boolean isEstimateExact() {
        return false;
    }

}
