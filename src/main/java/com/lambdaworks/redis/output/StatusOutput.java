// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.output;

import java.nio.ByteBuffer;

import com.lambdaworks.redis.codec.RedisCodec;

import static com.lambdaworks.redis.protocol.LettuceCharsets.buffer;

/**
 * Status message output.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Will Glozer
 */
public class StatusOutput<K, V> extends CommandOutput<K, V, String> {
    private static final ByteBuffer OK = buffer("OK");

    public StatusOutput(RedisCodec<K, V> codec) {
        super(codec, null);
    }

    @Override
    public void set(ByteBuffer bytes) {
        output = OK.equals(bytes) ? "OK" : decodeAscii(bytes);
    }
}
