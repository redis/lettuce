// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis;

import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.protocol.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
    private List<Command<K, V, ?>> pipeline;

    /**
     * Initialize a new connection.
     *
     * @param codec     Codec used to encode/decode keys and values.
     * @param parent    Parent connection.
     */
    public RedisAsyncConnection(RedisCodec<K, V> codec, RedisConnection<K, V> parent) {
        super(null, codec, parent.timeout, parent.unit);
        this.parent   = parent;
        this.pipeline = new ArrayList<Command<K, V, ?>>();
    }

    /**
     * Discard output of all commands executed since the last clear
     * or {@link #flush}.
     */
    public void clear() {
        pipeline.clear();
    }

    /**
     * Wait for completion of all commands executed since the last {@link #clear}
     * or flush and return their outputs, or until the connection's configured
     * {@link #setTimeout timeout} expires.
     *
     * @return The command outputs.
     */
    public List<Object> flush() {
        return flush(timeout, unit);
    }

    /**
     * Wait for completion of all commands executed since the last {@link #clear}
     * or flush and return their outputs.
     *
     * @param timeout Maximum time to wait for all commands to complete.
     * @param unit    Unit of time for the timeout.
     *
     * @return The command outputs.
     */
    public List<Object> flush(long timeout, TimeUnit unit) {
        List<Object> list = new ArrayList<Object>(pipeline.size());

        long nanos = unit.toNanos(timeout);
        long time  = System.nanoTime();

        for (Command<K, V, ?> cmd : pipeline) {
            list.add(parent.getOutput(cmd, nanos, TimeUnit.NANOSECONDS));
            long now = System.nanoTime();
            nanos -= now - time;
            time   = now;
            if (nanos <= 0) throw new RedisException("Command timed out");
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
    public <T> Command<K, V, T> dispatch(CommandType type, CommandOutput<K, V, T> output, CommandArgs<K, V> args) {
        Command<K, V, T> cmd = parent.dispatch(type, output, args);
        pipeline.add(cmd);
        return cmd;
    }

    @Override
    public <T> T getOutput(Command<K, V, T> cmd, long timeout, TimeUnit unit) {
        return null;
    }
}
