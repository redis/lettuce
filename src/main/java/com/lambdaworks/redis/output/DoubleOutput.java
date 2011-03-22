// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.output;

import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.protocol.CommandOutput;

import java.nio.ByteBuffer;

import static java.lang.Double.parseDouble;

/**
 * Double output, may be null.
 *
 * @author Will Glozer
 */
public class DoubleOutput extends CommandOutput<Double> {
    private Double value;

    public DoubleOutput(RedisCodec<?, ?> codec) {
        super(codec);
    }

    @Override
    public Double get() {
        errorCheck();
        return value;
    }

    @Override
    public void set(ByteBuffer bytes) {
        value = (bytes == null) ? null : parseDouble(decodeAscii(bytes));
    }
}
