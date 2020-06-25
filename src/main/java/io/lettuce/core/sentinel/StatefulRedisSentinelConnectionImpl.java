/*
 * Copyright 2011-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.sentinel;

import java.time.Duration;
import java.util.Collection;

import io.lettuce.core.ConnectionState;
import io.lettuce.core.RedisChannelHandler;
import io.lettuce.core.RedisChannelWriter;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.protocol.*;
import io.lettuce.core.sentinel.api.StatefulRedisSentinelConnection;
import io.lettuce.core.sentinel.api.async.RedisSentinelAsyncCommands;
import io.lettuce.core.sentinel.api.reactive.RedisSentinelReactiveCommands;
import io.lettuce.core.sentinel.api.sync.RedisSentinelCommands;

/**
 * @author Mark Paluch
 */
public class StatefulRedisSentinelConnectionImpl<K, V> extends RedisChannelHandler<K, V>
        implements StatefulRedisSentinelConnection<K, V> {

    protected final RedisCodec<K, V> codec;

    protected final RedisSentinelCommands<K, V> sync;

    protected final RedisSentinelAsyncCommands<K, V> async;

    protected final RedisSentinelReactiveCommands<K, V> reactive;

    private final SentinelConnectionState connectionState = new SentinelConnectionState();

    public StatefulRedisSentinelConnectionImpl(RedisChannelWriter writer, RedisCodec<K, V> codec, Duration timeout) {

        super(writer, timeout);

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

    /**
     * @param clientName
     * @deprecated since 6.0, use {@link RedisSentinelAsyncCommands#clientSetname(Object)}.
     */
    @Deprecated
    public void setClientName(String clientName) {

        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8).add(CommandKeyword.SETNAME).addValue(clientName);
        AsyncCommand<String, String, String> async = new AsyncCommand<>(
                new Command<>(CommandType.CLIENT, new StatusOutput<>(StringCodec.UTF8), args));
        connectionState.setClientName(clientName);

        dispatch((RedisCommand) async);
    }

    public ConnectionState getConnectionState() {
        return connectionState;
    }

    static class SentinelConnectionState extends ConnectionState {

        @Override
        protected void setClientName(String clientName) {
            super.setClientName(clientName);
        }

    }

}
