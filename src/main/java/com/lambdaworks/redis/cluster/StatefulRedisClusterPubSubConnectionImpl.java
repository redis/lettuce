/*
 * Copyright 2016 the original author or authors.
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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import com.lambdaworks.redis.AbstractRedisClient;
import com.lambdaworks.redis.RedisChannelWriter;
import com.lambdaworks.redis.RedisException;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.cluster.models.partitions.Partitions;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;
import com.lambdaworks.redis.cluster.pubsub.RedisClusterPubSubAdapter;
import com.lambdaworks.redis.cluster.pubsub.RedisClusterPubSubListener;
import com.lambdaworks.redis.cluster.pubsub.StatefulRedisClusterPubSubConnection;
import com.lambdaworks.redis.cluster.pubsub.api.async.RedisClusterPubSubAsyncCommands;
import com.lambdaworks.redis.cluster.pubsub.api.rx.RedisClusterPubSubReactiveCommands;
import com.lambdaworks.redis.cluster.pubsub.api.sync.NodeSelectionPubSubCommands;
import com.lambdaworks.redis.cluster.pubsub.api.sync.PubSubNodeSelection;
import com.lambdaworks.redis.cluster.pubsub.api.sync.RedisClusterPubSubCommands;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.models.command.CommandDetailParser;
import com.lambdaworks.redis.pubsub.*;
import com.lambdaworks.redis.pubsub.api.async.RedisPubSubAsyncCommands;
import com.lambdaworks.redis.pubsub.api.sync.RedisPubSubCommands;

import io.netty.channel.ChannelHandler;

/**
 * @author Mark Paluch
 */
@ChannelHandler.Sharable
class StatefulRedisClusterPubSubConnectionImpl<K, V> extends StatefulRedisPubSubConnectionImpl<K, V> implements
        StatefulRedisClusterPubSubConnection<K, V> {

    private final List<RedisClusterPubSubListener<K, V>> clusterListeners = new CopyOnWriteArrayList<>();
    private final NotifyingMessageListener multicast = new NotifyingMessageListener();
    private final UpstreamMessageListener upstream = new UpstreamMessageListener();

    private volatile Partitions partitions;
    private volatile boolean nodeMessagePropagation = false;
    private volatile String nodeId;
    private RedisState state;

    /**
     * Initialize a new connection.
     *
     * @param writer the channel writer
     * @param codec Codec used to encode/decode keys and values.
     * @param timeout Maximum time to wait for a response.
     * @param unit Unit of time for the timeout.
     */
    public StatefulRedisClusterPubSubConnectionImpl(RedisChannelWriter<K, V> writer, RedisCodec<K, V> codec, long timeout,
            TimeUnit unit) {

        super(writer, codec, timeout, unit);
    }

    @Override
    public RedisClusterPubSubAsyncCommands<K, V> async() {
        return (RedisClusterPubSubAsyncCommands<K, V>) super.async();
    }

    @Override
    protected RedisPubSubAsyncCommandsImpl<K, V> newRedisAsyncCommandsImpl() {
        return new RedisClusterPubSubAsyncCommandsImpl<>(this, codec);
    }

    @Override
    public RedisClusterPubSubCommands<K, V> sync() {
        return (RedisClusterPubSubCommands<K, V>) super.sync();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected RedisPubSubCommands<K, V> newRedisSyncCommandsImpl() {

        return (RedisPubSubCommands) Proxy.newProxyInstance(AbstractRedisClient.class.getClassLoader(), new Class<?>[] {
                RedisClusterPubSubCommands.class, RedisPubSubCommands.class }, syncInvocationHandler());
    }

    private InvocationHandler syncInvocationHandler() {
        return new ClusterFutureSyncInvocationHandler<>(this, RedisPubSubAsyncCommands.class, PubSubNodeSelection.class,
                NodeSelectionPubSubCommands.class, async());
    }

    @Override
    public RedisClusterPubSubReactiveCommands<K, V> reactive() {
        return (RedisClusterPubSubReactiveCommands<K, V>) super.reactive();
    }

    @Override
    protected RedisPubSubReactiveCommandsImpl<K, V> newRedisReactiveCommandsImpl() {
        return new RedisClusterPubSubReactiveCommandsImpl<>(this, codec);
    }

    void inspectRedisState() {
        this.state = new RedisState(CommandDetailParser.parse(sync().command()));
    }

    RedisState getState() {
        return state;
    }

    @Override
    public void activated() {
        super.activated();
        async().clusterMyId().thenAccept(nodeId -> this.nodeId = nodeId);
    }

    @Override
    public StatefulRedisPubSubConnection<K, V> getConnection(String nodeId) {

        RedisURI redisURI = lookup(nodeId);

        if (redisURI == null) {
            throw new RedisException("NodeId " + nodeId + " does not belong to the cluster");
        }

        return (StatefulRedisPubSubConnection<K, V>) getClusterDistributionChannelWriter().getClusterConnectionProvider()
                .getConnection(ClusterConnectionProvider.Intent.WRITE, nodeId);
    }

    @Override
    public StatefulRedisPubSubConnection<K, V> getConnection(String host, int port) {

        return (StatefulRedisPubSubConnection<K, V>) getClusterDistributionChannelWriter().getClusterConnectionProvider()
                .getConnection(ClusterConnectionProvider.Intent.WRITE, host, port);
    }

    public void setPartitions(Partitions partitions) {
        this.partitions = partitions;
        getClusterDistributionChannelWriter().setPartitions(partitions);
    }

    public Partitions getPartitions() {
        return partitions;
    }

    @Override
    public void setNodeMessagePropagation(boolean enabled) {
        this.nodeMessagePropagation = enabled;
    }

    /**
     * Add a new listener.
     *
     * @param listener Listener.
     */
    @Override
    public void addListener(RedisClusterPubSubListener<K, V> listener) {
        clusterListeners.add(listener);
    }

    /**
     * Remove an existing listener.
     *
     * @param listener Listener.
     */
    @Override
    public void removeListener(RedisClusterPubSubListener<K, V> listener) {
        clusterListeners.remove(listener);
    }

    protected void notifyListeners(PubSubOutput<K, V, V> output) {
        // update listeners

        switch (output.type()) {
            case message:
                multicast.message(getNode(), output.channel(), output.get());
                break;
            case pmessage:
                multicast.message(getNode(), output.pattern(), output.channel(), output.get());
                break;
            case psubscribe:
                multicast.psubscribed(getNode(), output.pattern(), output.count());
                break;
            case punsubscribe:
                multicast.punsubscribed(getNode(), output.pattern(), output.count());
                break;
            case subscribe:
                multicast.subscribed(getNode(), output.channel(), output.count());
                break;
            case unsubscribe:
                multicast.unsubscribed(getNode(), output.channel(), output.count());
                break;
            default:
                throw new UnsupportedOperationException("Operation " + output.type() + " not supported");
        }
    }

    protected RedisClusterPubSubListener<K, V> getUpstreamListener() {
        return upstream;
    }

    protected ClusterDistributionChannelWriter<K, V> getClusterDistributionChannelWriter() {
        return (ClusterDistributionChannelWriter<K, V>) super.getChannelWriter();
    }

    private RedisClusterNode getNode() {

        if (nodeId == null) {
            return null;
        }

        return partitions.getPartitionByNodeId(nodeId);
    }

    private RedisURI lookup(String nodeId) {

        for (RedisClusterNode partition : partitions) {
            if (partition.getNodeId().equals(nodeId)) {
                return partition.getUri();
            }
        }
        return null;
    }

    private class UpstreamMessageListener extends NotifyingMessageListener {

        @Override
        public void message(RedisClusterNode node, K channel, V message) {

            if (nodeMessagePropagation) {
                super.message(node, channel, message);
            }
        }

        @Override
        public void message(RedisClusterNode node, K pattern, K channel, V message) {

            if (nodeMessagePropagation) {
                super.message(node, pattern, channel, message);
            }
        }

        @Override
        public void subscribed(RedisClusterNode node, K channel, long count) {

            if (nodeMessagePropagation) {
                super.subscribed(node, channel, count);
            }
        }

        @Override
        public void psubscribed(RedisClusterNode node, K pattern, long count) {

            if (nodeMessagePropagation) {
                super.psubscribed(node, pattern, count);
            }
        }

        @Override
        public void unsubscribed(RedisClusterNode node, K channel, long count) {

            if (nodeMessagePropagation) {
                super.unsubscribed(node, channel, count);
            }
        }

        @Override
        public void punsubscribed(RedisClusterNode node, K pattern, long count) {

            if (nodeMessagePropagation) {
                super.punsubscribed(node, pattern, count);
            }
        }
    }

    private class NotifyingMessageListener extends RedisClusterPubSubAdapter<K, V> {

        @Override
        public void message(RedisClusterNode node, K channel, V message) {

            listeners.forEach(listener -> listener.message(channel, message));
            clusterListeners.forEach(listener -> listener.message(node, channel, message));
        }

        @Override
        public void message(RedisClusterNode node, K pattern, K channel, V message) {

            listeners.forEach(listener -> listener.message(pattern, channel, message));
            clusterListeners.forEach(listener -> listener.message(node, pattern, channel, message));
        }

        @Override
        public void subscribed(RedisClusterNode node, K channel, long count) {

            listeners.forEach(listener -> listener.subscribed(channel, count));
            clusterListeners.forEach(listener -> listener.subscribed(node, channel, count));
        }

        @Override
        public void psubscribed(RedisClusterNode node, K pattern, long count) {

            listeners.forEach(listener -> listener.psubscribed(pattern, count));
            clusterListeners.forEach(listener -> listener.psubscribed(node, pattern, count));
        }

        @Override
        public void unsubscribed(RedisClusterNode node, K channel, long count) {

            listeners.forEach(listener -> listener.unsubscribed(channel, count));
            clusterListeners.forEach(listener -> listener.unsubscribed(node, channel, count));
        }

        @Override
        public void punsubscribed(RedisClusterNode node, K pattern, long count) {

            listeners.forEach(listener -> listener.punsubscribed(pattern, count));
            clusterListeners.forEach(listener -> listener.punsubscribed(node, pattern, count));
        }
    }
}
