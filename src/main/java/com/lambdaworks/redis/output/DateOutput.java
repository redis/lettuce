// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.output;

import java.util.Date;

import com.lambdaworks.redis.codec.RedisCodec;

/**
 * Date output with no milliseconds.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Will Glozer
 */
public class DateOutput<K, V> extends CommandOutput<K, V, Date> {
    public DateOutput(RedisCodec<K, V> codec) {
        super(codec, null);
    }

    @Override
    public void set(long time) {
        output = new Date(time * 1000);
    }
}
