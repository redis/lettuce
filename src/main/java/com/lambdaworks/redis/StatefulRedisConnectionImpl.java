// Copyright (C) 2011 - Will Glozer.  All rights reserved.

package com.lambdaworks.redis;

import static com.lambdaworks.redis.protocol.CommandType.AUTH;
import static com.lambdaworks.redis.protocol.CommandType.DISCARD;
import static com.lambdaworks.redis.protocol.CommandType.EXEC;
import static com.lambdaworks.redis.protocol.CommandType.MULTI;
import static com.lambdaworks.redis.protocol.CommandType.READONLY;
import static com.lambdaworks.redis.protocol.CommandType.READWRITE;
import static com.lambdaworks.redis.protocol.CommandType.SELECT;

import java.lang.reflect.Proxy;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.output.MultiOutput;
import com.lambdaworks.redis.protocol.AsyncCommand;
import com.lambdaworks.redis.protocol.ConnectionWatchdog;
import com.lambdaworks.redis.protocol.RedisCommand;
import com.lambdaworks.redis.protocol.TransactionalCommand;
import io.netty.channel.ChannelHandler;

/**
 * An thread-safe connection to a redis server. Multiple threads may share one {@link StatefulRedisConnectionImpl}
 *
 * A {@link ConnectionWatchdog} monitors each connection and reconnects automatically until {@link #close} is called. All
 * pending commands will be (re)sent after successful reconnection.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
@ChannelHandler.Sharable
public class StatefulRedisConnectionImpl<K, V> extends RedisChannelHandler<K, V> implements StatefulRedisConnection<K, V> {

    protected RedisCodec<K, V> codec;
    protected RedisConnection<K, V> sync;
    protected RedisAsyncConnectionImpl<K, V> async;

    protected MultiOutput<K, V> multi;
    private char[] password;
    private int db;
    private boolean readOnly;

    /**
     * Initialize a new connection.
     *
     * @param writer the channel writer
     * @param codec Codec used to encode/decode keys and values.
     * @param timeout Maximum time to wait for a response.
     * @param unit Unit of time for the timeout.
     */
    public StatefulRedisConnectionImpl(RedisChannelWriter<K, V> writer, RedisCodec<K, V> codec, long timeout, TimeUnit unit) {
        super(writer, timeout, unit);
        this.codec = codec;

    }

    public RedisAsyncConnection<K, V> async() {
        return getAsyncConnection();
    }

    protected RedisAsyncConnectionImpl<K, V> getAsyncConnection() {
        if (async == null) {
            async = newRedisAsyncConnectionImpl();
        }

        return async;
    }

    protected RedisAsyncConnectionImpl<K, V> newRedisAsyncConnectionImpl() {
        return new RedisAsyncConnectionImpl<>(this, codec);
    }

    public RedisConnection<K, V> sync() {
        if (sync == null) {
            sync = (RedisConnection) syncHandler(RedisConnection.class, RedisClusterConnection.class);
        }
        return sync;
    }

    protected Object syncHandler(Class... interfaces) {
        FutureSyncInvocationHandler<K, V> h = new FutureSyncInvocationHandler<>(this, async());
        return Proxy.newProxyInstance(AbstractRedisClient.class.getClassLoader(), interfaces, h);
    }

    public static String string(double n) {
        if (Double.isInfinite(n)) {
            return (n > 0) ? "+inf" : "-inf";
        }
        return Double.toString(n);
    }

    @Override
    public boolean isMulti() {
        return multi != null;
    }

    @Override
    public void activated() {

        super.activated();
        // do not block in here, since the channel flow will be interrupted.
        if (password != null) {
            getAsyncConnection().authAsync(new String(password));
        }

        if (db != 0) {
            getAsyncConnection().selectAsync(db);
        }

        if (readOnly) {
            getAsyncConnection().readOnly();
        }
    }

    @Override
    public <T, C extends RedisCommand<K, V, T>> C dispatch(C cmd) {

        RedisCommand<K, V, T> local = cmd;

        if (local.getType().name().equals(AUTH.name())) {
            local = attachOnComplete(local, status -> {
                if (status.equals("OK")) {
                    this.password = cmd.getArgs().getStrings().get(0).toCharArray();
                }
            });
        }

        if (local.getType().name().equals(SELECT.name())) {
            local = attachOnComplete(local, status -> {
                if (status.equals("OK")) {
                    this.db = cmd.getArgs().getIntegers().get(0).intValue();
                }
            });
        }

        if (local.getType().name().equals(READONLY.name())) {
            local = attachOnComplete(local, status -> {
                if (status.equals("OK")) {
                    this.readOnly = true;
                }
            });
        }

        if (local.getType().name().equals(READWRITE.name())) {
            local = attachOnComplete(local, status -> {
                if (status.equals("OK")) {
                    this.readOnly = false;
                }
            });
        }

        if (local.getType().name().equals(DISCARD.name())) {
            if (multi != null) {
                multi.cancel();
                multi = null;
            }
        }

        if (local.getType().name().equals(EXEC.name())) {
            MultiOutput<K, V> multiOutput = this.multi;
            this.multi = null;
            if (multiOutput == null) {
                multiOutput = new MultiOutput<>(codec);
            }
            local.setOutput((MultiOutput) multiOutput);
        }

        if (multi != null) {
            local = new TransactionalCommand<>(local);
            multi.add(local);
        }

        try {
            return (C) super.dispatch(local);
        } finally {
            if (cmd.getType().name().equals(MULTI.name())) {
                multi = (multi == null ? new MultiOutput<>(codec) : multi);
            }
        }
    }

    private <T> RedisCommand<K, V, T> attachOnComplete(RedisCommand<K, V, T> command, Consumer<T> consumer) {

        if (command instanceof AsyncCommand) {
            AsyncCommand<K, V, T> async = (AsyncCommand<K, V, T>) command;
            async.thenAccept(consumer);
        }
        return command;
    }

}
