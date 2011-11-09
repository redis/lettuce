// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis.output;

import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.protocol.CommandOutput;

import java.nio.ByteBuffer;
import java.util.*;

/**
 * Output of all commands within a MULTI block.
 *
 * @author Will Glozer
 */
public class MultiOutput<K, V> extends CommandOutput<K, V, List<Object>> {
    private Queue<CommandOutput<K, V, ?>> queue;
    private List<Object> values;

    public MultiOutput(RedisCodec<K, V> codec) {
        super(codec);
        this.queue  = new LinkedList<CommandOutput<K, V, ?>>();
        this.values = new ArrayList<Object>();
    }

    public void add(CommandOutput<K, V, ?> cmd) {
        queue.add(cmd);
    }

    @Override
    public List<Object> get() {
        return values;
    }

    @Override
    public void set(long integer) {
        queue.peek().set(integer);
    }

    @Override
    public void set(ByteBuffer bytes) {
        queue.peek().set(bytes);
    }

    @Override
    public void setError(ByteBuffer error) {
        queue.peek().setError(error);
    }

    @Override
    public void complete(int depth) {
        if (depth == 1) {
            CommandOutput<K, V, ?> output = queue.remove();
            values.add(output.get());
        }
    }
}
