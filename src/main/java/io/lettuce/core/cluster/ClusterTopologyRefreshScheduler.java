/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
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

import static io.lettuce.core.event.cluster.AdaptiveRefreshTriggeredEvent.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.event.cluster.AdaptiveRefreshTriggeredEvent;
import io.lettuce.core.protocol.MaintenanceNotification.SlotMigration;
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

    /**
     * @param refreshTopology reloads the topology from the regular refresh sources
     * @param applyTopologyUpdate applies the slot migrations of a maintenance notification to the topology
     */
    ClusterTopologyRefreshScheduler(Supplier<ClusterClientOptions> clientOptions, Supplier<Partitions> partitions,
            Supplier<CompletionStage<?>> refreshTopology, Function<List<SlotMigration>, CompletionStage<?>> applyTopologyUpdate,
            ClientResources clientResources) {

        this.clientOptions = clientOptions;
        this.partitions = partitions;
        this.clientResources = clientResources;
        this.genericWorkerPool = this.clientResources.eventExecutorGroup();
        this.clusterTopologyRefreshTask = new ClusterTopologyRefreshTask(refreshTopology, applyTopologyUpdate,
                this::scheduleTopologyUpdate);
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
     * Suspend (cancel) periodic topology refresh.
     */
    public void suspendTopologyRefresh() {

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

    public boolean isTopologyRefreshInProgress() {
        return clusterTopologyRefreshTask.get();
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
            if (indicateTopologyRefreshSignal()) {
                emitAdaptiveRefreshScheduledEvent(ClusterTopologyRefreshOptions.RefreshTrigger.ASK_REDIRECT);
            }
        }
    }

    @Override
    public void onMovedRedirection() {

        if (isEnabled(ClusterTopologyRefreshOptions.RefreshTrigger.MOVED_REDIRECT)) {
            if (indicateTopologyRefreshSignal()) {
                emitAdaptiveRefreshScheduledEvent(ClusterTopologyRefreshOptions.RefreshTrigger.MOVED_REDIRECT);
            }
        }
    }

    @Override
    public void onReconnectAttempt(int attempt) {

        if (isEnabled(ClusterTopologyRefreshOptions.RefreshTrigger.PERSISTENT_RECONNECTS)
                && attempt >= getClusterTopologyRefreshOptions().getRefreshTriggersReconnectAttempts()) {
            if (indicateTopologyRefreshSignal()) {
                emitPersistentReconnectAdaptiveRefreshScheduledEvent(attempt);
            }
        }
    }

    @Override
    public void onUncoveredSlot(int slot) {

        if (isEnabled(ClusterTopologyRefreshOptions.RefreshTrigger.UNCOVERED_SLOT)) {
            if (indicateTopologyRefreshSignal()) {
                emitUncoveredSlotAdaptiveRefreshScheduledEvent(slot);
            }
        }
    }

    @Override
    public void onUnknownNode() {

        if (isEnabled(ClusterTopologyRefreshOptions.RefreshTrigger.UNKNOWN_NODE)) {
            if (indicateTopologyRefreshSignal()) {
                emitAdaptiveRefreshScheduledEvent(ClusterTopologyRefreshOptions.RefreshTrigger.UNKNOWN_NODE);
            }
        }
    }

    private void emitAdaptiveRefreshScheduledEvent(ClusterTopologyRefreshOptions.RefreshTrigger trigger) {
        logger.debug("Adaptive refresh event due to: {}", trigger);

        AdaptiveRefreshTriggeredEvent event = new AdaptiveRefreshTriggeredEvent(partitions, this::scheduleRefresh, trigger);

        clientResources.eventBus().publish(event);
    }

    private void emitPersistentReconnectAdaptiveRefreshScheduledEvent(int attempt) {
        logger.debug("Adaptive refresh event due to: {} attempt {}",
                ClusterTopologyRefreshOptions.RefreshTrigger.PERSISTENT_RECONNECTS, attempt);

        AdaptiveRefreshTriggeredEvent event = new PersistentReconnectsAdaptiveRefreshTriggeredEvent(partitions,
                this::scheduleRefresh, attempt);

        clientResources.eventBus().publish(event);
    }

    private void emitUncoveredSlotAdaptiveRefreshScheduledEvent(int slot) {
        logger.debug("Adaptive refresh event due to: {} for slot {}",
                ClusterTopologyRefreshOptions.RefreshTrigger.UNCOVERED_SLOT, slot);

        AdaptiveRefreshTriggeredEvent event = new UncoveredSlotAdaptiveRefreshTriggeredEvent(partitions, this::scheduleRefresh,
                slot);

        clientResources.eventBus().publish(event);
    }

    private boolean indicateTopologyRefreshSignal() {

        logger.debug("ClusterTopologyRefreshScheduler.indicateTopologyRefreshSignal()");

        if (!acquireTimeout()) {
            return false;
        }

        return scheduleRefresh();
    }

    /**
     * Request a topology update on behalf of a maintenance notification. Unlike adaptive refresh, the request is neither
     * subject to the adaptive refresh triggers nor to the adaptive refresh timeout. Updates run one at a time in the order of
     * their sequence ids, after a refresh in progress.
     *
     * @param seqId the sequence id of the notification
     * @param migrations the slot migrations of the notification, may be empty
     * @return {@code true} if the update was accepted
     */
    boolean requestTopologyUpdate(long seqId, List<SlotMigration> migrations) {

        logger.debug("ClusterTopologyRefreshScheduler.requestTopologyUpdate({}, {})", seqId, migrations);

        if (!isEventLoopActive()) {
            logger.debug("ClusterTopologyRefresh is disabled");
            return false;
        }

        clusterTopologyRefreshTask.addTopologyUpdate(seqId, migrations);
        return scheduleTopologyUpdate();
    }

    private boolean scheduleTopologyUpdate() {

        if (isEventLoopActive()) {
            clientResources.eventExecutorGroup().submit(clusterTopologyRefreshTask.topologyUpdateRun);
            return true;
        }

        logger.debug("ClusterTopologyRefresh is disabled");
        return false;
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

        private final Function<List<SlotMigration>, CompletionStage<?>> applyTopologyUpdate;

        private final Runnable scheduleTopologyUpdate;

        final Runnable topologyUpdateRun = this::runTopologyUpdate;

        // topology updates not yet run, by sequence id; guarded by itself
        private final TreeMap<Long, List<SlotMigration>> pendingUpdates = new TreeMap<>();

        ClusterTopologyRefreshTask(Supplier<CompletionStage<?>> reloadTopologyAsync,
                Function<List<SlotMigration>, CompletionStage<?>> applyTopologyUpdate, Runnable scheduleTopologyUpdate) {
            this.reloadTopologyAsync = reloadTopologyAsync;
            this.applyTopologyUpdate = applyTopologyUpdate;
            this.scheduleTopologyUpdate = scheduleTopologyUpdate;
        }

        void addTopologyUpdate(long seqId, List<SlotMigration> migrations) {
            synchronized (pendingUpdates) {
                pendingUpdates.put(seqId, migrations);
            }
        }

        private List<SlotMigration> pollTopologyUpdate() {
            synchronized (pendingUpdates) {
                Map.Entry<Long, List<SlotMigration>> first = pendingUpdates.pollFirstEntry();
                return first != null ? first.getValue() : null;
            }
        }

        private boolean hasPendingUpdates() {
            synchronized (pendingUpdates) {
                return !pendingUpdates.isEmpty();
            }
        }

        public void run() {

            if (compareAndSet(false, true)) {
                doRun(reloadTopologyAsync);
                return;
            }

            if (logger.isDebugEnabled()) {
                logger.debug("ClusterTopologyRefreshTask already in progress");
            }
        }

        private void runTopologyUpdate() {

            if (!compareAndSet(false, true)) {
                // scheduled again once the run in progress completes
                return;
            }

            List<SlotMigration> migrations = pollTopologyUpdate();

            if (migrations == null) {
                complete();
                return;
            }

            doRun(() -> applyTopologyUpdate.apply(migrations));
        }

        void doRun(Supplier<CompletionStage<?>> topologyUpdate) {

            if (logger.isDebugEnabled()) {
                logger.debug("ClusterTopologyRefreshTask requesting partitions");
            }
            try {
                topologyUpdate.get().whenComplete((ignore, throwable) -> {

                    if (throwable != null) {
                        logger.warn("Cannot refresh Redis Cluster topology", throwable);
                    }

                    complete();
                });
            } catch (Exception e) {
                logger.warn("Cannot refresh Redis Cluster topology", e);
                complete();
            }
        }

        private void complete() {

            set(false);

            if (hasPendingUpdates()) {
                scheduleTopologyUpdate.run();
            }
        }

    }

}
