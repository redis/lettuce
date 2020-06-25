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
package io.lettuce.core.cluster.pubsub.api.sync;

import java.util.function.Predicate;

import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.cluster.pubsub.StatefulRedisClusterPubSubConnection;
import io.lettuce.core.pubsub.api.sync.RedisPubSubCommands;

/**
 * Synchronous and thread-safe Redis Cluster PubSub API. Operations are executed either on the main connection or a
 * {@link PubSubNodeSelection}.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 4.4
 */
public interface RedisClusterPubSubCommands<K, V> extends RedisPubSubCommands<K, V> {

    /**
     * @return the underlying connection.
     */
    StatefulRedisClusterPubSubConnection<K, V> getStatefulConnection();

    /**
     * Select all upstream nodes.
     *
     * @return API with asynchronous executed commands on a selection of upstream cluster nodes.
     * @deprecated since 6.0 in favor of {@link #upstream()}.
     */
    @Deprecated
    default PubSubNodeSelection<K, V> masters() {
        return nodes(redisClusterNode -> redisClusterNode.is(RedisClusterNode.NodeFlag.UPSTREAM));
    }

    /**
     * Select all upstream nodes.
     *
     * @return API with asynchronous executed commands on a selection of upstream cluster nodes.
     * @since 6.0
     */
    default PubSubNodeSelection<K, V> upstream() {
        return nodes(redisClusterNode -> redisClusterNode.is(RedisClusterNode.NodeFlag.UPSTREAM));
    }

    /**
     * Select all replicas.
     *
     * @return API with asynchronous executed commands on a selection of replica cluster nodes.
     * @deprecated since 5.2, use {@link #replicas()}
     */
    @Deprecated
    default PubSubNodeSelection<K, V> slaves() {
        return nodes(redisClusterNode -> redisClusterNode.is(RedisClusterNode.NodeFlag.SLAVE));
    }

    /**
     * Select all replicas.
     *
     * @param predicate Predicate to filter nodes
     * @return API with asynchronous executed commands on a selection of replica cluster nodes.
     * @deprecated since 5.2, use {@link #replicas(Predicate)}
     */
    @Deprecated
    default PubSubNodeSelection<K, V> slaves(Predicate<RedisClusterNode> predicate) {
        return nodes(
                redisClusterNode -> predicate.test(redisClusterNode) && redisClusterNode.is(RedisClusterNode.NodeFlag.REPLICA));
    }

    /**
     * Select all replicas.
     *
     * @return API with asynchronous executed commands on a selection of replica cluster nodes.
     * @since 5.2
     */
    default PubSubNodeSelection<K, V> replicas() {
        return nodes(redisClusterNode -> redisClusterNode.is(RedisClusterNode.NodeFlag.REPLICA));
    }

    /**
     * Select all replicas.
     *
     * @param predicate Predicate to filter nodes
     * @return API with asynchronous executed commands on a selection of replica cluster nodes.
     * @since 5.2
     */
    default PubSubNodeSelection<K, V> replicas(Predicate<RedisClusterNode> predicate) {
        return nodes(
                redisClusterNode -> predicate.test(redisClusterNode) && redisClusterNode.is(RedisClusterNode.NodeFlag.REPLICA));
    }

    /**
     * Select all known cluster nodes.
     *
     * @return API with asynchronous executed commands on a selection of all cluster nodes.
     */
    default PubSubNodeSelection<K, V> all() {
        return nodes(redisClusterNode -> true);
    }

    /**
     * Select nodes by a predicate.
     *
     * @param predicate Predicate to filter nodes
     * @return API with asynchronous executed commands on a selection of cluster nodes matching {@code predicate}
     */
    PubSubNodeSelection<K, V> nodes(Predicate<RedisClusterNode> predicate);

}
