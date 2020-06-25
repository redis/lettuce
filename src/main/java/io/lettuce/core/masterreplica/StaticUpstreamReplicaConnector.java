/*
 * Copyright 2020 the original author or authors.
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
package io.lettuce.core.masterreplica;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import reactor.core.publisher.Mono;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.models.role.RedisNodeDescription;

/**
 * {@link UpstreamReplicaConnector} to connect to a static declared Master/Replica setup providing a fixed array of
 * {@link RedisURI}. This connector determines roles and remains using only the provided endpoints.
 *
 * @author Mark Paluch
 * @since 5.1
 */
class StaticUpstreamReplicaConnector<K, V> implements UpstreamReplicaConnector<K, V> {

    private final RedisClient redisClient;

    private final RedisCodec<K, V> codec;

    private final Iterable<RedisURI> redisURIs;

    StaticUpstreamReplicaConnector(RedisClient redisClient, RedisCodec<K, V> codec, Iterable<RedisURI> redisURIs) {
        this.redisClient = redisClient;
        this.codec = codec;
        this.redisURIs = redisURIs;
    }

    @Override
    public CompletableFuture<StatefulRedisMasterReplicaConnection<K, V>> connectAsync() {

        Map<RedisURI, StatefulRedisConnection<K, V>> initialConnections = new HashMap<>();

        TopologyProvider topologyProvider = new StaticMasterReplicaTopologyProvider(redisClient, redisURIs);

        RedisURI seedNode = redisURIs.iterator().next();

        UpstreamReplicaTopologyRefresh refresh = new UpstreamReplicaTopologyRefresh(redisClient, topologyProvider);
        UpstreamReplicaConnectionProvider<K, V> connectionProvider = new UpstreamReplicaConnectionProvider<>(redisClient, codec,
                seedNode, initialConnections);

        return refresh.getNodes(seedNode).flatMap(nodes -> {

            if (nodes.isEmpty()) {
                return Mono.error(new RedisException(String.format("Cannot determine topology from %s", redisURIs)));
            }

            return initializeConnection(codec, seedNode, connectionProvider, nodes);
        }).onErrorMap(ExecutionException.class, Throwable::getCause).toFuture();
    }

    private Mono<StatefulRedisMasterReplicaConnection<K, V>> initializeConnection(RedisCodec<K, V> codec, RedisURI seedNode,
            UpstreamReplicaConnectionProvider<K, V> connectionProvider, List<RedisNodeDescription> nodes) {

        connectionProvider.setKnownNodes(nodes);

        UpstreamReplicaChannelWriter channelWriter = new UpstreamReplicaChannelWriter(connectionProvider,
                redisClient.getResources());

        StatefulRedisUpstreamReplicaConnectionImpl<K, V> connection = new StatefulRedisUpstreamReplicaConnectionImpl<>(
                channelWriter, codec, seedNode.getTimeout());
        connection.setOptions(redisClient.getOptions());

        return Mono.just(connection);
    }

}
