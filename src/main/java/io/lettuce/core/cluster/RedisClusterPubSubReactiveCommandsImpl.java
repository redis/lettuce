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

import static io.lettuce.core.cluster.NodeSelectionInvocationHandler.ExecutionModel.REACTIVE;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import reactor.core.publisher.Flux;
import io.lettuce.core.GeoArgs;
import io.lettuce.core.GeoWithin;
import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.api.NodeSelectionSupport;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.cluster.pubsub.StatefulRedisClusterPubSubConnection;
import io.lettuce.core.cluster.pubsub.api.reactive.NodeSelectionPubSubReactiveCommands;
import io.lettuce.core.cluster.pubsub.api.reactive.PubSubReactiveNodeSelection;
import io.lettuce.core.cluster.pubsub.api.reactive.RedisClusterPubSubReactiveCommands;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.pubsub.RedisPubSubReactiveCommandsImpl;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.reactive.RedisPubSubReactiveCommands;

/**
 * A reactive and thread-safe API for a Redis pub/sub connection.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 5.0
 */
public class RedisClusterPubSubReactiveCommandsImpl<K, V> extends RedisPubSubReactiveCommandsImpl<K, V>
        implements RedisClusterPubSubReactiveCommands<K, V> {

    /**
     * Initialize a new connection.
     *
     * @param connection the connection.
     * @param codec Codec used to encode/decode keys and values.
     */
    public RedisClusterPubSubReactiveCommandsImpl(StatefulRedisPubSubConnection<K, V> connection, RedisCodec<K, V> codec) {
        super(connection, codec);
    }

    @Override
    public Flux<V> georadius(K key, double longitude, double latitude, double distance, GeoArgs.Unit unit) {

        if (getStatefulConnection().getState().hasCommand(CommandType.GEORADIUS_RO)) {
            return super.georadius_ro(key, longitude, latitude, distance, unit);
        }

        return super.georadius(key, longitude, latitude, distance, unit);
    }

    @Override
    public Flux<GeoWithin<V>> georadius(K key, double longitude, double latitude, double distance, GeoArgs.Unit unit,
            GeoArgs geoArgs) {

        if (getStatefulConnection().getState().hasCommand(CommandType.GEORADIUS_RO)) {
            return super.georadius_ro(key, longitude, latitude, distance, unit, geoArgs);
        }

        return super.georadius(key, longitude, latitude, distance, unit, geoArgs);
    }

    @Override
    public Flux<V> georadiusbymember(K key, V member, double distance, GeoArgs.Unit unit) {

        if (getStatefulConnection().getState().hasCommand(CommandType.GEORADIUS_RO)) {
            return super.georadiusbymember_ro(key, member, distance, unit);
        }

        return super.georadiusbymember(key, member, distance, unit);
    }

    @Override
    public Flux<GeoWithin<V>> georadiusbymember(K key, V member, double distance, GeoArgs.Unit unit, GeoArgs geoArgs) {

        if (getStatefulConnection().getState().hasCommand(CommandType.GEORADIUS_RO)) {
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
    public PubSubReactiveNodeSelection<K, V> nodes(Predicate<RedisClusterNode> predicate) {

        PubSubReactiveNodeSelection<K, V> selection = new StaticPubSubReactiveNodeSelection<K, V>(getStatefulConnection(),
                predicate);

        NodeSelectionInvocationHandler h = new NodeSelectionInvocationHandler((AbstractNodeSelection<?, ?, ?, ?>) selection,
                RedisPubSubReactiveCommands.class, REACTIVE);
        return (PubSubReactiveNodeSelection<K, V>) Proxy.newProxyInstance(NodeSelectionSupport.class.getClassLoader(),
                new Class<?>[] { NodeSelectionPubSubReactiveCommands.class, PubSubReactiveNodeSelection.class }, h);
    }

    private static class StaticPubSubReactiveNodeSelection<K, V>
            extends AbstractNodeSelection<RedisPubSubReactiveCommands<K, V>, NodeSelectionPubSubReactiveCommands<K, V>, K, V>
            implements PubSubReactiveNodeSelection<K, V> {

        private final List<RedisClusterNode> redisClusterNodes;

        private final ClusterDistributionChannelWriter writer;

        @SuppressWarnings("unchecked")
        public StaticPubSubReactiveNodeSelection(StatefulRedisClusterPubSubConnection<K, V> globalConnection,
                Predicate<RedisClusterNode> selector) {

            this.redisClusterNodes = globalConnection.getPartitions().stream().filter(selector).collect(Collectors.toList());
            writer = ((StatefulRedisClusterPubSubConnectionImpl) globalConnection).getClusterDistributionChannelWriter();
        }

        @Override
        protected CompletableFuture<RedisPubSubReactiveCommands<K, V>> getApi(RedisClusterNode redisClusterNode) {
            return getConnection(redisClusterNode).thenApply(StatefulRedisPubSubConnection::reactive);
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
