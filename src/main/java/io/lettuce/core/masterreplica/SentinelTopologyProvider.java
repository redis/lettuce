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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import io.lettuce.core.internal.Exceptions;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.models.role.RedisInstance;
import io.lettuce.core.models.role.RedisNodeDescription;
import io.lettuce.core.sentinel.api.StatefulRedisSentinelConnection;
import io.lettuce.core.sentinel.api.reactive.RedisSentinelReactiveCommands;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Topology provider using Redis Sentinel and the Sentinel API.
 *
 * @author Mark Paluch
 * @since 4.1
 */
class SentinelTopologyProvider implements TopologyProvider {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(SentinelTopologyProvider.class);

    private final String masterId;

    private final RedisClient redisClient;

    private final RedisURI sentinelUri;

    private final Duration timeout;

    /**
     * Creates a new {@link SentinelTopologyProvider}.
     *
     * @param masterId must not be empty
     * @param redisClient must not be {@code null}.
     * @param sentinelUri must not be {@code null}.
     */
    public SentinelTopologyProvider(String masterId, RedisClient redisClient, RedisURI sentinelUri) {

        LettuceAssert.notEmpty(masterId, "MasterId must not be empty");
        LettuceAssert.notNull(redisClient, "RedisClient must not be null");
        LettuceAssert.notNull(sentinelUri, "Sentinel URI must not be null");

        this.masterId = masterId;
        this.redisClient = redisClient;
        this.sentinelUri = sentinelUri;
        this.timeout = sentinelUri.getTimeout();
    }

    @Override
    public List<RedisNodeDescription> getNodes() {

        logger.debug("lookup topology for masterId {}", masterId);

        try {
            return getNodesAsync().get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw Exceptions.bubble(e);
        }
    }

    @Override
    public CompletableFuture<List<RedisNodeDescription>> getNodesAsync() {

        logger.debug("lookup topology for masterId {}", masterId);

        Mono<StatefulRedisSentinelConnection<String, String>> connect = Mono
                .fromFuture(redisClient.connectSentinelAsync(StringCodec.UTF8, sentinelUri));

        return connect.flatMap(this::getNodes).toFuture();
    }

    protected Mono<List<RedisNodeDescription>> getNodes(StatefulRedisSentinelConnection<String, String> connection) {

        RedisSentinelReactiveCommands<String, String> reactive = connection.reactive();

        Mono<Tuple2<Map<String, String>, List<Map<String, String>>>> masterAndReplicas = reactive.master(masterId)
                .zipWith(reactive.slaves(masterId).collectList()).timeout(this.timeout).flatMap(tuple -> {
                    return ResumeAfter.close(connection).thenEmit(tuple);
                }).doOnError(e -> connection.closeAsync());

        return masterAndReplicas.map(tuple -> {

            List<RedisNodeDescription> result = new ArrayList<>();

            result.add(toNode(tuple.getT1(), RedisInstance.Role.UPSTREAM));
            result.addAll(tuple.getT2().stream().filter(SentinelTopologyProvider::isAvailable)
                    .map(map -> toNode(map, RedisInstance.Role.REPLICA)).collect(Collectors.toList()));

            return result;
        });
    }

    private static boolean isAvailable(Map<String, String> map) {

        String flags = map.get("flags");
        if (flags != null) {
            if (flags.contains("s_down") || flags.contains("o_down") || flags.contains("disconnected")) {
                return false;
            }
        }
        return true;
    }

    private RedisNodeDescription toNode(Map<String, String> map, RedisInstance.Role role) {

        String ip = map.get("ip");
        String port = map.get("port");
        return new RedisUpstreamReplicaNode(ip, Integer.parseInt(port), sentinelUri, role);
    }

}
