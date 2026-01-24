/*
 * Copyright 2025, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */

package io.lettuce.core.output;

import io.lettuce.core.codec.RedisCodec;

import java.nio.ByteBuffer;

public class EncodedComplexOutput<K, V, T> extends ComplexOutput<K, V, T> {

    /**
     * Constructs a new instance of the {@link ComplexOutput}
     *
     * @param codec the {@link RedisCodec} to be applied
     * @param parser
     */
    public EncodedComplexOutput(RedisCodec<K, V> codec, ComplexDataParser<T> parser) {
        super(codec, parser);
    }

    @Override
    public void set(ByteBuffer bytes) {
        data.storeObject(copy(bytes));
    }

    @Override
    public void setSingle(ByteBuffer bytes) {
        data.storeObject(copy(bytes));
    }

    private ByteBuffer copy(ByteBuffer source) {
        if (source == null) {
            return null;
        }

        ByteBuffer copy = ByteBuffer.allocate(source.remaining());
        copy.put(source.duplicate());
        copy.flip();
        return copy;
    }

}
