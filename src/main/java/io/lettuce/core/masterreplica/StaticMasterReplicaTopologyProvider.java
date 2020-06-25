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

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import io.lettuce.core.internal.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.models.role.RedisNodeDescription;
import io.lettuce.core.models.role.RoleParser;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Topology provider for a static node collection. This provider uses a static collection of nodes to determine the role of each
 * {@link RedisURI node}. Node roles may change during runtime but the configuration must remain the same. This
 * {@link TopologyProvider} does not auto-discover nodes.
 *
 * @author Mark Paluch
 * @author Adam McElwee
 */
class StaticMasterReplicaTopologyProvider implements TopologyProvider {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(StaticMasterReplicaTopologyProvider.class);

    private final RedisClient redisClient;

    private final Iterable<RedisURI> redisURIs;

    public StaticMasterReplicaTopologyProvider(RedisClient redisClient, Iterable<RedisURI> redisURIs) {

        LettuceAssert.notNull(redisClient, "RedisClient must not be null");
        LettuceAssert.notNull(redisURIs, "RedisURIs must not be null");
        LettuceAssert.notNull(redisURIs.iterator().hasNext(), "RedisURIs must not be empty");

        this.redisClient = redisClient;
        this.redisURIs = redisURIs;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List<RedisNodeDescription> getNodes() {

        RedisURI next = redisURIs.iterator().next();

        try {
            return getNodesAsync().get(next.getTimeout().toMillis(), TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw Exceptions.bubble(e);
        }
    }

    @Override
    public CompletableFuture<List<RedisNodeDescription>> getNodesAsync() {

        List<StatefulRedisConnection<String, String>> connections = new CopyOnWriteArrayList<>();

        Flux<RedisURI> uris = Flux.fromIterable(redisURIs);
        Mono<List<RedisNodeDescription>> nodes = uris.flatMap(uri -> getNodeDescription(connections, uri)).collectList()
                .flatMap((nodeDescriptions) -> {

                    if (nodeDescriptions.isEmpty()) {
                        return Mono.error(new RedisConnectionException(
                                String.format("Failed to connect to at least one node in %s", redisURIs)));
                    }

                    return Mono.just(nodeDescriptions);
                });

        return nodes.toFuture();
    }

    private Mono<RedisNodeDescription> getNodeDescription(List<StatefulRedisConnection<String, String>> connections,
            RedisURI uri) {

        return Mono.fromCompletionStage(redisClient.connectAsync(StringCodec.UTF8, uri)) //
                .onErrorResume(t -> {

                    logger.warn("Cannot connect to {}", uri, t);
                    return Mono.empty();
                }) //
                .doOnNext(connections::add) //
                .flatMap(connection -> {

                    Mono<RedisNodeDescription> instance = getNodeDescription(uri, connection);

                    return instance.flatMap(it -> ResumeAfter.close(connection).thenEmit(it)).doFinally(s -> {
                        connections.remove(connection);
                    });
                });
    }

    private static Mono<RedisNodeDescription> getNodeDescription(RedisURI uri,
            StatefulRedisConnection<String, String> connection) {

        return connection.reactive().role().collectList().map(RoleParser::parse)
                .map(it -> new RedisUpstreamReplicaNode(uri.getHost(), uri.getPort(), uri, it.getRole()));
    }

}
