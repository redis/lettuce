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

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import io.lettuce.core.internal.ExceptionFactory;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.resource.ClientResources;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * @author Mark Paluch
 * @author Christian Weitendorf
 * @author Xujs
 */
class Connections {

    private final static InternalLogger LOG = InternalLoggerFactory.getInstance(Connections.class);

    private final ClientResources clientResources;

    private final Map<RedisURI, StatefulRedisConnection<String, String>> connections;

    private volatile boolean closed = false;

    public Connections(ClientResources clientResources, Map<RedisURI, StatefulRedisConnection<String, String>> connections) {
        this.clientResources = clientResources;
        this.connections = connections;
    }

    /**
     * Add a connection for a {@link RedisURI}
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
    public Requests requestTopology(long timeout, TimeUnit timeUnit) {

        return doRequest(() -> {

            CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8).add(CommandKeyword.NODES);
            Command<String, String, String> command = new Command<>(CommandType.CLUSTER, new StatusOutput<>(StringCodec.UTF8),
                    args);
            return new TimedAsyncCommand<>(command);
        }, timeout, timeUnit);
    }

    /*
     * Initiate {@code INFO CLIENTS} on all connections and return the {@link Requests}.
     * @return the {@link Requests}.
     */
    public Requests requestClients(long timeout, TimeUnit timeUnit) {

        return doRequest(() -> {

            Command<String, String, String> command = new Command<>(CommandType.INFO, new StatusOutput<>(StringCodec.UTF8),
                    new CommandArgs<>(StringCodec.UTF8).add("CLIENTS"));
            return new TimedAsyncCommand<>(command);
        }, timeout, timeUnit);
    }

    /*
     * Initiate {@code CLUSTER NODES} on all connections and return the {@link Requests}.
     * @return the {@link Requests}.
     */
    private Requests doRequest(Supplier<TimedAsyncCommand<String, String, String>> commandFactory, long timeout,
            TimeUnit timeUnit) {

        Requests requests = new Requests();
        Duration timeoutDuration = Duration.ofNanos(timeUnit.toNanos(timeout));

        synchronized (this.connections) {
            for (Map.Entry<RedisURI, StatefulRedisConnection<String, String>> entry : this.connections.entrySet()) {

                TimedAsyncCommand<String, String, String> timedCommand = commandFactory.get();

                clientResources.timer().newTimeout(it -> {
                    timedCommand.completeExceptionally(ExceptionFactory.createTimeoutException(timeoutDuration));
                }, timeout, timeUnit);

                entry.getValue().dispatch(timedCommand);
                requests.addRequest(entry.getKey(), timedCommand);
            }
        }

        return requests;
    }

    public Connections retainAll(Set<RedisURI> connectionsToRetain) {

        Set<RedisURI> keys = new LinkedHashSet<>(connections.keySet());

        for (RedisURI key : keys) {
            if (!connectionsToRetain.contains(key)) {
                this.connections.remove(key);
            }
        }

        return this;
    }

}
