/*
 * Copyright 2016-2017 the original author or authors.
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

import static com.lambdaworks.redis.cluster.NodeSelectionInvocationHandler.ExecutionModel.ASYNC;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.lambdaworks.redis.GeoArgs;
import com.lambdaworks.redis.GeoWithin;
import com.lambdaworks.redis.RedisFuture;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.cluster.api.NodeSelectionSupport;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;
import com.lambdaworks.redis.cluster.pubsub.StatefulRedisClusterPubSubConnection;
import com.lambdaworks.redis.cluster.pubsub.api.async.NodeSelectionPubSubAsyncCommands;
import com.lambdaworks.redis.cluster.pubsub.api.async.PubSubAsyncNodeSelection;
import com.lambdaworks.redis.cluster.pubsub.api.async.RedisClusterPubSubAsyncCommands;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.protocol.CommandType;
import com.lambdaworks.redis.pubsub.RedisPubSubAsyncCommandsImpl;
import com.lambdaworks.redis.pubsub.StatefulRedisPubSubConnection;
import com.lambdaworks.redis.pubsub.api.async.RedisPubSubAsyncCommands;

/**
 * @author Mark Paluch
 */
class RedisClusterPubSubAsyncCommandsImpl<K, V> extends RedisPubSubAsyncCommandsImpl<K, V> implements
        RedisClusterPubSubAsyncCommands<K, V> {

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

    private static class StaticPubSubAsyncNodeSelection<K, V> extends
            AbstractNodeSelection<RedisPubSubAsyncCommands<K, V>, NodeSelectionPubSubAsyncCommands<K, V>, K, V> implements
            PubSubAsyncNodeSelection<K, V> {

        private final List<RedisClusterNode> redisClusterNodes;
        private final ClusterDistributionChannelWriter<K, V> writer;

        @SuppressWarnings("unchecked")
        public StaticPubSubAsyncNodeSelection(StatefulRedisClusterPubSubConnection<K, V> globalConnection,
                Predicate<RedisClusterNode> selector) {

            this.redisClusterNodes = globalConnection.getPartitions().getPartitions().stream().filter(selector)
                    .collect(Collectors.toList());
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

            return async.getConnectionAsync(ClusterConnectionProvider.Intent.WRITE, uri.getHost(), uri.getPort()).thenApply(
                    it -> (StatefulRedisPubSubConnection<K, V>) it);
        }
    }
}
