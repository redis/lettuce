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
package io.lettuce.core.cluster.topology;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;

/**
 *
 * @author Mark Paluch
 */
class AsyncConnections {

    private final Map<RedisURI, CompletableFuture<StatefulRedisConnection<String, String>>> futures = new TreeMap<>(
            TopologyComparators.RedisURIComparator.INSTANCE);

    public AsyncConnections() {
    }

    /**
     * Add a connection for a {@link RedisURI}.
     *
     * @param redisURI
     * @param connection
     */
    public void addConnection(RedisURI redisURI, CompletableFuture<StatefulRedisConnection<String, String>> connection) {
        futures.put(redisURI, connection);
    }

    /**
     *
     * @return a set of {@link RedisURI} for which {@link Connections} has a connection.
     */
    public Set<RedisURI> connectedNodes() {
        return futures.keySet();
    }

    /**
     *
     * @return the {@link Connections}.
     * @throws RedisConnectionException if no connection could be established.
     */
    public Connections get(long timeout, TimeUnit timeUnit) throws InterruptedException {

        Connections connections = new Connections();
        List<Throwable> exceptions = new CopyOnWriteArrayList<>();
        List<Future<?>> sync = new ArrayList<>(this.futures.size());

        for (Map.Entry<RedisURI, CompletableFuture<StatefulRedisConnection<String, String>>> entry : this.futures.entrySet()) {

            CompletableFuture<StatefulRedisConnection<String, String>> future = entry.getValue();

            sync.add(future.whenComplete((connection, throwable) -> {

                if (throwable != null) {
                    exceptions.add(throwable);
                } else {
                    connections.addConnection(entry.getKey(), connection);
                }
            }));
        }

        RefreshFutures.awaitAll(timeout, timeUnit, sync);

        if (connections.isEmpty() && !sync.isEmpty() && !exceptions.isEmpty()) {

            RedisConnectionException collector = null;

            for (Throwable exception : exceptions) {
                if (collector == null) {
                    collector = new RedisConnectionException(
                            "Unable to establish a connection to Redis Cluster at " + this.futures.keySet(), exception);
                } else {
                    collector.addSuppressed(exception);
                }
            }

            throw collector;
        }

        return connections;
    }

    /**
     *
     * @return the {@link Connections}.
     */
    public Connections optionalGet(long timeout, TimeUnit timeUnit) throws InterruptedException {

        Connections connections = new Connections();
        List<Future<?>> sync = new ArrayList<>(this.futures.size());

        for (Map.Entry<RedisURI, CompletableFuture<StatefulRedisConnection<String, String>>> entry : this.futures.entrySet()) {

            CompletableFuture<StatefulRedisConnection<String, String>> future = entry.getValue();

            sync.add(future.thenAccept((connection) -> {
                connections.addConnection(entry.getKey(), connection);
            }));
        }

        RefreshFutures.awaitAll(timeout, timeUnit, sync);

        return connections;
    }

}
