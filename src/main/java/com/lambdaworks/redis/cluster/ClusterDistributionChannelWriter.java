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
package com.lambdaworks.redis.cluster;

import static com.lambdaworks.redis.cluster.SlotHash.getSlot;

import com.lambdaworks.redis.*;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.cluster.models.partitions.Partitions;
import com.lambdaworks.redis.internal.HostAndPort;
import com.lambdaworks.redis.internal.LettuceAssert;
import com.lambdaworks.redis.protocol.CommandArgs;
import com.lambdaworks.redis.protocol.CommandKeyword;
import com.lambdaworks.redis.protocol.ProtocolKeyword;
import com.lambdaworks.redis.protocol.RedisCommand;

import io.netty.util.concurrent.EventExecutorGroup;

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
    private final EventExecutorGroup eventExecutors;
    private final int executionLimit;

    private ClusterConnectionProvider clusterConnectionProvider;
    private boolean closed = false;

    ClusterDistributionChannelWriter(ClientOptions clientOptions, RedisChannelWriter<K, V> defaultWriter,
            ClusterEventListener clusterEventListener, EventExecutorGroup eventExecutors) {

        if (clientOptions instanceof ClusterClientOptions) {
            this.executionLimit = ((ClusterClientOptions) clientOptions).getMaxRedirects();
        } else {
            this.executionLimit = 5;
        }

        this.defaultWriter = defaultWriter;
        this.clusterEventListener = clusterEventListener;
        this.eventExecutors = eventExecutors;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T, C extends RedisCommand<K, V, T>> C write(C command) {

        LettuceAssert.notNull(command, "Command must not be null");

        if (closed) {
            throw new RedisException("Connection is closed");
        }

        RedisCommand<K, V, T> commandToSend = command;
        CommandArgs<K, V> args = command.getArgs();

        if (!(command instanceof ClusterCommand)) {
            commandToSend = new ClusterCommand<>(command, this, executionLimit);
        }

        if (commandToSend instanceof ClusterCommand && !commandToSend.isDone()) {

            ClusterCommand<K, V, T> clusterCommand = (ClusterCommand<K, V, T>) commandToSend;
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

                commandToSend.getOutput().setError((String) null);

                eventExecutors.submit(() -> {

                    try {
                        RedisChannelHandler<K, V> connection = (RedisChannelHandler<K, V>) clusterConnectionProvider
                                .getConnection(ClusterConnectionProvider.Intent.WRITE, target.getHostText(), target.getPort());

                        if (asking) {
                            // set asking bit
                        StatefulRedisConnection<K, V> statefulRedisConnection = (StatefulRedisConnection<K, V>) connection;
                        statefulRedisConnection.async().asking();
                    }

                    connection.getChannelWriter().write(command);
                } catch (Exception e) {
                    command.completeExceptionally(e);
                }
            })  ;

                return command;
            }
        }

        RedisChannelWriter<K, V> channelWriter = null;

        if (args != null && args.getFirstEncodedKey() != null) {
            int hash = getSlot(args.getFirstEncodedKey());
            ClusterConnectionProvider.Intent intent = getIntent(command.getType());

            RedisChannelHandler<K, V> connection = (RedisChannelHandler<K, V>) clusterConnectionProvider.getConnection(intent,
                    hash);

            channelWriter = connection.getChannelWriter();
        }

        if (channelWriter instanceof ClusterDistributionChannelWriter) {
            ClusterDistributionChannelWriter<K, V> writer = (ClusterDistributionChannelWriter<K, V>) channelWriter;
            channelWriter = writer.defaultWriter;
        }

        if (command.getOutput() != null) {
            commandToSend.getOutput().setError((String) null);
        }

        if (channelWriter != null && channelWriter != this && channelWriter != defaultWriter) {
            return channelWriter.write((C) commandToSend);
        }

        defaultWriter.write((C) commandToSend);

        return command;
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
    }

    public void setPartitions(Partitions partitions) {
        if (clusterConnectionProvider != null) {
            clusterConnectionProvider.setPartitions(partitions);
        }
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
