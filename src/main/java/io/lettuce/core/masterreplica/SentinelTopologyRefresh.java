/*
 * Copyright 2020-2022 the original author or authors.
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
package io.lettuce.core.masterreplica;

import java.io.Closeable;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Supplier;

import io.lettuce.core.ConnectionFuture;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.AsyncCloseable;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.event.jfr.EventRecorder;
import io.lettuce.core.internal.Futures;
import io.lettuce.core.internal.LettuceLists;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Sentinel Pub/Sub listener-enabled topology refresh. This refresh triggers topology updates if Redis topology changes
 * (monitored master/replicas) or the Sentinel availability changes.
 *
 * @author Mark Paluch
 * @since 4.2
 */
class SentinelTopologyRefresh implements AsyncCloseable, Closeable {

    private static final InternalLogger LOG = InternalLoggerFactory.getInstance(SentinelTopologyRefresh.class);

    private static final StringCodec CODEC = new StringCodec(StandardCharsets.US_ASCII);

    private static final Set<String> PROCESSING_CHANNELS = new HashSet<>(
            Arrays.asList("failover-end", "failover-end-for-timeout"));

    private final Map<RedisURI, ConnectionFuture<StatefulRedisPubSubConnection<String, String>>> pubSubConnections = new ConcurrentHashMap<>();

    private final RedisClient redisClient;

    private final List<RedisURI> sentinels;

    private final List<Runnable> refreshRunnables = new CopyOnWriteArrayList<>();

    private final PubSubMessageHandler messageHandler = SentinelTopologyRefresh.this::processMessage;

    private final PubSubMessageActionScheduler topologyRefresh;

    private final PubSubMessageActionScheduler sentinelReconnect;

    private final CompletableFuture<Void> closeFuture = new CompletableFuture<>();

    private volatile boolean closed = false;

    SentinelTopologyRefresh(RedisClient redisClient, String masterId, List<RedisURI> sentinels) {

        this.redisClient = redisClient;
        this.sentinels = LettuceLists.newList(sentinels);
        this.topologyRefresh = new PubSubMessageActionScheduler(redisClient.getResources().eventExecutorGroup(),
                new TopologyRefreshMessagePredicate(masterId));
        this.sentinelReconnect = new PubSubMessageActionScheduler(redisClient.getResources().eventExecutorGroup(),
                new SentinelReconnectMessagePredicate());
    }

    @Override
    public void close() {
        closeAsync().join();
    }

    @Override
    public CompletableFuture<Void> closeAsync() {

        if (closed) {
            return closeFuture;
        }

        closed = true;

        HashMap<RedisURI, ConnectionFuture<StatefulRedisPubSubConnection<String, String>>> connections = new HashMap<>(
                pubSubConnections);
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        connections.forEach((k, f) -> {

            futures.add(f.exceptionally(t -> null).thenCompose(c -> {

                if (c == null) {
                    return CompletableFuture.completedFuture(null);
                }
                return c.closeAsync();
            }).toCompletableFuture());

            pubSubConnections.remove(k);
        });

        Futures.allOf(futures).whenComplete((aVoid, throwable) -> {

            if (throwable != null) {
                closeFuture.completeExceptionally(throwable);
            } else {
                closeFuture.complete(null);
            }
        });

        return closeFuture;
    }

    CompletionStage<Void> bind(Runnable runnable) {

        refreshRunnables.add(runnable);

        return initializeSentinels();
    }

    /**
     * Initialize/extend connections to Sentinel servers.
     *
     * @return
     */
    private CompletionStage<Void> initializeSentinels() {

        if (closed) {
            return closeFuture;
        }

        Duration timeout = getTimeout();

        List<ConnectionFuture<StatefulRedisPubSubConnection<String, String>>> connectionFutures = potentiallyConnectSentinels();

        if (connectionFutures.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        if (closed) {
            return closeAsync();
        }

        SentinelTopologyRefreshConnections collector = collectConnections(connectionFutures);

        CompletionStage<SentinelTopologyRefreshConnections> completionStage = collector.getOrTimeout(timeout,
                redisClient.getResources().eventExecutorGroup());

        return completionStage.whenComplete((aVoid, throwable) -> {

            if (throwable != null) {
                closeAsync();
            }
        }).thenApply(noop -> (Void) null);
    }

    /**
     * Inspect whether additional Sentinel connections are required based on the which Sentinels are currently connected.
     *
     * @return list of futures that are notified with the connection progress.
     */
    private List<ConnectionFuture<StatefulRedisPubSubConnection<String, String>>> potentiallyConnectSentinels() {

        List<ConnectionFuture<StatefulRedisPubSubConnection<String, String>>> connectionFutures = new ArrayList<>();
        for (RedisURI sentinel : sentinels) {

            if (pubSubConnections.containsKey(sentinel)) {
                continue;
            }

            ConnectionFuture<StatefulRedisPubSubConnection<String, String>> future = redisClient.connectPubSubAsync(CODEC,
                    sentinel);
            pubSubConnections.put(sentinel, future);

            future.whenComplete((connection, throwable) -> {

                if (throwable != null || closed) {
                    pubSubConnections.remove(sentinel);
                }

                if (closed) {
                    connection.closeAsync();
                }
            });

            connectionFutures.add(future);
        }

        return connectionFutures;
    }

    private SentinelTopologyRefreshConnections collectConnections(
            List<ConnectionFuture<StatefulRedisPubSubConnection<String, String>>> connectionFutures) {

        SentinelTopologyRefreshConnections collector = new SentinelTopologyRefreshConnections(connectionFutures.size());

        for (ConnectionFuture<StatefulRedisPubSubConnection<String, String>> connectionFuture : connectionFutures) {

            String source = connectionFuture.getRemoteAddress() != null ? connectionFuture.getRemoteAddress().toString() : null;
            connectionFuture.thenCompose(connection -> {

                connection.addListener(new RedisPubSubAdapter<String, String>() {

                    @Override
                    public void message(String pattern, String channel, String message) {
                        messageHandler.handle(source, channel, message);
                    }

                });

                return connection.async().psubscribe("*").thenApply(v -> connection).whenComplete((c, t) -> {

                    if (t != null) {
                        connection.closeAsync();
                    }
                });
            }).whenComplete((connection, throwable) -> {

                if (throwable != null) {
                    collector.accept(throwable);
                } else {
                    collector.accept(connection);
                }
            });
        }

        return collector;
    }

    /**
     * @return operation timeout from the first sentinel to connect/first URI. Fallback to default timeout if no other timeout
     *         found.
     * @see RedisURI#DEFAULT_TIMEOUT_DURATION
     */
    private Duration getTimeout() {

        for (RedisURI sentinel : sentinels) {

            if (!pubSubConnections.containsKey(sentinel)) {
                return sentinel.getTimeout();
            }
        }

        for (RedisURI sentinel : sentinels) {
            return sentinel.getTimeout();
        }

        return RedisURI.DEFAULT_TIMEOUT_DURATION;
    }

    private void processMessage(String source, String channel, String message) {

        topologyRefresh.processMessage(source, channel, message, () -> {
            LOG.debug("Received topology changed signal from Redis Sentinel ({}), scheduling topology update", channel);
            return () -> refreshRunnables.forEach(Runnable::run);
        });

        sentinelReconnect.processMessage(source, channel, message, () -> {

            LOG.debug("Received sentinel state changed signal from Redis Sentinel, scheduling sentinel reconnect attempts");

            return this::initializeSentinels;
        });
    }

    private static class PubSubMessageActionScheduler {

        private final TimedSemaphore timedSemaphore = new TimedSemaphore();

        private final EventExecutorGroup eventExecutors;

        private final MessagePredicate filter;

        PubSubMessageActionScheduler(EventExecutorGroup eventExecutors, MessagePredicate filter) {
            this.eventExecutors = eventExecutors;
            this.filter = filter;
        }

        void processMessage(String source, String channel, String message, Supplier<Runnable> runnableSupplier) {

            if (!processingAllowed(channel, message)) {
                return;
            }

            timedSemaphore.onEvent(timeout -> {

                Runnable runnable = runnableSupplier.get();

                if (timeout == null) {
                    EventRecorder.getInstance().record(new SentinelTopologyRefreshEvent(source, message, 0));
                    eventExecutors.submit(runnable);
                } else {
                    EventRecorder.getInstance().record(new SentinelTopologyRefreshEvent(source, message, timeout.remaining()));
                    eventExecutors.schedule(runnable, timeout.remaining(), TimeUnit.MILLISECONDS);
                }
            });
        }

        private boolean processingAllowed(String channel, String message) {

            if (eventExecutors.isShuttingDown()) {
                return false;
            }

            if (!filter.test(channel, message)) {
                return false;
            }

            return true;
        }

    }

    /**
     * Lock-free semaphore that limits calls by using a {@link Timeout}. This class is thread-safe and
     * {@link #onEvent(Consumer)} may be called by multiple threads concurrently. It's guaranteed the first caller for an
     * expired {@link Timeout} will be called.
     */
    static class TimedSemaphore {

        private final AtomicReference<Timeout> timeoutRef = new AtomicReference<>();

        private final int timeout = 5;

        private final TimeUnit timeUnit = TimeUnit.SECONDS;

        /**
         * Rate-limited method that notifies the given {@link Consumer} once the current {@link Timeout} is expired.
         *
         * @param timeoutConsumer callback.
         */
        protected void onEvent(Consumer<Timeout> timeoutConsumer) {

            Timeout existingTimeout = timeoutRef.get();

            if (existingTimeout != null) {
                if (!existingTimeout.isExpired()) {
                    return;
                }
            }

            Timeout timeout = new Timeout(this.timeout, this.timeUnit);
            boolean state = timeoutRef.compareAndSet(existingTimeout, timeout);

            if (state) {
                timeoutConsumer.accept(timeout);
            }
        }

    }

    interface MessagePredicate extends BiPredicate<String, String> {

        @Override
        boolean test(String message, String channel);

    }

    /**
     * {@link MessagePredicate} to check whether the channel and message contain topology changes related to the monitored
     * master.
     */
    private static class TopologyRefreshMessagePredicate implements MessagePredicate {

        private final String masterId;

        private Set<String> TOPOLOGY_CHANGE_CHANNELS = new HashSet<>(
                Arrays.asList("+slave", "+sdown", "-sdown", "fix-slave-config", "+convert-to-slave", "+role-change"));

        TopologyRefreshMessagePredicate(String masterId) {
            this.masterId = masterId;
        }

        @Override
        public boolean test(String channel, String message) {

            // trailing spaces after the master name are not bugs
            if (channel.equals("+elected-leader") || channel.equals("+reset-master")) {
                if (message.startsWith(String.format("master %s ", masterId))) {
                    return true;
                }
            }

            if (TOPOLOGY_CHANGE_CHANNELS.contains(channel)) {
                if (message.contains(String.format("@ %s ", masterId))) {
                    return true;
                }
            }

            if (channel.equals("+switch-master")) {
                if (message.startsWith(String.format("%s ", masterId))) {
                    return true;
                }
            }

            return PROCESSING_CHANNELS.contains(channel);
        }

    }

    /**
     * {@link MessagePredicate} to check whether the channel and message contain Sentinel availability changes or a Sentinel was
     * added.
     */
    private static class SentinelReconnectMessagePredicate implements MessagePredicate {

        @Override
        public boolean test(String channel, String message) {

            if (channel.equals("+sentinel")) {
                return true;
            }

            if (channel.equals("-odown") || channel.equals("-sdown")) {
                if (message.startsWith("sentinel ")) {
                    return true;
                }
            }

            return false;
        }

    }

    interface PubSubMessageHandler {

        void handle(String source, String channel, String message);

    }

}
