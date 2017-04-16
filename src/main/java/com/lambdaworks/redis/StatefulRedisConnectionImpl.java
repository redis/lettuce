/*
 * Copyright 2011-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lambdaworks.redis;

import static com.lambdaworks.redis.protocol.CommandType.*;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.api.async.RedisAsyncCommands;
import com.lambdaworks.redis.api.rx.RedisReactiveCommands;
import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.cluster.api.sync.RedisClusterCommands;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.codec.StringCodec;
import com.lambdaworks.redis.output.MultiOutput;
import com.lambdaworks.redis.output.StatusOutput;
import com.lambdaworks.redis.protocol.*;

import io.netty.channel.ChannelHandler;

/**
 * A thread-safe connection to a Redis server. Multiple threads may share one {@link StatefulRedisConnectionImpl}
 *
 * A {@link ConnectionWatchdog} monitors each connection and reconnects automatically until {@link #close} is called. All
 * pending commands will be (re)sent after successful reconnection.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 */
@ChannelHandler.Sharable
public class StatefulRedisConnectionImpl<K, V> extends RedisChannelHandler<K, V> implements StatefulRedisConnection<K, V> {

    protected final RedisCodec<K, V> codec;
    protected final RedisCommands<K, V> sync;
    protected final RedisAsyncCommandsImpl<K, V> async;
    protected final RedisReactiveCommandsImpl<K, V> reactive;

    protected MultiOutput<K, V> multi;
    private char[] password;
    private int db;
    private boolean readOnly;
    private String clientName;

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
        this.async = newRedisAsyncCommandsImpl();
        this.sync = newRedisSyncCommandsImpl();
        this.reactive = newRedisReactiveCommandsImpl();
    }

    @Override
    public RedisAsyncCommands<K, V> async() {
        return async;
    }

    /**
     * Create a new instance of {@link RedisCommands}. Can be overriden to extend.
     *
     * @return a new instance
     */
    protected RedisCommands<K, V> newRedisSyncCommandsImpl() {
        return syncHandler(async(), RedisCommands.class, RedisClusterCommands.class);
    }

    /**
     * Create a new instance of {@link RedisAsyncCommandsImpl}. Can be overriden to extend.
     *
     * @return a new instance
     */
    protected RedisAsyncCommandsImpl<K, V> newRedisAsyncCommandsImpl() {
        return new RedisAsyncCommandsImpl<>(this, codec);
    }

    @Override
    public RedisReactiveCommands<K, V> reactive() {
        return reactive;
    }

    /**
     * Create a new instance of {@link RedisReactiveCommandsImpl}. Can be overriden to extend.
     *
     * @return a new instance
     */
    protected RedisReactiveCommandsImpl<K, V> newRedisReactiveCommandsImpl() {
        return new RedisReactiveCommandsImpl<>(this, codec);
    }

    @Override
    public RedisCommands<K, V> sync() {
        return sync;
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
            async.authAsync(new String(password));
        }

        if (db != 0) {
            async.selectAsync(db);
        }

        if (clientName != null) {
            setClientName(clientName);
        }

        if (readOnly) {
            async.readOnly();
        }
    }

    @Override
    public <T, C extends RedisCommand<K, V, T>> C dispatch(C cmd) {

        RedisCommand<K, V, T> local = cmd;

        if (local.getType().name().equals(AUTH.name())) {
            local = attachOnComplete(local, status -> {
                if ("OK".equals(status)) {
                    String password = CommandArgsAccessor.getFirstString(cmd.getArgs());
                    if (password != null) {
                        this.password = password.toCharArray();
                    }
                }
            });
        }

        if (local.getType().name().equals(SELECT.name())) {
            local = attachOnComplete(local, status -> {
                if ("OK".equals(status)) {
                    Long db = CommandArgsAccessor.getFirstInteger(cmd.getArgs());
                    if (db != null) {
                        this.db = db.intValue();
                    }
                }
            });
        }

        if (local.getType().name().equals(READONLY.name())) {
            local = attachOnComplete(local, status -> {
                if ("OK".equals(status)) {
                    this.readOnly = true;
                }
            });
        }

        if (local.getType().name().equals(READWRITE.name())) {
            local = attachOnComplete(local, status -> {
                if ("OK".equals(status)) {
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

        if (command instanceof CompleteableCommand) {
            CompleteableCommand<T> completeable = (CompleteableCommand<T>) command;
            completeable.onComplete(consumer);
        }
        return command;
    }

    public void setClientName(String clientName) {

        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8).add(CommandKeyword.SETNAME).addValue(clientName);
        AsyncCommand<String, String, String> async = new AsyncCommand<>(new Command<>(CommandType.CLIENT, new StatusOutput<>(
                StringCodec.UTF8), args));
        this.clientName = clientName;

        dispatch((RedisCommand) async);
    }
}
