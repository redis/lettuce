/*
 * Copyright 2026, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core;

import java.nio.ByteBuffer;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.protocol.CommandArgs;

/**
 * Command args for RediSearch commands. This implementation hides the first key as RediSearch command arguments (index names,
 * index definition arguments, schema field names, aliases, dictionary names, etc.) are not keys from the key-space.
 *
 * @author Viktoriya Kutsarova
 * @since 7.6
 */
class RediSearchCommandArgs<K, V> extends CommandArgs<K, V> {

    /**
     * @param codec Codec used to encode/decode keys and values, must not be {@code null}.
     */
    RediSearchCommandArgs(RedisCodec<K, V> codec) {
        super(codec);
    }

    /**
     * @return always {@code null}.
     */
    @Override
    public ByteBuffer getFirstEncodedKey() {
        return null;
    }

}
