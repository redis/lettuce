// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis;

import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.protocol.*;

import java.util.ArrayList;
import java.util.List;

/**
 * An asynchronous {@link RedisConnection} wrapper. All redis command methods
 * with the exception of {@link #multi}, {@link #exec} and {@link #discard} will
 * immediately return null and their output can be collected later via the
 * {@link #flush} method.
 *
 * RedisAsyncConnections are not thread-safe and should only be used by a
 * single thread.
 *
 * @author Will Glozer
 */
public class RedisAsyncConnection<K, V> extends RedisConnection<K, V> {
    private RedisConnection<K, V> parent;
    private List<Command<?>> pipeline;

    /**
     * Initialize a new connection.
     *
     * @param codec     Codec used to encode/decode keys and values.
     * @param parent    Parent connection.
     */
    public RedisAsyncConnection(RedisCodec<K, V> codec, RedisConnection<K, V> parent) {
        super(null, codec, 0, null);
        this.parent   = parent;
        this.pipeline = new ArrayList<Command<?>>();
    }

    /**
     * Wait for completion of all commands executed since the last flush
     * and return their outputs.
     *
     * @return The command outputs.
     */
    public List<Object> flush() {
        List<Object> list = new ArrayList<Object>(pipeline.size());
        for (Command<?> cmd : pipeline) {
            list.add(parent.getOutput(cmd));
        }
        pipeline.clear();
        return list;
    }

    @Override
    public String discard() {
        return parent.discard();
    }

    @Override
    public List<Object> exec() {
        return parent.exec();
    }

    @Override
    public String multi() {
        return parent.multi();
    }

    @Override
    public <T> Command<T> dispatch(CommandType type, CommandOutput<T> output, CommandArgs<K, V> args) {
        Command<T> cmd = parent.dispatch(type, output, args);
        pipeline.add(cmd);
        return cmd;
    }

    @Override
    public <T> T getOutput(Command<T> cmd) {
        return null;
    }
}
