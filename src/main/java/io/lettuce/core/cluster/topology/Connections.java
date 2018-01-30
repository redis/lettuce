/*
 * Copyright 2011-2018 the original author or authors.
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
package io.lettuce.core.cluster.topology;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;
import io.lettuce.core.protocol.CommandType;

/**
 * @author Mark Paluch
 */
class Connections {

    private final Map<RedisURI, StatefulRedisConnection<String, String>> connections;
    private volatile boolean closed = false;

    public Connections() {
        connections = new TreeMap<>(TopologyComparators.RedisURIComparator.INSTANCE);
    }

    private Connections(Map<RedisURI, StatefulRedisConnection<String, String>> connections) {
        this.connections = connections;
    }

    /**
     * Add a connection for a {@link RedisURI}
     *
     * @param redisURI
     * @param connection
     */
    public void addConnection(RedisURI redisURI, StatefulRedisConnection<String, String> connection) {

        if (closed) { // fastpath
            connection.close();
            return;
        }

        synchronized (connections) {

            if (closed) {
                connection.close();
                return;
            }

            connections.put(redisURI, connection);
        }
    }

    /**
     * @return {@literal true} if no connections present.
     */
    public boolean isEmpty() {
        synchronized (connections) {
            return connections.isEmpty();
        }
    }

    /*
     * Initiate {@code CLUSTER NODES} on all connections and return the {@link Requests}.
     *
     * @return the {@link Requests}.
     */
    public Requests requestTopology() {

        Requests requests = new Requests();

        for (Map.Entry<RedisURI, StatefulRedisConnection<String, String>> entry : connections.entrySet()) {

            CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8).add(CommandKeyword.NODES);
            Command<String, String, String> command = new Command<>(CommandType.CLUSTER, new StatusOutput<>(StringCodec.UTF8),
                    args);
            TimedAsyncCommand<String, String, String> timedCommand = new TimedAsyncCommand<>(command);

            entry.getValue().dispatch(timedCommand);
            requests.addRequest(entry.getKey(), timedCommand);
        }

        return requests;
    }

    /*
     * Initiate {@code CLIENT LIST} on all connections and return the {@link Requests}.
     *
     * @return the {@link Requests}.
     */
    public Requests requestClients() {

        Requests requests = new Requests();

        for (Map.Entry<RedisURI, StatefulRedisConnection<String, String>> entry : connections.entrySet()) {

            CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8).add(CommandKeyword.LIST);
            Command<String, String, String> command = new Command<>(CommandType.CLIENT, new StatusOutput<>(StringCodec.UTF8),
                    args);
            TimedAsyncCommand<String, String, String> timedCommand = new TimedAsyncCommand<>(command);

            entry.getValue().dispatch(timedCommand);
            requests.addRequest(entry.getKey(), timedCommand);
        }

        return requests;
    }

    /**
     * Close all connections.
     */
    public void close() {

        this.closed = true;

        while (hasConnections()) {

            for (StatefulRedisConnection<String, String> connection : drainConnections()) {
                connection.close();
            }
        }
    }

    private boolean hasConnections() {
        synchronized (connections) {
            return !this.connections.isEmpty();
        }
    }

    private Collection<StatefulRedisConnection<String, String>> drainConnections() {

        synchronized (this.connections) {

            Map<RedisURI, StatefulRedisConnection<String, String>> connections = new HashMap<>(this.connections);
            connections.forEach((k, v) -> {
                this.connections.remove(k);
            });
        }

        return connections.values();
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
                result.putAll(discoveredConnections.connections);
            }
        }

        return new Connections(result);
    }
}
