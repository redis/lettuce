/*
 * Copyright 2020-2022 the original author or authors.
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
package io.lettuce.core.masterreplica;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import io.lettuce.core.ReadFrom;
import io.lettuce.core.RedisChannelWriter;
import io.lettuce.core.RedisException;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.ConnectionFacade;
import io.lettuce.core.protocol.ConnectionIntent;
import io.lettuce.core.protocol.ProtocolKeyword;
import io.lettuce.core.protocol.RedisCommand;
import io.lettuce.core.resource.ClientResources;

/**
 * Channel writer/dispatcher that dispatches commands based on the ConnectionIntent to different connections.
 *
 * @author Mark Paluch
 */
class MasterReplicaChannelWriter implements RedisChannelWriter {

    private MasterReplicaConnectionProvider<?, ?> masterReplicaConnectionProvider;

    private final ClientResources clientResources;

    private boolean closed = false;

    private boolean inTransaction;

    MasterReplicaChannelWriter(MasterReplicaConnectionProvider<?, ?> masterReplicaConnectionProvider,
            ClientResources clientResources) {
        this.masterReplicaConnectionProvider = masterReplicaConnectionProvider;
        this.clientResources = clientResources;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <K, V, T> RedisCommand<K, V, T> write(RedisCommand<K, V, T> command) {

        LettuceAssert.notNull(command, "Command must not be null");

        if (closed) {
            throw new RedisException("Connection is closed");
        }

        if (isStartTransaction(command.getType())) {
            inTransaction = true;
        }

        ConnectionIntent connectionIntent = inTransaction ? ConnectionIntent.WRITE : getIntent(command.getType());
        CompletableFuture<StatefulRedisConnection<K, V>> future = (CompletableFuture) masterReplicaConnectionProvider
                .getConnectionAsync(connectionIntent);

        if (isEndTransaction(command.getType())) {
            inTransaction = false;
        }

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

        for (RedisCommand<K, V, ?> command : commands) {
            if (isStartTransaction(command.getType())) {
                inTransaction = true;
                break;
            }
        }

        // TODO: Retain order or retain ConnectionIntent preference?
        // Currently: Retain order
        ConnectionIntent connectionIntent = inTransaction ? ConnectionIntent.WRITE : getIntent(commands);

        CompletableFuture<StatefulRedisConnection<K, V>> future = (CompletableFuture) masterReplicaConnectionProvider
                .getConnectionAsync(connectionIntent);

        for (RedisCommand<K, V, ?> command : commands) {
            if (isEndTransaction(command.getType())) {
                inTransaction = false;
                break;
            }
        }

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

    /**
     * Optimization: Determine command intents and optimize for bulk execution preferring one node.
     * <p>
     * If there is only one ConnectionIntent, then we take the ConnectionIntent derived from the commands. If there is more than
     * one ConnectionIntent, then use {@link ConnectionIntent#WRITE}.
     *
     * @param commands {@link Collection} of {@link RedisCommand commands}.
     * @return the ConnectionIntent.
     */
    static ConnectionIntent getIntent(Collection<? extends RedisCommand<?, ?, ?>> commands) {

        boolean w = false;
        boolean r = false;
        ConnectionIntent singleIntent = ConnectionIntent.WRITE;

        for (RedisCommand<?, ?, ?> command : commands) {

            singleIntent = getIntent(command.getType());
            if (singleIntent == ConnectionIntent.READ) {
                r = true;
            }

            if (singleIntent == ConnectionIntent.WRITE) {
                w = true;
            }

            if (r && w) {
                return ConnectionIntent.WRITE;
            }
        }

        return singleIntent;
    }

    private static ConnectionIntent getIntent(ProtocolKeyword type) {
        return ReadOnlyCommands.isReadOnlyCommand(type) ? ConnectionIntent.READ : ConnectionIntent.WRITE;
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

        if (masterReplicaConnectionProvider != null) {
            future = masterReplicaConnectionProvider.closeAsync();
            masterReplicaConnectionProvider = null;
        }

        if (future == null) {
            future = CompletableFuture.completedFuture(null);
        }

        return future;
    }

    MasterReplicaConnectionProvider<?, ?> getUpstreamReplicaConnectionProvider() {
        return masterReplicaConnectionProvider;
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
        masterReplicaConnectionProvider.setAutoFlushCommands(autoFlush);
    }

    @Override
    public void flushCommands() {
        masterReplicaConnectionProvider.flushCommands();
    }

    @Override
    public void reset() {
        masterReplicaConnectionProvider.reset();
    }

    /**
     * Set from which nodes data is read. The setting is used as default for read operations on this connection. See the
     * documentation for {@link ReadFrom} for more information.
     *
     * @param readFrom the read from setting, must not be {@code null}
     */
    public void setReadFrom(ReadFrom readFrom) {
        masterReplicaConnectionProvider.setReadFrom(readFrom);
    }

    /**
     * Gets the {@link ReadFrom} setting for this connection. Defaults to {@link ReadFrom#UPSTREAM} if not set.
     *
     * @return the read from setting
     */
    public ReadFrom getReadFrom() {
        return masterReplicaConnectionProvider.getReadFrom();
    }

    private static boolean isSuccessfullyCompleted(CompletableFuture<?> connectFuture) {
        return connectFuture.isDone() && !connectFuture.isCompletedExceptionally();
    }

    private static boolean isStartTransaction(ProtocolKeyword command) {
        return command.name().equals("MULTI");
    }

    private boolean isEndTransaction(ProtocolKeyword command) {
        return command.name().equals("EXEC") || command.name().equals("DISCARD");
    }

}
