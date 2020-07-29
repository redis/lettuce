/*
 * Copyright 2019-2020 the original author or authors.
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

    public AdaptiveRefreshTriggeredEvent(Supplier<Partitions> partitionsSupplier, Runnable topologyRefreshScheduler) {
        this.partitionsSupplier = partitionsSupplier;
        this.topologyRefreshScheduler = topologyRefreshScheduler;
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

}
