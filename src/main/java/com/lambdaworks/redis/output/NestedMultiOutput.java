// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.output;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import com.lambdaworks.redis.RedisCommandExecutionException;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.internal.LettuceFactories;

/**
 * {@link List} of command outputs, possibly deeply nested.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Will Glozer
 */
public class NestedMultiOutput<K, V> extends CommandOutput<K, V, List<Object>> {
    private final Deque<List<Object>> stack;
    private int depth;

    public NestedMultiOutput(RedisCodec<K, V> codec) {
        super(codec, new ArrayList<>());
        stack = LettuceFactories.newSpScQueue();
        depth = 0;
    }

    @Override
    public void set(long integer) {
        output.add(integer);
    }

    @Override
    public void set(ByteBuffer bytes) {
        output.add(bytes == null ? null : codec.decodeValue(bytes));
    }

    @Override
    public void setError(ByteBuffer error) {
        output.add(new RedisCommandExecutionException(decodeAscii(error)));
    }

    @Override
    public void complete(int depth) {
        if (depth > 0 && depth < this.depth) {
            output = stack.pop();
            this.depth--;
        }
    }

    @Override
    public void multi(int count) {
        List<Object> a = new ArrayList<>(count);
        output.add(a);
        stack.push(output);
        output = a;
        this.depth++;
    }
}
