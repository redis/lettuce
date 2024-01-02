/*
 * Copyright 2019-2024 the original author or authors.
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
package io.lettuce.core.event.cluster;

import java.util.function.Supplier;

import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.event.Event;

/**
 * Event when a topology refresh is about to start by an adaptive refresh trigger.
 *
 * @author Mark Paluch
 * @since 5.2
 */
public class AdaptiveRefreshTriggeredEvent implements Event {

    private final Supplier<Partitions> partitionsSupplier;

    private final Runnable topologyRefreshScheduler;

    private final ClusterTopologyRefreshOptions.RefreshTrigger refreshTrigger;

    public AdaptiveRefreshTriggeredEvent(Supplier<Partitions> partitionsSupplier, Runnable topologyRefreshScheduler,
            ClusterTopologyRefreshOptions.RefreshTrigger refreshTrigger) {
        this.partitionsSupplier = partitionsSupplier;
        this.topologyRefreshScheduler = topologyRefreshScheduler;
        this.refreshTrigger = refreshTrigger;
    }

    /**
     * Schedules a new topology refresh. Refresh happens asynchronously.
     */
    public void scheduleRefresh() {
        topologyRefreshScheduler.run();
    }

    /**
     * Retrieve the currently known partitions.
     *
     * @return the currently known topology view. The view is mutable and changes over time.
     */
    public Partitions getPartitions() {
        return partitionsSupplier.get();
    }

    /**
     * Retrieve the {@link ClusterTopologyRefreshOptions.RefreshTrigger} that caused this event.
     *
     * @return the {@link ClusterTopologyRefreshOptions.RefreshTrigger} that caused this event.
     */
    public ClusterTopologyRefreshOptions.RefreshTrigger getRefreshTrigger() {
        return refreshTrigger;
    }

    /**
     * Extension to {@link AdaptiveRefreshTriggeredEvent} providing the reconnect-attempt counter value.
     *
     * @since 6.2.3
     */
    public static class PersistentReconnectsAdaptiveRefreshTriggeredEvent extends AdaptiveRefreshTriggeredEvent {

        private final int attempt;

        public PersistentReconnectsAdaptiveRefreshTriggeredEvent(Supplier<Partitions> partitionsSupplier,
                Runnable topologyRefreshScheduler, int attempt) {
            super(partitionsSupplier, topologyRefreshScheduler,
                    ClusterTopologyRefreshOptions.RefreshTrigger.PERSISTENT_RECONNECTS);
            this.attempt = attempt;
        }

        /**
         * Return the reconnection-attempt at which this event was emitted.
         *
         * @return the reconnection-attempt at which this event was emitted.
         */
        public int getAttempt() {
            return attempt;
        }

    }

    /**
     * Extension to {@link AdaptiveRefreshTriggeredEvent} providing the uncovered slot value.
     *
     * @since 6.2.3
     */
    public static class UncoveredSlotAdaptiveRefreshTriggeredEvent extends AdaptiveRefreshTriggeredEvent {

        private final int slot;

        public UncoveredSlotAdaptiveRefreshTriggeredEvent(Supplier<Partitions> partitionsSupplier,
                Runnable topologyRefreshScheduler, int slot) {
            super(partitionsSupplier, topologyRefreshScheduler, ClusterTopologyRefreshOptions.RefreshTrigger.UNCOVERED_SLOT);
            this.slot = slot;
        }

        /**
         * Return the slot that is not covered.
         *
         * @return the slot that is not covered.
         */
        public int getSlot() {
            return slot;
        }

    }

}
