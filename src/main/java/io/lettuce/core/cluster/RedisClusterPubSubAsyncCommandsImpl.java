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
package io.lettuce.core.cluster;

import static io.lettuce.core.cluster.NodeSelectionInvocationHandler.ExecutionModel.ASYNC;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.api.NodeSelectionSupport;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.cluster.pubsub.StatefulRedisClusterPubSubConnection;
import io.lettuce.core.cluster.pubsub.api.async.NodeSelectionPubSubAsyncCommands;
import io.lettuce.core.cluster.pubsub.api.async.PubSubAsyncNodeSelection;
import io.lettuce.core.cluster.pubsub.api.async.RedisClusterPubSubAsyncCommands;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.pubsub.RedisPubSubAsyncCommandsImpl;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands;

/**
 * @author Mark Paluch
 */
class RedisClusterPubSubAsyncCommandsImpl<K, V> extends RedisPubSubAsyncCommandsImpl<K, V>
        implements RedisClusterPubSubAsyncCommands<K, V> {

    /**
     * Initialize a new connection.
     *
     * @param connection the connection .
     * @param codec Codec used to encode/decode keys and values.
     */
    public RedisClusterPubSubAsyncCommandsImpl(StatefulRedisClusterPubSubConnection<K, V> connection, RedisCodec<K, V> codec) {
        super(connection, codec);
    }

    @Override
    public StatefulRedisClusterPubSubConnection<K, V> getStatefulConnection() {
        return (StatefulRedisClusterPubSubConnection<K, V>) super.getStatefulConnection();
    }

    @SuppressWarnings("unchecked")
    @Override
    public PubSubAsyncNodeSelection<K, V> nodes(Predicate<RedisClusterNode> predicate) {

        PubSubAsyncNodeSelection<K, V> selection = new StaticPubSubAsyncNodeSelection<>(getStatefulConnection(), predicate);

        NodeSelectionInvocationHandler h = new NodeSelectionInvocationHandler((AbstractNodeSelection<?, ?, ?, ?>) selection,
                RedisPubSubAsyncCommands.class, ASYNC);
        return (PubSubAsyncNodeSelection<K, V>) Proxy.newProxyInstance(NodeSelectionSupport.class.getClassLoader(),
                new Class<?>[] { NodeSelectionPubSubAsyncCommands.class, PubSubAsyncNodeSelection.class }, h);
    }

    private static class StaticPubSubAsyncNodeSelection<K, V>
            extends AbstractNodeSelection<RedisPubSubAsyncCommands<K, V>, NodeSelectionPubSubAsyncCommands<K, V>, K, V>
            implements PubSubAsyncNodeSelection<K, V> {

        private final List<RedisClusterNode> redisClusterNodes;
        private final ClusterDistributionChannelWriter writer;

        @SuppressWarnings("unchecked")
        public StaticPubSubAsyncNodeSelection(StatefulRedisClusterPubSubConnection<K, V> globalConnection,
                Predicate<RedisClusterNode> selector) {

            this.redisClusterNodes = globalConnection.getPartitions().getPartitions().stream().filter(selector)
                    .collect(Collectors.toList());
            writer = ((StatefulRedisClusterPubSubConnectionImpl) globalConnection).getClusterDistributionChannelWriter();
        }

        @Override
        protected RedisPubSubAsyncCommands<K, V> getApi(RedisClusterNode redisClusterNode) {
            return getConnection(redisClusterNode).async();
        }

        protected List<RedisClusterNode> nodes() {
            return redisClusterNodes;
        }

        @SuppressWarnings("unchecked")
        protected StatefulRedisPubSubConnection<K, V> getConnection(RedisClusterNode redisClusterNode) {
            RedisURI uri = redisClusterNode.getUri();
            return (StatefulRedisPubSubConnection<K, V>) writer.getClusterConnectionProvider()
                    .getConnection(ClusterConnectionProvider.Intent.WRITE, uri.getHost(), uri.getPort());
        }
    }
}
