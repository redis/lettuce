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
package io.lettuce.core.cluster;

import java.time.Duration;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.event.cluster.AdaptiveRefreshTriggeredEvent;
import io.lettuce.core.resource.ClientResources;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.ScheduledFuture;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Scheduler utility to schedule and initiate cluster topology refresh.
 *
 * @author Mark Paluch
 */
class ClusterTopologyRefreshScheduler implements Runnable, ClusterEventListener {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ClusterTopologyRefreshScheduler.class);

    private static final ClusterTopologyRefreshOptions FALLBACK_OPTIONS = ClusterTopologyRefreshOptions.create();

    private final Supplier<ClusterClientOptions> clientOptions;

    private final Supplier<Partitions> partitions;

    private final ClientResources clientResources;

    private final ClusterTopologyRefreshTask clusterTopologyRefreshTask;

    private final AtomicReference<Timeout> timeoutRef = new AtomicReference<>();

    private final AtomicBoolean clusterTopologyRefreshActivated = new AtomicBoolean(false);

    private final AtomicReference<ScheduledFuture<?>> clusterTopologyRefreshFuture = new AtomicReference<>();

    private final EventExecutorGroup genericWorkerPool;

    ClusterTopologyRefreshScheduler(Supplier<ClusterClientOptions> clientOptions, Supplier<Partitions> partitions,
            Supplier<CompletionStage<?>> refreshTopology, ClientResources clientResources) {

        this.clientOptions = clientOptions;
        this.partitions = partitions;
        this.clientResources = clientResources;
        this.genericWorkerPool = this.clientResources.eventExecutorGroup();
        this.clusterTopologyRefreshTask = new ClusterTopologyRefreshTask(refreshTopology);
    }

    protected void activateTopologyRefreshIfNeeded() {

        ClusterClientOptions options = clientOptions.get();
        ClusterTopologyRefreshOptions topologyRefreshOptions = options.getTopologyRefreshOptions();

        if (!topologyRefreshOptions.isPeriodicRefreshEnabled() || clusterTopologyRefreshActivated.get()) {
            return;
        }

        if (clusterTopologyRefreshActivated.compareAndSet(false, true)) {
            ScheduledFuture<?> scheduledFuture = genericWorkerPool.scheduleAtFixedRate(this,
                    options.getRefreshPeriod().toNanos(), options.getRefreshPeriod().toNanos(), TimeUnit.NANOSECONDS);
            clusterTopologyRefreshFuture.set(scheduledFuture);
        }
    }

    /**
     * Disable periodic topology refresh.
     */
    public void shutdown() {

        if (clusterTopologyRefreshActivated.compareAndSet(true, false)) {

            ScheduledFuture<?> scheduledFuture = clusterTopologyRefreshFuture.get();

            try {
                scheduledFuture.cancel(false);
                clusterTopologyRefreshFuture.set(null);
            } catch (Exception e) {
                logger.debug("Could not cancel Cluster topology refresh", e);
            }
        }
    }

    @Override
    public void run() {

        logger.debug("ClusterTopologyRefreshScheduler.run()");

        if (isEventLoopActive()) {

            if (!clientOptions.get().isRefreshClusterView()) {
                logger.debug("Periodic ClusterTopologyRefresh is disabled");
                return;
            }
        } else {
            logger.debug("Periodic ClusterTopologyRefresh is disabled");
            return;
        }

        clientResources.eventExecutorGroup().submit(clusterTopologyRefreshTask);
    }

    @Override
    public void onAskRedirection() {

        if (isEnabled(ClusterTopologyRefreshOptions.RefreshTrigger.ASK_REDIRECT)) {
            indicateTopologyRefreshSignal();
        }
    }

    @Override
    public void onMovedRedirection() {

        if (isEnabled(ClusterTopologyRefreshOptions.RefreshTrigger.MOVED_REDIRECT)) {
            if (indicateTopologyRefreshSignal()) {
                emitAdaptiveRefreshScheduledEvent();
            }
        }
    }

    @Override
    public void onReconnectAttempt(int attempt) {

        if (isEnabled(ClusterTopologyRefreshOptions.RefreshTrigger.PERSISTENT_RECONNECTS)
                && attempt >= getClusterTopologyRefreshOptions().getRefreshTriggersReconnectAttempts()) {
            if (indicateTopologyRefreshSignal()) {
                emitAdaptiveRefreshScheduledEvent();
            }
        }
    }

    @Override
    public void onUncoveredSlot(int slot) {

        if (isEnabled(ClusterTopologyRefreshOptions.RefreshTrigger.UNCOVERED_SLOT)) {
            if (indicateTopologyRefreshSignal()) {
                emitAdaptiveRefreshScheduledEvent();
            }
        }
    }

    @Override
    public void onUnknownNode() {

        if (isEnabled(ClusterTopologyRefreshOptions.RefreshTrigger.UNKNOWN_NODE)) {
            if (indicateTopologyRefreshSignal()) {
                emitAdaptiveRefreshScheduledEvent();
            }
        }
    }

    private void emitAdaptiveRefreshScheduledEvent() {

        AdaptiveRefreshTriggeredEvent event = new AdaptiveRefreshTriggeredEvent(partitions, this::scheduleRefresh);

        clientResources.eventBus().publish(event);
    }

    private boolean indicateTopologyRefreshSignal() {

        logger.debug("ClusterTopologyRefreshScheduler.indicateTopologyRefreshSignal()");

        if (!acquireTimeout()) {
            return false;
        }

        return scheduleRefresh();
    }

    private boolean scheduleRefresh() {

        if (isEventLoopActive()) {
            clientResources.eventExecutorGroup().submit(clusterTopologyRefreshTask);
            return true;
        }

        logger.debug("ClusterTopologyRefresh is disabled");
        return false;
    }

    /**
     * Check if the {@link EventExecutorGroup} is active
     *
     * @return false if the worker pool is terminating, shutdown or terminated
     */
    private boolean isEventLoopActive() {

        EventExecutorGroup eventExecutors = clientResources.eventExecutorGroup();
        if (eventExecutors.isShuttingDown() || eventExecutors.isShutdown() || eventExecutors.isTerminated()) {
            return false;
        }

        return true;
    }

    private boolean acquireTimeout() {

        Timeout existingTimeout = timeoutRef.get();

        if (existingTimeout != null) {
            if (!existingTimeout.isExpired()) {
                return false;
            }
        }

        ClusterTopologyRefreshOptions refreshOptions = getClusterTopologyRefreshOptions();
        Timeout timeout = new Timeout(refreshOptions.getAdaptiveRefreshTimeout());

        if (timeoutRef.compareAndSet(existingTimeout, timeout)) {
            return true;
        }

        return false;
    }

    private ClusterTopologyRefreshOptions getClusterTopologyRefreshOptions() {

        ClientOptions clientOptions = this.clientOptions.get();

        if (clientOptions instanceof ClusterClientOptions) {
            return ((ClusterClientOptions) clientOptions).getTopologyRefreshOptions();
        }

        return FALLBACK_OPTIONS;
    }

    private boolean isEnabled(ClusterTopologyRefreshOptions.RefreshTrigger refreshTrigger) {
        return getClusterTopologyRefreshOptions().getAdaptiveRefreshTriggers().contains(refreshTrigger);
    }

    /**
     * Value object to represent a timeout.
     *
     * @author Mark Paluch
     * @since 4.2
     */
    private class Timeout {

        private final long expiresMs;

        public Timeout(Duration duration) {
            this.expiresMs = System.currentTimeMillis() + duration.toMillis();
        }

        public boolean isExpired() {
            return expiresMs < System.currentTimeMillis();
        }

        public long remaining() {

            long diff = expiresMs - System.currentTimeMillis();
            if (diff > 0) {
                return diff;
            }
            return 0;
        }

    }

    private static class ClusterTopologyRefreshTask extends AtomicBoolean implements Runnable {

        private static final long serialVersionUID = -1337731371220365694L;

        private final Supplier<CompletionStage<?>> reloadTopologyAsync;

        ClusterTopologyRefreshTask(Supplier<CompletionStage<?>> reloadTopologyAsync) {
            this.reloadTopologyAsync = reloadTopologyAsync;
        }

        public void run() {

            if (compareAndSet(false, true)) {
                doRun();
                return;
            }

            if (logger.isDebugEnabled()) {
                logger.debug("ClusterTopologyRefreshTask already in progress");
            }
        }

        void doRun() {

            if (logger.isDebugEnabled()) {
                logger.debug("ClusterTopologyRefreshTask requesting partitions");
            }
            try {
                reloadTopologyAsync.get().whenComplete((ignore, throwable) -> {

                    if (throwable != null) {
                        logger.warn("Cannot refresh Redis Cluster topology", throwable);
                    }

                    set(false);
                });
            } catch (Exception e) {
                logger.warn("Cannot refresh Redis Cluster topology", e);
            }
        }

    }

}
