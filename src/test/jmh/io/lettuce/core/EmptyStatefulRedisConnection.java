/*
 * Copyright 2017-2020 the original author or authors.
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
package io.lettuce.core;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.push.PushListener;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.protocol.ConnectionFacade;
import io.lettuce.core.protocol.RedisCommand;

/**
 * @author Mark Paluch
 */
public class EmptyStatefulRedisConnection extends RedisChannelHandler implements StatefulRedisConnection, ConnectionFacade {

    public static final EmptyStatefulRedisConnection INSTANCE = new EmptyStatefulRedisConnection(
            EmptyRedisChannelWriter.INSTANCE);

    public EmptyStatefulRedisConnection(RedisChannelWriter writer) {
        super(writer, Duration.ZERO);
    }

    @Override
    public boolean isMulti() {
        return false;
    }

    @Override
    public RedisCommands sync() {
        return null;
    }

    @Override
    public RedisAsyncCommands async() {
        return null;
    }

    @Override
    public RedisReactiveCommands reactive() {
        return null;
    }

    @Override
    public void close() {
    }

    @Override
    public boolean isOpen() {
        return false;
    }

    @Override
    public ClientOptions getOptions() {
        return null;
    }

    @Override
    public void activated() {
    }

    @Override
    public void deactivated() {
    }

    @Override
    public void reset() {
    }

    @Override
    public void setAutoFlushCommands(boolean autoFlush) {
    }

    @Override
    public void flushCommands() {
    }

    @Override
    public void addListener(PushListener listener) {
    }

    @Override
    public void removeListener(PushListener listener) {
    }

    @Override
    public RedisCommand dispatch(RedisCommand command) {
        return null;
    }

    @Override
    public Collection<? extends RedisCommand> dispatch(Collection commands) {
        return commands;
    }
}
