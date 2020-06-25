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

import io.lettuce.core.*;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.async.RedisAdvancedClusterAsyncCommands;
import io.lettuce.core.cluster.api.async.RedisClusterAsyncCommands;
import io.lettuce.core.cluster.api.reactive.RedisAdvancedClusterReactiveCommands;
import io.lettuce.core.cluster.api.sync.NodeSelection;
import io.lettuce.core.cluster.api.sync.NodeSelectionCommands;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.protocol.*;
import io.netty.util.internal.logging.InternalLoggerFactory;

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

    private Partitions partitions;

    private char[] password;

    private boolean readOnly;

    private String clientName;

    protected final RedisCodec<K, V> codec;

    protected final RedisAdvancedClusterCommands<K, V> sync;

    protected final RedisAdvancedClusterAsyncCommandsImpl<K, V> async;

    protected final RedisAdvancedClusterReactiveCommandsImpl<K, V> reactive;

    private volatile RedisState state;

    /**
     * Initialize a new connection.
     *
     * @param writer the channel writer
     * @param codec Codec used to encode/decode keys and values.
     * @param timeout Maximum time to wait for a response.
     */
    public StatefulRedisClusterConnectionImpl(RedisChannelWriter writer, RedisCodec<K, V> codec, Duration timeout) {

        super(writer, timeout);
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

    RedisState getState() {
        return state;
    }

    void setState(RedisState state) {
        this.state = state;
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

    ClusterDistributionChannelWriter getClusterDistributionChannelWriter() {
        return (ClusterDistributionChannelWriter) super.getChannelWriter();
    }

    @Override
    public void activated() {

        super.activated();
        // do not block in here, since the channel flow will be interrupted.
        if (password != null) {
            AsyncCommand<K, V, String> command = async.authAsync(password);
            command.exceptionally(throwable -> {
                return logOnFailure(throwable, "AUTH failed: " + command.getError());
            });
        }

        if (clientName != null) {
            setClientName(clientName);
        }

        if (readOnly) {
            RedisFuture<String> command = async.readOnly();
            command.exceptionally(throwable -> {
                return logOnFailure(throwable, "READONLY failed: " + command.getError());
            });
        }
    }

    private String logOnFailure(Throwable throwable, String message) {
        if (throwable instanceof RedisCommandExecutionException) {
            InternalLoggerFactory.getInstance(getClass()).warn(message);
        }
        return "";
    }

    void setClientName(String clientName) {

        CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8).add(CommandKeyword.SETNAME).addValue(clientName);
        AsyncCommand<String, String, String> async = new AsyncCommand<>(
                new Command<>(CommandType.CLIENT, new StatusOutput<>(StringCodec.UTF8), args));
        this.clientName = clientName;

        dispatch((RedisCommand) async);
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
                    char[] password = CommandArgsAccessor.getFirstCharArray(command.getArgs());

                    if (password != null) {
                        this.password = password;
                    } else {

                        String stringPassword = CommandArgsAccessor.getFirstString(command.getArgs());
                        if (stringPassword != null) {
                            this.password = stringPassword.toCharArray();
                        }
                    }
                }
            });
        }

        if (local.getType().name().equals(READONLY.name())) {
            local = attachOnComplete(local, status -> {
                if (status.equals("OK")) {
                    this.readOnly = true;
                }
            });
        }

        if (local.getType().name().equals(READWRITE.name())) {
            local = attachOnComplete(local, status -> {
                if (status.equals("OK")) {
                    this.readOnly = false;
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
        this.partitions = partitions;
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

}
