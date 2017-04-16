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
package com.lambdaworks.redis.cluster;

import static com.lambdaworks.redis.cluster.SlotHash.getSlot;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

import com.lambdaworks.redis.*;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.cluster.models.partitions.Partitions;
import com.lambdaworks.redis.internal.HostAndPort;
import com.lambdaworks.redis.internal.LettuceAssert;
import com.lambdaworks.redis.protocol.*;

/**
 * Channel writer for cluster operation. This writer looks up the right partition by hash/slot for the operation.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 3.0
 */
class ClusterDistributionChannelWriter<K, V> implements RedisChannelWriter<K, V> {

    private final RedisChannelWriter<K, V> defaultWriter;
    private final ClusterEventListener clusterEventListener;
    private final int executionLimit;

    private ClusterConnectionProvider clusterConnectionProvider;
    private AsyncClusterConnectionProvider asyncClusterConnectionProvider;
    private boolean closed = false;
    private volatile Partitions partitions;

    ClusterDistributionChannelWriter(ClientOptions clientOptions, RedisChannelWriter<K, V> defaultWriter,
            ClusterEventListener clusterEventListener) {

        if (clientOptions instanceof ClusterClientOptions) {
            this.executionLimit = ((ClusterClientOptions) clientOptions).getMaxRedirects();
        } else {
            this.executionLimit = 5;
        }

        this.defaultWriter = defaultWriter;
        this.clusterEventListener = clusterEventListener;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T, C extends RedisCommand<K, V, T>> C write(C command) {

        LettuceAssert.notNull(command, "Command must not be null");

        if (closed) {
            throw new RedisException("Connection is closed");
        }

        if (command instanceof ClusterCommand && !command.isDone()) {

            ClusterCommand<K, V, T> clusterCommand = (ClusterCommand<K, V, T>) command;
            if (clusterCommand.isMoved() || clusterCommand.isAsk()) {

                HostAndPort target;
                boolean asking;
                if (clusterCommand.isMoved()) {
                    target = getMoveTarget(clusterCommand.getError());
                    clusterEventListener.onMovedRedirection();
                    asking = false;
                } else {
                    target = getAskTarget(clusterCommand.getError());
                    asking = true;
                    clusterEventListener.onAskRedirection();
                }

                command.getOutput().setError((String) null);

                CompletableFuture<StatefulRedisConnection<K, V>> connectFuture = asyncClusterConnectionProvider
                        .getConnectionAsync(ClusterConnectionProvider.Intent.WRITE, target.getHostText(), target.getPort());

                if (isSuccessfullyCompleted(connectFuture)) {
                    writeCommand(command, asking, connectFuture.join(), null);
                } else {
                    connectFuture.whenComplete((connection, throwable) -> writeCommand(command, asking, connection, throwable));
                }

                return command;
            }
        }

        ClusterCommand<K, V, ?> commandToSend = getCommandToSend(command);
        CommandArgs<K, V> args = command.getArgs();

        if (args != null) {

            ByteBuffer encodedKey = CommandArgsAccessor.encodeFirstKey(args);
            if (encodedKey != null) {

                int hash = getSlot(encodedKey);
                ClusterConnectionProvider.Intent intent = getIntent(command.getType());

                CompletableFuture<StatefulRedisConnection<K, V>> connectFuture = ((AsyncClusterConnectionProvider) clusterConnectionProvider)
                        .getConnectionAsync(intent, hash);

                if (isSuccessfullyCompleted(connectFuture)) {
                    writeCommand(commandToSend, false, connectFuture.join(), null);
                } else {
                    connectFuture.whenComplete((connection, throwable) -> writeCommand(commandToSend, false, connection,
                            throwable));
                }

                return (C) commandToSend;
            }
        }

        writeCommand(commandToSend, defaultWriter);

        return (C) commandToSend;
    }

    private static boolean isSuccessfullyCompleted(CompletableFuture<?> connectFuture) {
        return connectFuture.isDone() && !connectFuture.isCompletedExceptionally();
    }

    @SuppressWarnings("unchecked")
    private ClusterCommand<K, V, ?> getCommandToSend(RedisCommand<K, V, ?> command) {

        if (command instanceof ClusterCommand) {
            return (ClusterCommand<K, V, ?>) command;
        }

        return new ClusterCommand<>(command, this, executionLimit);
    }

    @SuppressWarnings("unchecked")
    private static <K, V> void writeCommand(RedisCommand<K, V, ?> command, boolean asking,
            StatefulRedisConnection<K, V> connection, Throwable throwable) {

        if (throwable != null) {
            command.completeExceptionally(throwable);
            return;
        }

        try {
            if (asking) { // set asking bit
                connection.async().asking();
            }

            writeCommand(command, ((RedisChannelHandler<K, V>) connection).getChannelWriter());
        } catch (Exception e) {
            command.completeExceptionally(e);
        }
    }

    private static <K, V> void writeCommand(RedisCommand<K, V, ?> command, RedisChannelWriter<K, V> writer) {

        try {
            getWriterToUse(writer).write(command);
        } catch (Exception e) {
            command.completeExceptionally(e);
        }
    }

    private static <K, V> RedisChannelWriter<K, V> getWriterToUse(RedisChannelWriter<K, V> writer) {
        RedisChannelWriter<K, V> writerToUse = writer;

        if (writer instanceof ClusterDistributionChannelWriter) {
            writerToUse = ((ClusterDistributionChannelWriter<K, V>) writer).defaultWriter;
        }
        return writerToUse;
    }

    private ClusterConnectionProvider.Intent getIntent(ProtocolKeyword type) {

        if (ReadOnlyCommands.isReadOnlyCommand(type)) {
            return ClusterConnectionProvider.Intent.READ;
        }

        return ClusterConnectionProvider.Intent.WRITE;
    }

    static HostAndPort getMoveTarget(String errorMessage) {

        LettuceAssert.notEmpty(errorMessage, "ErrorMessage must not be empty");
        LettuceAssert.isTrue(errorMessage.startsWith(CommandKeyword.MOVED.name()), "ErrorMessage must start with "
                + CommandKeyword.MOVED);

        String[] movedMessageParts = errorMessage.split(" ");
        LettuceAssert.isTrue(movedMessageParts.length >= 3, "ErrorMessage must consist of 3 tokens (" + errorMessage + ")");

        return HostAndPort.parseCompat(movedMessageParts[2]);
    }

    static HostAndPort getAskTarget(String errorMessage) {

        LettuceAssert.notEmpty(errorMessage, "ErrorMessage must not be empty");
        LettuceAssert.isTrue(errorMessage.startsWith(CommandKeyword.ASK.name()), "ErrorMessage must start with "
                + CommandKeyword.ASK);

        String[] movedMessageParts = errorMessage.split(" ");
        LettuceAssert.isTrue(movedMessageParts.length >= 3, "ErrorMessage must consist of 3 tokens (" + errorMessage + ")");

        return HostAndPort.parseCompat(movedMessageParts[2]);
    }

    @Override
    public void close() {

        if (closed) {
            return;
        }

        closed = true;

        if (defaultWriter != null) {
            defaultWriter.close();
        }

        if (clusterConnectionProvider != null) {
            clusterConnectionProvider.close();
            clusterConnectionProvider = null;
        }
    }

    @Override
    public void setRedisChannelHandler(RedisChannelHandler<K, V> redisChannelHandler) {
        defaultWriter.setRedisChannelHandler(redisChannelHandler);
    }

    @Override
    public void setAutoFlushCommands(boolean autoFlush) {
        getClusterConnectionProvider().setAutoFlushCommands(autoFlush);
    }

    @Override
    public void flushCommands() {
        getClusterConnectionProvider().flushCommands();
    }

    public ClusterConnectionProvider getClusterConnectionProvider() {
        return clusterConnectionProvider;
    }

    @Override
    public void reset() {
        defaultWriter.reset();
        clusterConnectionProvider.reset();
    }

    public void setClusterConnectionProvider(ClusterConnectionProvider clusterConnectionProvider) {
        this.clusterConnectionProvider = clusterConnectionProvider;
        this.asyncClusterConnectionProvider = (AsyncClusterConnectionProvider) clusterConnectionProvider;
    }

    public void setPartitions(Partitions partitions) {

        this.partitions = partitions;

        if (clusterConnectionProvider != null) {
            clusterConnectionProvider.setPartitions(partitions);
        }
    }

    public Partitions getPartitions() {
        return partitions;
    }

    /**
     * Set from which nodes data is read. The setting is used as default for read operations on this connection. See the
     * documentation for {@link ReadFrom} for more information.
     *
     * @param readFrom the read from setting, must not be {@literal null}
     */
    public void setReadFrom(ReadFrom readFrom) {
        clusterConnectionProvider.setReadFrom(readFrom);
    }

    /**
     * Gets the {@link ReadFrom} setting for this connection. Defaults to {@link ReadFrom#MASTER} if not set.
     *
     * @return the read from setting
     */
    public ReadFrom getReadFrom() {
        return clusterConnectionProvider.getReadFrom();
    }
}
