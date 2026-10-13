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

import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.ProtocolKeyword;
import io.lettuce.core.protocol.RedisCommand;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.netty.channel.local.LocalAddress;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Micrometer implementation of {@link CommandLatencyRecorder}.
 * <p>
 * The recorder registers a pair of {@link Timer}s for each distinct remote address and command name (see
 * {@link ProtocolKeyword#toString()}) and retains them for its lifetime. The number of registered timer pairs is limited by
 * {@link MicrometerOptions#maxCommandLatencyIds()}.
 *
 * @author Steven Sheehy
 * @author shariorfarhan07 (Sharior Hossain Farhan)
 * @since 6.1
 */
public class MicrometerCommandLatencyRecorder implements CommandLatencyRecorder {

    static final String LABEL_COMMAND = "command";

    static final String LABEL_LOCAL = "local";

    static final String LABEL_REMOTE = "remote";

    static final String METRIC_COMPLETION = "lettuce.command.completion";

    static final String METRIC_FIRST_RESPONSE = "lettuce.command.firstresponse";

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(MicrometerCommandLatencyRecorder.class);

    private final MeterRegistry meterRegistry;

    private final MicrometerOptions options;

    private final Map<CommandLatencyId, Timer> completionTimers = new ConcurrentHashMap<>();

    private final Map<CommandLatencyId, Timer> firstResponseTimers = new ConcurrentHashMap<>();

    private final AtomicBoolean maxCommandLatencyIdsReachedLogged = new AtomicBoolean();

    /**
     * Create a new {@link MicrometerCommandLatencyRecorder} instance given {@link MeterRegistry} and {@link MicrometerOptions}.
     *
     * @param meterRegistry
     * @param options
     */
    public MicrometerCommandLatencyRecorder(MeterRegistry meterRegistry, MicrometerOptions options) {

        LettuceAssert.notNull(meterRegistry, "MeterRegistry must not be null");
        LettuceAssert.notNull(options, "MicrometerOptions must not be null");

        this.meterRegistry = meterRegistry;
        this.options = options;
    }

    @Override
    public void recordCommandLatency(SocketAddress local, SocketAddress remote, RedisCommand<?, ?, ?> redisCommand,
            long firstResponseLatency, long completionLatency) {

        if (isEnabled() && isCommandEnabled(redisCommand)) {
            recordCommandLatency(local, remote, redisCommand.getType(), firstResponseLatency, completionLatency);
        }
    }

    @Override
    public void recordCommandLatency(SocketAddress local, SocketAddress remote, ProtocolKeyword commandType,
            long firstResponseLatency, long completionLatency) {

        if (!isEnabled()) {
            return;
        }

        CommandLatencyId commandLatencyId = createId(local, remote, commandType);

        Timer completionTimer = completionTimers.get(commandLatencyId);

        if (completionTimer == null) {

            if (completionTimers.size() >= options.maxCommandLatencyIds()) {
                logMaxCommandLatencyIdsReached();
                return;
            }

            completionTimer = completionTimers.computeIfAbsent(commandLatencyId, this::completionTimer);
        }

        Timer firstResponseTimer = firstResponseTimers.computeIfAbsent(commandLatencyId, this::firstResponseTimer);
        firstResponseTimer.record(firstResponseLatency, TimeUnit.NANOSECONDS);
        completionTimer.record(completionLatency, TimeUnit.NANOSECONDS);
    }

    private void logMaxCommandLatencyIdsReached() {

        if (maxCommandLatencyIdsReachedLogged.compareAndSet(false, true)) {
            logger.warn("Command latency recording reached the limit of {} distinct remote address and command type "
                    + "combinations; latencies of new combinations are not recorded. Ensure that custom ProtocolKeyword "
                    + "implementations return a stable command name from toString() or raise "
                    + "MicrometerOptions.maxCommandLatencyIds", options.maxCommandLatencyIds());
        }
    }

    @Override
    public boolean isEnabled() {
        return options.isEnabled();
    }

    private boolean isCommandEnabled(RedisCommand<?, ?, ?> redisCommand) {
        return options.getMetricsFilter().test(redisCommand);
    }

    private CommandLatencyId createId(SocketAddress local, SocketAddress remote, ProtocolKeyword commandType) {
        return CommandLatencyId.create(options.localDistinction() ? local : LocalAddress.ANY, remote, commandType);
    }

    protected Timer completionTimer(CommandLatencyId commandLatencyId) {

        Timer.Builder timer = Timer.builder(METRIC_COMPLETION)
                .description("Latency between command send and command completion (complete response received")
                .tag(LABEL_COMMAND, commandLatencyId.commandType().toString())
                .tag(LABEL_LOCAL, commandLatencyId.localAddress().toString())
                .tag(LABEL_REMOTE, commandLatencyId.remoteAddress().toString()).tags(options.tags());

        if (options.isHistogram()) {
            timer.publishPercentileHistogram().publishPercentiles(options.targetPercentiles())
                    .minimumExpectedValue(options.minLatency()).maximumExpectedValue(options.maxLatency());
        }

        return timer.register(meterRegistry);
    }

    protected Timer firstResponseTimer(CommandLatencyId commandLatencyId) {

        Timer.Builder timer = Timer.builder(METRIC_FIRST_RESPONSE)
                .description("Latency between command send and first response (first response received)")
                .tag(LABEL_COMMAND, commandLatencyId.commandType().toString())
                .tag(LABEL_LOCAL, commandLatencyId.localAddress().toString())
                .tag(LABEL_REMOTE, commandLatencyId.remoteAddress().toString()).tags(options.tags());

        if (options.isHistogram()) {
            timer.publishPercentileHistogram().publishPercentiles(options.targetPercentiles())
                    .minimumExpectedValue(options.minLatency()).maximumExpectedValue(options.maxLatency());
        }

        return timer.register(meterRegistry);
    }

}
