// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.output;

import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.protocol.CommandOutput;

import java.nio.ByteBuffer;

/**
 * Status message output.
 *
 * @author Will Glozer
 */
public class StatusOutput extends CommandOutput<String> {
    private static final ByteBuffer OK = ByteBuffer.wrap("OK".getBytes());

    private String status;

    public StatusOutput(RedisCodec<?, ?> codec) {
        super(codec);
    }

    @Override
    public String get() {
        errorCheck();
        return status;
    }

    @Override
    public void set(ByteBuffer bytes) {
        if (bytes == null) return;
        status = OK.equals(bytes) ? "OK" : decodeAscii(bytes);
    }
}
