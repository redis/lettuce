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
package io.lettuce.core.cluster;

import static io.lettuce.core.protocol.CommandType.AUTH;
import static io.lettuce.core.protocol.CommandType.READONLY;
import static io.lettuce.core.protocol.CommandType.READWRITE;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import io.lettuce.core.*;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.async.RedisAdvancedClusterAsyncCommands;
import io.lettuce.core.cluster.api.async.RedisClusterAsyncCommands;
import io.lettuce.core.cluster.api.push.RedisClusterPushListener;
import io.lettuce.core.cluster.api.reactive.RedisAdvancedClusterReactiveCommands;
import io.lettuce.core.cluster.api.sync.NodeSelection;
import io.lettuce.core.cluster.api.sync.NodeSelectionCommands;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.CommandArgsAccessor;
import io.lettuce.core.protocol.CompleteableCommand;
import io.lettuce.core.protocol.ConnectionWatchdog;
import io.lettuce.core.protocol.RedisCommand;

/**
 * A thread-safe connection to a Redis Cluster. Multiple threads may share one {@link StatefulRedisClusterConnectionImpl}
 *
 * A {@link ConnectionWatchdog} monitors each connection and reconnects automatically until {@link #close} is called. All
 * pending commands will be (re)sent after successful reconnection.
 *
 * @author Mark Paluch
 * @since 4.0
 */
public class StatefulRedisClusterConnectionImpl<K, V> extends RedisChannelHandler<K, V>
        implements StatefulRedisClusterConnection<K, V> {

    private final ClusterPushHandler pushHandler;

    protected final RedisCodec<K, V> codec;

    protected final RedisAdvancedClusterCommands<K, V> sync;

    protected final RedisAdvancedClusterAsyncCommandsImpl<K, V> async;

    protected final RedisAdvancedClusterReactiveCommandsImpl<K, V> reactive;

    private final ClusterConnectionState connectionState = new ClusterConnectionState();

    private volatile Partitions partitions;

    private volatile CommandSet commandSet;

    /**
     * Initialize a new connection.
     *
     * @param writer the channel writer
     * @param pushHandler the Cluster push handler
     * @param codec Codec used to encode/decode keys and values.
     * @param timeout Maximum time to wait for a response.
     */
    public StatefulRedisClusterConnectionImpl(RedisChannelWriter writer, ClusterPushHandler pushHandler, RedisCodec<K, V> codec,
            Duration timeout) {

        super(writer, timeout);
        this.pushHandler = pushHandler;
        this.codec = codec;

        this.async = new RedisAdvancedClusterAsyncCommandsImpl<>(this, codec);
        this.sync = (RedisAdvancedClusterCommands) Proxy.newProxyInstance(AbstractRedisClient.class.getClassLoader(),
                new Class<?>[] { RedisAdvancedClusterCommands.class }, syncInvocationHandler());
        this.reactive = new RedisAdvancedClusterReactiveCommandsImpl<>(this, codec);
    }

    @Override
    public RedisAdvancedClusterCommands<K, V> sync() {
        return sync;
    }

    protected InvocationHandler syncInvocationHandler() {
        return new ClusterFutureSyncInvocationHandler<>(this, RedisClusterAsyncCommands.class, NodeSelection.class,
                NodeSelectionCommands.class, async());
    }

    @Override
    public RedisAdvancedClusterAsyncCommands<K, V> async() {
        return async;
    }

    @Override
    public RedisAdvancedClusterReactiveCommands<K, V> reactive() {
        return reactive;
    }

    @Override
    public void addListener(RedisClusterPushListener listener) {
        pushHandler.addListener(listener);
    }

    @Override
    public void removeListener(RedisClusterPushListener listener) {
        pushHandler.removeListener(listener);
    }

    CommandSet getCommandSet() {
        return commandSet;
    }

    void setCommandSet(CommandSet commandSet) {
        this.commandSet = commandSet;
    }

    private RedisURI lookup(String nodeId) {

        for (RedisClusterNode partition : partitions) {
            if (partition.getNodeId().equals(nodeId)) {
                return partition.getUri();
            }
        }
        return null;
    }

    @Override
    public StatefulRedisConnection<K, V> getConnection(String nodeId) {

        RedisURI redisURI = lookup(nodeId);

        if (redisURI == null) {
            throw new RedisException("NodeId " + nodeId + " does not belong to the cluster");
        }

        return getClusterDistributionChannelWriter().getClusterConnectionProvider()
                .getConnection(ClusterConnectionProvider.Intent.WRITE, nodeId);
    }

    @Override
    public CompletableFuture<StatefulRedisConnection<K, V>> getConnectionAsync(String nodeId) {

        RedisURI redisURI = lookup(nodeId);

        if (redisURI == null) {
            throw new RedisException("NodeId " + nodeId + " does not belong to the cluster");
        }

        AsyncClusterConnectionProvider provider = (AsyncClusterConnectionProvider) getClusterDistributionChannelWriter()
                .getClusterConnectionProvider();

        return provider.getConnectionAsync(ClusterConnectionProvider.Intent.WRITE, nodeId);
    }

    @Override
    public StatefulRedisConnection<K, V> getConnection(String host, int port) {

        return getClusterDistributionChannelWriter().getClusterConnectionProvider()
                .getConnection(ClusterConnectionProvider.Intent.WRITE, host, port);
    }

    @Override
    public CompletableFuture<StatefulRedisConnection<K, V>> getConnectionAsync(String host, int port) {

        AsyncClusterConnectionProvider provider = (AsyncClusterConnectionProvider) getClusterDistributionChannelWriter()
                .getClusterConnectionProvider();

        return provider.getConnectionAsync(ClusterConnectionProvider.Intent.WRITE, host, port);
    }

    @Override
    public void activated() {
        super.activated();

        async.clusterMyId().thenAccept(connectionState::setNodeId);
    }

    ClusterDistributionChannelWriter getClusterDistributionChannelWriter() {
        return (ClusterDistributionChannelWriter) super.getChannelWriter();
    }

    @Override
    public <T> RedisCommand<K, V, T> dispatch(RedisCommand<K, V, T> command) {
        return super.dispatch(preProcessCommand(command));
    }

    @Override
    public Collection<RedisCommand<K, V, ?>> dispatch(Collection<? extends RedisCommand<K, V, ?>> commands) {

        List<RedisCommand<K, V, ?>> commandsToSend = new ArrayList<>(commands.size());
        for (RedisCommand<K, V, ?> command : commands) {
            commandsToSend.add(preProcessCommand(command));
        }

        return super.dispatch(commandsToSend);
    }

    private <T> RedisCommand<K, V, T> preProcessCommand(RedisCommand<K, V, T> command) {

        RedisCommand<K, V, T> local = command;

        if (local.getType().name().equals(AUTH.name())) {
            local = attachOnComplete(local, status -> {
                if (status.equals("OK")) {
                    List<char[]> args = CommandArgsAccessor.getCharArrayArguments(command.getArgs());

                    if (!args.isEmpty()) {
                        this.connectionState.setUserNamePassword(args);
                    } else {

                        List<String> stringArgs = CommandArgsAccessor.getStringArguments(command.getArgs());
                        this.connectionState
                                .setUserNamePassword(stringArgs.stream().map(String::toCharArray).collect(Collectors.toList()));
                    }
                }
            });
        }

        if (local.getType().name().equals(READONLY.name())) {
            local = attachOnComplete(local, status -> {
                if (status.equals("OK")) {
                    this.connectionState.setReadOnly(true);
                }
            });
        }

        if (local.getType().name().equals(READWRITE.name())) {
            local = attachOnComplete(local, status -> {
                if (status.equals("OK")) {
                    this.connectionState.setReadOnly(false);
                }
            });
        }
        return local;
    }

    private <T> RedisCommand<K, V, T> attachOnComplete(RedisCommand<K, V, T> command, Consumer<T> consumer) {

        if (command instanceof CompleteableCommand) {
            CompleteableCommand<T> completeable = (CompleteableCommand<T>) command;
            completeable.onComplete(consumer);
        }
        return command;
    }

    public void setPartitions(Partitions partitions) {

        LettuceAssert.notNull(partitions, "Partitions must not be null");

        this.partitions = partitions;

        String nodeId = connectionState.getNodeId();
        if (nodeId != null && expireStaleConnections()) {

            if (partitions.getPartitionByNodeId(nodeId) == null) {
                getClusterDistributionChannelWriter().disconnectDefaultEndpoint();
            }
        }

        getClusterDistributionChannelWriter().setPartitions(partitions);
    }

    public Partitions getPartitions() {
        return partitions;
    }

    @Override
    public void setReadFrom(ReadFrom readFrom) {
        LettuceAssert.notNull(readFrom, "ReadFrom must not be null");
        getClusterDistributionChannelWriter().setReadFrom(readFrom);
    }

    @Override
    public ReadFrom getReadFrom() {
        return getClusterDistributionChannelWriter().getReadFrom();
    }

    ConnectionState getConnectionState() {
        return connectionState;
    }

    static class ClusterConnectionState extends ConnectionState {

        private volatile String nodeId;

        @Override
        protected void setUserNamePassword(List<char[]> args) {
            super.setUserNamePassword(args);
        }

        @Override
        protected void setDb(int db) {
            super.setDb(db);
        }

        @Override
        protected void setReadOnly(boolean readOnly) {
            super.setReadOnly(readOnly);
        }

        public String getNodeId() {
            return nodeId;
        }

        public void setNodeId(String nodeId) {
            this.nodeId = nodeId;
        }

    }

    private boolean expireStaleConnections() {

        ClusterClientOptions options = getClusterClientOptions();
        return options == null || options.isCloseStaleConnections();
    }

    private ClusterClientOptions getClusterClientOptions() {

        ClientOptions options = getOptions();
        return options instanceof ClusterClientOptions ? (ClusterClientOptions) options : null;
    }

}
