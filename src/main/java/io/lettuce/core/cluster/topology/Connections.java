/*
 * Copyright 2011-2020 the original author or authors.
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

import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.internal.Futures;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;
import io.lettuce.core.protocol.CommandType;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 *
 * @author Mark Paluch
 * @author Christian Weitendorf
 * @author Xujs
 */
class Connections {

    private final static InternalLogger LOG = InternalLoggerFactory.getInstance(Connections.class);

    private final Map<RedisURI, StatefulRedisConnection<String, String>> connections;

    private volatile boolean closed = false;

    public Connections() {
        connections = new TreeMap<>(TopologyComparators.RedisURIComparator.INSTANCE);
    }

    private Connections(Map<RedisURI, StatefulRedisConnection<String, String>> connections) {
        this.connections = connections;
    }

    /**
     * Add a connection for a {@link RedisURI}.
     *
     * @param redisURI
     * @param connection
     */
    public void addConnection(RedisURI redisURI, StatefulRedisConnection<String, String> connection) {

        if (this.closed) { // fastpath
            connection.closeAsync();
            return;
        }

        synchronized (this.connections) {

            if (this.closed) {
                connection.closeAsync();
                return;
            }

            this.connections.put(redisURI, connection);
        }
    }

    /**
     *
     * @return {@code true} if no connections present.
     */
    public boolean isEmpty() {
        synchronized (this.connections) {
            return this.connections.isEmpty();
        }
    }

    /*
     * Initiate {@code CLUSTER NODES} on all connections and return the {@link Requests}.
     * @return the {@link Requests}.
     */
    public Requests requestTopology() {

        Requests requests = new Requests();

        synchronized (this.connections) {
            for (Map.Entry<RedisURI, StatefulRedisConnection<String, String>> entry : this.connections.entrySet()) {

                CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8).add(CommandKeyword.NODES);
                Command<String, String, String> command = new Command<>(CommandType.CLUSTER,
                        new StatusOutput<>(StringCodec.UTF8), args);
                TimedAsyncCommand<String, String, String> timedCommand = new TimedAsyncCommand<>(command);

                entry.getValue().dispatch(timedCommand);
                requests.addRequest(entry.getKey(), timedCommand);
            }
        }

        return requests;
    }

    /*
     * Initiate {@code INFO CLIENTS} on all connections and return the {@link Requests}.
     * @return the {@link Requests}.
     */
    public Requests requestClients() {

        Requests requests = new Requests();

        synchronized (this.connections) {
            for (Map.Entry<RedisURI, StatefulRedisConnection<String, String>> entry : this.connections.entrySet()) {

                Command<String, String, String> command = new Command<>(CommandType.INFO, new StatusOutput<>(StringCodec.UTF8),
                        new CommandArgs<>(StringCodec.UTF8).add("CLIENTS"));
                TimedAsyncCommand<String, String, String> timedCommand = new TimedAsyncCommand<>(command);

                entry.getValue().dispatch(timedCommand);
                requests.addRequest(entry.getKey(), timedCommand);
            }
        }

        return requests;
    }

    /**
     * Close all connections.
     */
    public void close() {

        this.closed = true;

        List<CompletableFuture<?>> closeFutures = new ArrayList<>();
        while (hasConnections()) {
            for (StatefulRedisConnection<String, String> connection : drainConnections()) {
                closeFutures.add(connection.closeAsync());
            }
        }

        Futures.allOf(closeFutures).join();
    }

    private boolean hasConnections() {

        synchronized (this.connections) {
            return !this.connections.isEmpty();
        }
    }

    private Collection<StatefulRedisConnection<String, String>> drainConnections() {

        Map<RedisURI, StatefulRedisConnection<String, String>> drainedConnections;

        synchronized (this.connections) {

            drainedConnections = new HashMap<>(this.connections);
            drainedConnections.forEach((k, v) -> {
                this.connections.remove(k);
            });
        }

        return drainedConnections.values();
    }

    /**
     * Merges this and {@code discoveredConnections} into a new {@link Connections} instance. This instance is marked as closed
     * to prevent lingering connections.
     *
     * @param discoveredConnections
     * @return
     */
    public Connections mergeWith(Connections discoveredConnections) {

        Map<RedisURI, StatefulRedisConnection<String, String>> result = new TreeMap<>(
                TopologyComparators.RedisURIComparator.INSTANCE);

        this.closed = true;
        discoveredConnections.closed = true;

        synchronized (this.connections) {
            synchronized (discoveredConnections.connections) {

                result.putAll(this.connections);

                for (RedisURI redisURI : discoveredConnections.connections.keySet()) {

                    StatefulRedisConnection<String, String> existing = result.put(redisURI,
                            discoveredConnections.connections.get(redisURI));

                    if (existing != null) {
                        LOG.error("Duplicate topology refresh connection for " + redisURI);
                        existing.closeAsync();
                    }
                }
            }
        }

        return new Connections(result);
    }

}
