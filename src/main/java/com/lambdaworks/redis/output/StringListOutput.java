// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.output;

import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.protocol.CommandOutput;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link List} of string output.
 *
 * @author Will Glozer
 */
public class StringListOutput extends CommandOutput<List<String>> {
    private List<String> list = new ArrayList<String>();

    public StringListOutput(RedisCodec<?, ?> codec) {
        super(codec);
    }

    @Override
    public List<String> get() {
        errorCheck();
        return list;
    }

    @Override
    public void set(ByteBuffer bytes) {
        list.add(bytes == null ? null : decodeAscii(bytes));
    }
}
