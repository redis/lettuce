/*
 * Copyright 2011-2017 the original author or authors.
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
package com.lambdaworks.redis.sentinel;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import com.lambdaworks.redis.RedisChannelHandler;
import com.lambdaworks.redis.RedisChannelWriter;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.codec.StringCodec;
import com.lambdaworks.redis.output.StatusOutput;
import com.lambdaworks.redis.protocol.*;
import com.lambdaworks.redis.sentinel.api.StatefulRedisSentinelConnection;
import com.lambdaworks.redis.sentinel.api.async.RedisSentinelAsyncCommands;
import com.lambdaworks.redis.sentinel.api.reactive.RedisSentinelReactiveCommands;
import com.lambdaworks.redis.sentinel.api.sync.RedisSentinelCommands;

/**
 * @author Mark Paluch
 */
public class StatefulRedisSentinelConnectionImpl<K, V> extends RedisChannelHandler<K, V> implements
        StatefulRedisSentinelConnection<K, V> {

    protected final RedisCodec<K, V> codec;
    protected final RedisSentinelCommands<K, V> sync;
    protected final RedisSentinelAsyncCommands<K, V> async;
    protected final RedisSentinelReactiveCommands<K, V> reactive;

    private String clientName;

    public StatefulRedisSentinelConnectionImpl(RedisChannelWriter writer, RedisCodec<K, V> codec, long timeout,
            TimeUnit unit) {
        super(writer, timeout, unit);

        this.codec = codec;
        this.async = new RedisSentinelAsyncCommandsImpl<>(this, codec);
        this.sync = syncHandler(async, RedisSentinelCommands.class);
        this.reactive = new RedisSentinelReactiveCommandsImpl<>(this, codec);
    }

    @Override
    public <T> RedisCommand<K, V, T> dispatch(RedisCommand<K, V, T> command) {
        return super.dispatch(command);
    }

    @Override
    public Collection<RedisCommand<K, V, ?>> dispatch(Collection<? extends RedisCommand<K, V, ?>> commands) {
        return super.dispatch(commands);
    }

    @Override
    public RedisSentinelCommands<K, V> sync() {
        return sync;
    }

    @Override
    public RedisSentinelAsyncCommands<K, V> async() {
        return async;
    }

    @Override
    public RedisSentinelReactiveCommands<K, V> reactive() {
        return reactive;
    }

    @Override
    public void activated() {

        super.activated();

        if (clientName != null) {
            setClientName(clientName);
        }
    }

    public void setClientName(String clientName) {

        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8).add(CommandKeyword.SETNAME).addValue(clientName);
        AsyncCommand<String, String, String> async = new AsyncCommand<>(
                new Command<>(CommandType.CLIENT, new StatusOutput<>(StringCodec.UTF8), args));
        this.clientName = clientName;

        dispatch((RedisCommand) async);
    }
}
