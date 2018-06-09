/*
 * Copyright 2017-2018 the original author or authors.
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
package io.lettuce.core;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import io.lettuce.core.protocol.ConnectionFacade;
import io.lettuce.core.protocol.EmptyClientResources;
import io.lettuce.core.protocol.RedisCommand;
import io.lettuce.core.resource.ClientResources;

/**
 * @author Mark Paluch
 */
public class EmptyRedisChannelWriter implements RedisChannelWriter {

    public static final EmptyRedisChannelWriter INSTANCE = new EmptyRedisChannelWriter();
    private static final CompletableFuture CLOSE_FUTURE = CompletableFuture.completedFuture(null);

    @Override
    public <K, V, T> RedisCommand<K, V, T> write(RedisCommand<K, V, T> command) {
        return null;
    }

    @Override
    public <K, V> Collection<RedisCommand<K, V, ?>> write(Collection<? extends RedisCommand<K, V, ?>> redisCommands) {
        return (Collection) redisCommands;
    }

    @Override
    public void close() {

    }

    @Override
    public CompletableFuture<Void> closeAsync() {
        return CLOSE_FUTURE;
    }

    @Override
    public void reset() {

    }

    @Override
    public void setConnectionFacade(ConnectionFacade connection) {
    }

    @Override
    public ClientResources getClientResources() {
        return EmptyClientResources.INSTANCE;
    }

    @Override
    public void setAutoFlushCommands(boolean autoFlush) {
    }

    @Override
    public void flushCommands() {
    }
}
