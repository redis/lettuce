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
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;

import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.internal.AsyncCloseable;
import io.lettuce.core.internal.Futures;
import io.lettuce.core.models.role.RedisNodeDescription;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;
import io.lettuce.core.protocol.CommandType;

/**
 * Connection collector with non-blocking synchronization. This synchronizer emits itself through a {@link Mono} as soon as it
 * gets synchronized via either receiving connects/exceptions from all connections or timing out.
 * <p>
 * It can be used only once via {@link #getOrTimeout(Duration, ScheduledExecutorService)}.
 * <p>
 * Synchronizer uses a gate to determine whether it was already emitted or awaiting incoming events (exceptions, successful
 * connects). Connections arriving after closing the gate are discarded.
 *
 * @author Mark Paluch
 */
class Connections extends CompletableEventLatchSupport<Tuple2<RedisURI, StatefulRedisConnection<String, String>>, Connections>
        implements AsyncCloseable {

    private final Map<RedisURI, StatefulRedisConnection<String, String>> connections = new TreeMap<>(
            ReplicaUtils.RedisURIComparator.INSTANCE);

    private final List<Throwable> exceptions = new CopyOnWriteArrayList<>();

    private final List<RedisNodeDescription> nodes;

    private volatile boolean closed = false;

    public Connections(int expectedConnectionCount, List<RedisNodeDescription> nodes) {
        super(expectedConnectionCount);
        this.nodes = nodes;
    }

    @Override
    protected void onAccept(Tuple2<RedisURI, StatefulRedisConnection<String, String>> value) {

        if (this.closed) {
            value.getT2().closeAsync();
            return;
        }

        synchronized (this.connections) {
            this.connections.put(value.getT1(), value.getT2());
        }
    }

    @Override
    protected void onError(Throwable value) {
        this.exceptions.add(value);
    }

    @Override
    protected void onDrop(Tuple2<RedisURI, StatefulRedisConnection<String, String>> value) {
        value.getT2().closeAsync();
    }

    @Override
    protected void onDrop(Throwable value) {

    }

    @Override
    protected void onEmit(Emission<Connections> emission) {

        if (getExpectedCount() != 0 && this.connections.isEmpty() && !this.exceptions.isEmpty()) {

            RedisConnectionException collector = new RedisConnectionException(
                    "Unable to establish a connection to Redis Master/Replica");
            this.exceptions.forEach(collector::addSuppressed);

            emission.error(collector);
        } else {
            emission.success(this);
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
     * Initiate {@code PING} on all connections and return the {@link Requests}.
     * @return the {@link Requests}.
     */
    public Requests requestPing() {

        Set<Map.Entry<RedisURI, StatefulRedisConnection<String, String>>> entries = new LinkedHashSet<>(
                this.connections.entrySet());
        Requests requests = new Requests(entries.size(), this.nodes);

        for (Map.Entry<RedisURI, StatefulRedisConnection<String, String>> entry : entries) {

            CommandArgs<String, String> args = new CommandArgs<>(StringCodec.ASCII).add(CommandKeyword.NODES);
            Command<String, String, String> command = new Command<>(CommandType.PING, new StatusOutput<>(StringCodec.ASCII),
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
    public CompletableFuture<Void> closeAsync() {

        List<CompletableFuture<?>> close = new ArrayList<>(this.connections.size());
        List<RedisURI> toRemove = new ArrayList<>(this.connections.size());

        this.closed = true;

        for (Map.Entry<RedisURI, StatefulRedisConnection<String, String>> entry : this.connections.entrySet()) {

            toRemove.add(entry.getKey());
            close.add(entry.getValue().closeAsync());
        }

        for (RedisURI redisURI : toRemove) {
            this.connections.remove(redisURI);
        }

        return Futures.allOf(close);
    }

}
