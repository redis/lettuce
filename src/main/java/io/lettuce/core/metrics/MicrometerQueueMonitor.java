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
package io.lettuce.core.metrics;

import io.lettuce.core.internal.LettuceAssert;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.function.Supplier;

public class MicrometerQueueMonitor implements EndpointQueueMonitor {

    public static final String LABEL_EPID = "epId";

    private final MeterRegistry meterRegistry;

    private final MicrometerOptions options;

    /**
     * Create a new {@link MicrometerQueueMonitor} instance given {@link MeterRegistry} and {@link MicrometerOptions}.
     *
     * @param meterRegistry
     * @param options
     */
    public MicrometerQueueMonitor(MeterRegistry meterRegistry, MicrometerOptions options) {

        LettuceAssert.notNull(meterRegistry, "MeterRegistry must not be null");
        LettuceAssert.notNull(options, "MicrometerOptions must not be null");

        this.meterRegistry = meterRegistry;
        this.options = options;
    }

    @Override
    public boolean isEnabled() {
        return options.isEnabled();
    }

    @Override
    public void observeQueueSize(QueueId queueId, Supplier<Number> queueSizeSupplier) {
        if (!isEnabled()) {
            return;
        }

        Gauge.builder(queueId.getQueueName(), queueSizeSupplier).description("Queue size").tag(LABEL_EPID, queueId.getEpId())
                .tags(options.tags()).register(meterRegistry);
    }

}
