/*
 * Copyright 2016-2020 the original author or authors.
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

import static io.lettuce.core.cluster.NodeSelectionInvocationHandler.ExecutionModel.ASYNC;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import io.lettuce.core.GeoArgs;
import io.lettuce.core.GeoWithin;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.api.NodeSelectionSupport;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.cluster.pubsub.StatefulRedisClusterPubSubConnection;
import io.lettuce.core.cluster.pubsub.api.async.NodeSelectionPubSubAsyncCommands;
import io.lettuce.core.cluster.pubsub.api.async.PubSubAsyncNodeSelection;
import io.lettuce.core.cluster.pubsub.api.async.RedisClusterPubSubAsyncCommands;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.pubsub.RedisPubSubAsyncCommandsImpl;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands;

/**
 * An asynchronous and thread-safe API for a Redis pub/sub connection.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 5.0
 */
public class RedisClusterPubSubAsyncCommandsImpl<K, V> extends RedisPubSubAsyncCommandsImpl<K, V>
        implements RedisClusterPubSubAsyncCommands<K, V> {

    /**
     * Initialize a new connection.
     *
     * @param connection the connection .
     * @param codec Codec used to encode/decode keys and values.
     */
    public RedisClusterPubSubAsyncCommandsImpl(StatefulRedisPubSubConnection<K, V> connection, RedisCodec<K, V> codec) {
        super(connection, codec);
    }

    @Override
    public RedisFuture<Set<V>> georadius(K key, double longitude, double latitude, double distance, GeoArgs.Unit unit) {

        if (getStatefulConnection().getState().hasCommand(CommandType.GEORADIUS_RO)) {
            return super.georadius_ro(key, longitude, latitude, distance, unit);
        }

        return super.georadius(key, longitude, latitude, distance, unit);
    }

    @Override
    public RedisFuture<List<GeoWithin<V>>> georadius(K key, double longitude, double latitude, double distance,
            GeoArgs.Unit unit, GeoArgs geoArgs) {

        if (getStatefulConnection().getState().hasCommand(CommandType.GEORADIUS_RO)) {
            return super.georadius_ro(key, longitude, latitude, distance, unit, geoArgs);
        }

        return super.georadius(key, longitude, latitude, distance, unit, geoArgs);
    }

    @Override
    public RedisFuture<Set<V>> georadiusbymember(K key, V member, double distance, GeoArgs.Unit unit) {

        if (getStatefulConnection().getState().hasCommand(CommandType.GEORADIUSBYMEMBER_RO)) {
            return super.georadiusbymember_ro(key, member, distance, unit);
        }

        return super.georadiusbymember(key, member, distance, unit);
    }

    @Override
    public RedisFuture<List<GeoWithin<V>>> georadiusbymember(K key, V member, double distance, GeoArgs.Unit unit,
            GeoArgs geoArgs) {

        if (getStatefulConnection().getState().hasCommand(CommandType.GEORADIUSBYMEMBER_RO)) {
            return super.georadiusbymember_ro(key, member, distance, unit, geoArgs);
        }

        return super.georadiusbymember(key, member, distance, unit, geoArgs);
    }

    @Override
    public StatefulRedisClusterPubSubConnectionImpl<K, V> getStatefulConnection() {
        return (StatefulRedisClusterPubSubConnectionImpl<K, V>) super.getStatefulConnection();
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

            this.redisClusterNodes = globalConnection.getPartitions().stream().filter(selector).collect(Collectors.toList());
            writer = ((StatefulRedisClusterPubSubConnectionImpl) globalConnection).getClusterDistributionChannelWriter();
        }

        @Override
        protected CompletableFuture<RedisPubSubAsyncCommands<K, V>> getApi(RedisClusterNode redisClusterNode) {
            return getConnection(redisClusterNode).thenApply(StatefulRedisPubSubConnection::async);
        }

        protected List<RedisClusterNode> nodes() {
            return redisClusterNodes;
        }

        @SuppressWarnings("unchecked")
        protected CompletableFuture<StatefulRedisPubSubConnection<K, V>> getConnection(RedisClusterNode redisClusterNode) {

            RedisURI uri = redisClusterNode.getUri();
            AsyncClusterConnectionProvider async = (AsyncClusterConnectionProvider) writer.getClusterConnectionProvider();

            return async.getConnectionAsync(ClusterConnectionProvider.Intent.WRITE, uri.getHost(), uri.getPort())
                    .thenApply(it -> (StatefulRedisPubSubConnection<K, V>) it);
        }

    }

}
