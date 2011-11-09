// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.output;

import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.protocol.CommandOutput;

import java.util.Date;

/**
 * Date output with no milliseconds.
 *
 * @author Will Glozer
 */
public class DateOutput<K, V> extends CommandOutput<K, V, Date> {
    private Date value;

    public DateOutput(RedisCodec<K, V> codec) {
        super(codec);
    }

    @Override
    public Date get() {
        errorCheck();
        return value;
    }

    @Override
    public void set(long time) {
        value = new Date(time * 1000);
    }
}
