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
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.netty.channel.local.LocalAddress;

import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Micrometer implementation for tracking connection metrics.
 *
 * <ul>
 * <li>Time taken from initiating connection to connection getting in active state (successful):
 * <ul>
 * <li>lettuce.connection.active.success.time</li>
 * <li>Description: Measures the time taken from initiating a connection to the point where the connection becomes active and is
 * successfully established.</li>
 * </ul>
 * </li>
 * <li>Time taken from initiating connection to connection getting in active state (failed):
 * <ul>
 * <li>lettuce.connection.active.failure.time</li>
 * <li>Description: Measures the time taken from initiating a connection until the connection attempt fails (i.e., the
 * connection doesn’t become active).</li>
 * </ul>
 * </li>
 * <li>Time from having connection disconnected till successfully reconnected:
 * <ul>
 * <li>lettuce.reconnection.success.time</li>
 * <li>Description: Measures the time between a connection being disconnected and successfully reconnected.</li>
 * </ul>
 * </li>
 * <li>Number of reconnection attempts:
 * <ul>
 * <li>lettuce.reconnection.attempts.count</li>
 * <li>Description: Tracks the number of reconnection attempts made during a disconnection.</li>
 * </ul>
 * </li>
 * </ul>
 *
 * @author Ivo Gaydajiev
 * @since 6.7
 */
public class MicrometerConnectionMonitor implements ConnectionMonitor {

    static final String LABEL_EPID = "epid";

    // Track the time between a connection being disconnected and successfully reconnected or closed
    public static final String METRIC_CONNECTION_INACTIVE_TIME = "lettuce.connection.inactive.duration";

    public static final String METRIC_CONNECTION_RECONNECTION_ATTEMPTS = "lettuce.reconnection.attempts.count";

    private final MeterRegistry meterRegistry;

    private final MicrometerOptions options;

    private final Map<MonitoredConnectionId, Timer> disconnectedTimers = new ConcurrentHashMap<>();

    private final Map<MonitoredConnectionId, Counter> reconnectionAttempts = new ConcurrentHashMap<>();

    /**
     * Create a new {@link MicrometerConnectionMonitor} instance given {@link MeterRegistry} and {@link MicrometerOptions}.
     *
     * @param meterRegistry
     * @param options
     */
    public MicrometerConnectionMonitor(MeterRegistry meterRegistry, MicrometerOptions options) {

        LettuceAssert.notNull(meterRegistry, "MeterRegistry must not be null");
        LettuceAssert.notNull(options, "MicrometerOptions must not be null");

        this.meterRegistry = meterRegistry;
        this.options = options;
    }

    @Override
    public void recordDisconnectedTime(String epid, long time) {

        if (!isEnabled()) {
            return;
        }

        MonitoredConnectionId commandLatencyId = createId(epid);

        Timer inavtiveConnectionTimer = disconnectedTimers.computeIfAbsent(commandLatencyId, this::inactiveConnectionTimer);
        inavtiveConnectionTimer.record(time, TimeUnit.NANOSECONDS);
    }

    @Override
    public void incrementReconnectionAttempts(String epid) {

        if (!isEnabled()) {
            return;
        }

        MonitoredConnectionId commandLatencyId = createId(epid);

        Counter recconectionAttemptsCounter = reconnectionAttempts.computeIfAbsent(commandLatencyId, this::reconnectAttempts);
        recconectionAttemptsCounter.increment();
    }

    @Override
    public boolean isEnabled() {
        return options.isEnabled();
    }

    private MonitoredConnectionId createId(String epId) {
        return MonitoredConnectionId.create(epId);
    }

    protected Timer inactiveConnectionTimer(MonitoredConnectionId connectionId) {
        Timer.Builder timer = Timer.builder(METRIC_CONNECTION_INACTIVE_TIME)
                .description("Time taken for successful reconnection").tag(LABEL_EPID, connectionId.epId())
                .tags(options.tags());

        if (options.isHistogram()) {
            timer.publishPercentileHistogram().publishPercentiles(options.targetPercentiles())
                    .minimumExpectedValue(options.minLatency()).maximumExpectedValue(options.maxLatency());
        }

        return timer.register(meterRegistry);
    }

    protected Counter reconnectAttempts(MonitoredConnectionId connectionId) {
        Counter.Builder timer = Counter.builder(METRIC_CONNECTION_RECONNECTION_ATTEMPTS)
                .description("Number of reconnection attempts").tag(LABEL_EPID, connectionId.epId()).tags(options.tags());

        return timer.register(meterRegistry);
    }

}
