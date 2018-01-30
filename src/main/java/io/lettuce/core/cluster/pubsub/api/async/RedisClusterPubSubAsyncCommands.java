/*
 * Copyright 2016-2018 the original author or authors.
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
package io.lettuce.core.cluster.pubsub.api.async;

import java.util.function.Predicate;

import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.cluster.pubsub.StatefulRedisClusterPubSubConnection;
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands;

/**
 * Asynchronous and thread-safe Redis Cluster PubSub API. Operations are executed either on the main connection or a
 * {@link PubSubAsyncNodeSelection}.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 4.4
 */
public interface RedisClusterPubSubAsyncCommands<K, V> extends RedisPubSubAsyncCommands<K, V> {

    /**
     * @return the underlying connection.
     */
    StatefulRedisClusterPubSubConnection<K, V> getStatefulConnection();

    /**
     * Select all masters.
     *
     * @return API with asynchronous executed commands on a selection of master cluster nodes.
     */
    default PubSubAsyncNodeSelection<K, V> masters() {
        return nodes(redisClusterNode -> redisClusterNode.is(RedisClusterNode.NodeFlag.MASTER));
    }

    /**
     * Select all slaves.
     *
     * @return API with asynchronous executed commands on a selection of slave cluster nodes.
     */
    default PubSubAsyncNodeSelection<K, V> slaves() {
        return nodes(redisClusterNode -> redisClusterNode.is(RedisClusterNode.NodeFlag.SLAVE));
    }

    /**
     * Select all slaves.
     *
     * @param predicate Predicate to filter nodes
     * @return API with asynchronous executed commands on a selection of slave cluster nodes.
     */
    default PubSubAsyncNodeSelection<K, V> slaves(Predicate<RedisClusterNode> predicate) {
        return nodes(
                redisClusterNode -> predicate.test(redisClusterNode) && redisClusterNode.is(RedisClusterNode.NodeFlag.SLAVE));
    }

    /**
     * Select all known cluster nodes.
     *
     * @return API with asynchronous executed commands on a selection of all cluster nodes.
     */
    default PubSubAsyncNodeSelection<K, V> all() {
        return nodes(redisClusterNode -> true);
    }

    /**
     * Select nodes by a predicate.
     *
     * @param predicate Predicate to filter nodes
     * @return API with asynchronous executed commands on a selection of cluster nodes matching {@code predicate}
     */
    PubSubAsyncNodeSelection<K, V> nodes(Predicate<RedisClusterNode> predicate);
}
