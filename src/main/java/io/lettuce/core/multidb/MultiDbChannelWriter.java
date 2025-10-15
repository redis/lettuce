/*
 * Copyright 2020-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
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
package io.lettuce.core.multidb;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisChannelWriter;
import io.lettuce.core.RedisException;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.ConnectionFacade;
import io.lettuce.core.protocol.RedisCommand;
import io.lettuce.core.resource.ClientResources;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

class MultiDbChannelWriter implements RedisChannelWriter {

    private MultiDbConnectionProvider<?, ?> multiDbConnectionProvider;

    private final ClientResources clientResources;

    private boolean closed = false;

    MultiDbChannelWriter(MultiDbConnectionProvider<?, ?> multiDbConnectionProvider, ClientResources clientResources) {
        this.multiDbConnectionProvider = multiDbConnectionProvider;
        this.clientResources = clientResources;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <K, V, T> RedisCommand<K, V, T> write(RedisCommand<K, V, T> command) {

        LettuceAssert.notNull(command, "Command must not be null");

        if (closed) {
            throw new RedisException("Connection is closed");
        }

        CompletableFuture<StatefulRedisConnection<K, V>> future = (CompletableFuture) multiDbConnectionProvider
                .getConnectionAsync();

        if (isSuccessfullyCompleted(future)) {
            writeCommand(command, future.join(), null);
        } else {
            future.whenComplete((c, t) -> writeCommand(command, c, t));
        }

        return command;
    }

    @SuppressWarnings("unchecked")
    private static <K, V> void writeCommand(RedisCommand<K, V, ?> command, StatefulRedisConnection<K, V> connection,
            Throwable throwable) {

        if (throwable != null) {
            command.completeExceptionally(throwable);
            return;
        }

        try {
            connection.dispatch(command);
        } catch (Exception e) {
            command.completeExceptionally(e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <K, V> Collection<RedisCommand<K, V, ?>> write(Collection<? extends RedisCommand<K, V, ?>> commands) {

        LettuceAssert.notNull(commands, "Commands must not be null");

        if (closed) {
            throw new RedisException("Connection is closed");
        }

        CompletableFuture<StatefulRedisConnection<K, V>> future = (CompletableFuture) multiDbConnectionProvider
                .getConnectionAsync();

        if (isSuccessfullyCompleted(future)) {
            writeCommands(commands, future.join(), null);
        } else {
            future.whenComplete((c, t) -> writeCommands(commands, c, t));
        }

        return (Collection) commands;
    }

    @SuppressWarnings("unchecked")
    private static <K, V> void writeCommands(Collection<? extends RedisCommand<K, V, ?>> commands,
            StatefulRedisConnection<K, V> connection, Throwable throwable) {

        if (throwable != null) {
            commands.forEach(c -> c.completeExceptionally(throwable));
            return;
        }

        try {
            connection.dispatch(commands);
        } catch (Exception e) {
            commands.forEach(c -> c.completeExceptionally(e));
        }
    }

    @Override
    public void close() {
        closeAsync().join();
    }

    @Override
    public CompletableFuture<Void> closeAsync() {

        if (closed) {
            return CompletableFuture.completedFuture(null);
        }

        closed = true;

        CompletableFuture<Void> future = null;

        if (multiDbConnectionProvider != null) {
            future = multiDbConnectionProvider.closeAsync();
            multiDbConnectionProvider = null;
        }

        if (future == null) {
            future = CompletableFuture.completedFuture(null);
        }

        return future;
    }

    @Override
    public void setConnectionFacade(ConnectionFacade connection) {
    }

    @Override
    public ClientResources getClientResources() {
        return clientResources;
    }

    @Override
    public void setAutoFlushCommands(boolean autoFlush) {
        multiDbConnectionProvider.setAutoFlushCommands(autoFlush);
    }

    @Override
    public void flushCommands() {
        multiDbConnectionProvider.flushCommands();
    }

    private static boolean isSuccessfullyCompleted(CompletableFuture<?> connectFuture) {
        return connectFuture.isDone() && !connectFuture.isCompletedExceptionally();
    }

}
