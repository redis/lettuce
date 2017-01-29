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
import java.util.*;
import java.util.stream.IntStream;

import com.lambdaworks.redis.*;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.cluster.ClusterConnectionProvider.Intent;
import com.lambdaworks.redis.cluster.models.partitions.Partitions;
import com.lambdaworks.redis.internal.HostAndPort;
import com.lambdaworks.redis.internal.LettuceAssert;
import com.lambdaworks.redis.protocol.*;

import io.netty.util.concurrent.EventExecutorGroup;

/**
 * Channel writer for cluster operation. This writer looks up the right partition by hash/slot for the operation.
 *
 * @author Mark Paluch
 * @since 3.0
 */
class ClusterDistributionChannelWriter implements RedisChannelWriter {

    private final RedisChannelWriter defaultWriter;
    private final ClusterEventListener clusterEventListener;
    private final EventExecutorGroup eventExecutors;
    private final int executionLimit;

    private ClusterConnectionProvider clusterConnectionProvider;
    private boolean closed = false;
    private volatile Partitions partitions;

    ClusterDistributionChannelWriter(ClientOptions clientOptions, RedisChannelWriter defaultWriter,
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

    @SuppressWarnings("unchecked")
    @Override
    public <K, V, T> RedisCommand<K, V, T> write(RedisCommand<K, V, T> command) {

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
                                .getConnection(Intent.WRITE, target.getHostText(), target.getPort());

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

        RedisChannelWriter channelWriter = null;

        ByteBuffer firstEncodedKey = args != null ? args.getFirstEncodedKey() : null;
        if (firstEncodedKey != null) {
            int hash = getSlot(args.getFirstEncodedKey());
            Intent intent = getIntent(command.getType());

            RedisChannelHandler<K, V> connection = (RedisChannelHandler<K, V>) clusterConnectionProvider.getConnection(intent,
                    hash);

            channelWriter = connection.getChannelWriter();
        }

        if (channelWriter instanceof ClusterDistributionChannelWriter) {
            ClusterDistributionChannelWriter writer = (ClusterDistributionChannelWriter) channelWriter;
            channelWriter = writer.defaultWriter;
        }

        if (command.getOutput() != null) {
            commandToSend.getOutput().setError((String) null);
        }

        if (channelWriter != null && channelWriter != this && channelWriter != defaultWriter) {
            return channelWriter.write(commandToSend);
        }

        defaultWriter.write(commandToSend);

        return command;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <K, V> Collection<RedisCommand<K, V, ?>> write(Collection<? extends RedisCommand<K, V, ?>> commands) {

        LettuceAssert.notNull(commands, "Commands must not be null");

        if (closed) {
            throw new RedisException("Connection is closed");
        }

        List<ClusterCommand<K, V, ?>> clusterCommands = new ArrayList<>(commands.size());
        List<ClusterCommand<K, V, ?>> defaultCommands = new ArrayList<>(commands.size());
        Map<SlotIntent, List<ClusterCommand<K, V, ?>>> partitions = new HashMap<>();

        // TODO: Retain order or retain Intent preference?
        // Currently: Retain order
        Intent intent = getIntent(commands);

        for (RedisCommand<K, V, ?> cmd : commands) {

            if (cmd instanceof ClusterCommand) {
                clusterCommands.add((ClusterCommand) cmd);
                continue;
            }

            CommandArgs<K, V> args = cmd.getArgs();
            ByteBuffer firstEncodedKey = args != null ? args.getFirstEncodedKey() : null;

            if (firstEncodedKey == null) {
                defaultCommands.add(new ClusterCommand<>(cmd, this, executionLimit));
                continue;
            }

            int hash = getSlot(args.getFirstEncodedKey());

            List<ClusterCommand<K, V, ?>> commandPartition = partitions.computeIfAbsent(SlotIntent.of(intent, hash),
                    slotIntent -> new ArrayList<>());

            commandPartition.add(new ClusterCommand<>(cmd, this, executionLimit));
        }

        for (Map.Entry<SlotIntent, List<ClusterCommand<K, V, ?>>> entry : partitions.entrySet()) {

            SlotIntent slotIntent = entry.getKey();
            RedisChannelHandler<K, V> connection = (RedisChannelHandler<K, V>) clusterConnectionProvider.getConnection(
                    slotIntent.intent, slotIntent.slotHash);

            RedisChannelWriter channelWriter = connection.getChannelWriter();
            if (channelWriter instanceof ClusterDistributionChannelWriter) {
                ClusterDistributionChannelWriter writer = (ClusterDistributionChannelWriter) channelWriter;
                channelWriter = writer.defaultWriter;
            }

            if (channelWriter != null && channelWriter != this && channelWriter != defaultWriter) {
                channelWriter.write(entry.getValue());
            }
        }

        clusterCommands.forEach(this::write);
        defaultCommands.forEach(defaultWriter::write);

        return (Collection) commands;
    }

    /**
     * Optimization: Determine command intents and optimize for bulk execution preferring one node.
     * <p>
     * If there is only one intent, then we take the intent derived from the commands. If there is more than one intent, then
     * use {@link Intent#WRITE}.
     *
     * @param commands {@link Collection} of {@link RedisCommand commands}.
     * @return the intent.
     */
    static Intent getIntent(Collection<? extends RedisCommand<?, ?, ?>> commands) {

        boolean w = false;
        boolean r = false;
        Intent singleIntent = Intent.WRITE;

        for (RedisCommand<?, ?, ?> command : commands) {

            if (command instanceof ClusterCommand) {
                continue;
            }

            singleIntent = getIntent(command.getType());
            if (singleIntent == Intent.READ) {
                r = true;
            }

            if (singleIntent == Intent.WRITE) {
                w = true;
            }

            if (r && w) {
                return Intent.WRITE;
            }
        }

        return singleIntent;
    }

    private static Intent getIntent(ProtocolKeyword type) {
        return ReadOnlyCommands.isReadOnlyCommand(type) ? Intent.READ : Intent.WRITE;
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
    public void setConnectionFacade(ConnectionFacade redisChannelHandler) {
        defaultWriter.setConnectionFacade(redisChannelHandler);
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

    static class SlotIntent {

        final int slotHash;
        final Intent intent;
        private final static SlotIntent[] READ;
        private final static SlotIntent[] WRITE;

        static {
            READ = new SlotIntent[SlotHash.SLOT_COUNT];
            WRITE = new SlotIntent[SlotHash.SLOT_COUNT];

            IntStream.range(0, SlotHash.SLOT_COUNT).forEach(i -> {

                READ[i] = new SlotIntent(i, Intent.READ);
                WRITE[i] = new SlotIntent(i, Intent.WRITE);
            });

        }

        private SlotIntent(int slotHash, Intent intent) {
            this.slotHash = slotHash;
            this.intent = intent;
        }

        public static SlotIntent of(Intent intent, int slot) {

            if (intent == Intent.READ) {
                return READ[slot];
            }

            return WRITE[slot];
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof SlotIntent))
                return false;

            SlotIntent that = (SlotIntent) o;

            if (slotHash != that.slotHash)
                return false;
            return intent == that.intent;
        }

        @Override
        public int hashCode() {
            int result = slotHash;
            result = 31 * result + intent.hashCode();
            return result;
        }
    }
}
