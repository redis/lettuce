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
package io.lettuce.core.masterslave;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;

import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.models.role.RedisNodeDescription;

/**
 *
 * @author Mark Paluch
 */
class AsyncConnections {

    private final Map<RedisURI, CompletableFuture<StatefulRedisConnection<String, String>>> connections = new TreeMap<>(
            MasterSlaveUtils.RedisURIComparator.INSTANCE);

    private final List<RedisNodeDescription> nodeList;

    AsyncConnections(List<RedisNodeDescription> nodeList) {
        this.nodeList = nodeList;
    }

    /**
     * Add a connection for a {@link RedisURI}.
     *
     * @param redisURI
     * @param connection
     */
    public void addConnection(RedisURI redisURI, CompletableFuture<StatefulRedisConnection<String, String>> connection) {
        connections.put(redisURI, connection);
    }

    public Mono<Connections> asMono(Duration timeout, ScheduledExecutorService timeoutExecutor) {

        Connections connections = new Connections(this.connections.size(), nodeList);

        for (Map.Entry<RedisURI, CompletableFuture<StatefulRedisConnection<String, String>>> entry : this.connections
                .entrySet()) {

            CompletableFuture<StatefulRedisConnection<String, String>> future = entry.getValue();

            future.whenComplete((connection, throwable) -> {

                if (throwable != null) {
                    connections.accept(throwable);
                } else {
                    connections.accept(Tuples.of(entry.getKey(), connection));
                }
            });
        }

        return Mono.fromCompletionStage(connections.getOrTimeout(timeout, timeoutExecutor));
    }

}
