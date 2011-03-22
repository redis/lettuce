// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.output;

import com.lambdaworks.redis.ScoredValue;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.protocol.CommandOutput;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link List} of values and their associated scores.
 *
 * @param <V> Value type.
 *
 * @author Will Glozer
 */
public class ScoredValueListOutput<V> extends CommandOutput<List<ScoredValue<V>>> {
    private List<ScoredValue<V>> list = new ArrayList<ScoredValue<V>>();
    private V value;

    public ScoredValueListOutput(RedisCodec<?, V> codec) {
        super(codec);
    }

    @Override
    public List<ScoredValue<V>> get() {
        errorCheck();
        return list;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void set(ByteBuffer bytes) {
        if (value == null) {
            value = (V) codec.decodeValue(bytes);
            return;
        }

        double score = Double.parseDouble(decodeAscii(bytes));
        list.add(new ScoredValue<V>(score, value));
        value = null;
    }
}
