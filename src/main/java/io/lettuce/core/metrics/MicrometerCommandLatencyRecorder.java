/*
 * Copyright 2011-2022 the original author or authors.
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
package io.lettuce.core.metrics;

import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.ProtocolKeyword;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.netty.channel.local.LocalAddress;

/**
 * Micrometer implementation of {@link CommandLatencyRecorder}
 *
 * @author Steven Sheehy
 * @since 6.1
 */
public class MicrometerCommandLatencyRecorder implements CommandLatencyRecorder {

    static final String LABEL_COMMAND = "command";

    static final String LABEL_LOCAL = "local";

    static final String LABEL_REMOTE = "remote";

    static final String METRIC_COMPLETION = "lettuce.command.completion";

    static final String METRIC_FIRST_RESPONSE = "lettuce.command.firstresponse";

    private final MeterRegistry meterRegistry;

    private final MicrometerOptions options;

    private final Map<CommandLatencyId, Timer> completionTimers = new ConcurrentHashMap<>();

    private final Map<CommandLatencyId, Timer> firstResponseTimers = new ConcurrentHashMap<>();

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
    public void recordCommandLatency(SocketAddress local, SocketAddress remote, ProtocolKeyword protocolKeyword,
            long firstResponseLatency, long completionLatency) {

        if (!isEnabled()) {
            return;
        }

        CommandLatencyId commandLatencyId = createId(local, remote, protocolKeyword);

        Timer firstResponseTimer = firstResponseTimers.computeIfAbsent(commandLatencyId, this::firstResponseTimer);
        firstResponseTimer.record(firstResponseLatency, TimeUnit.NANOSECONDS);

        Timer completionTimer = completionTimers.computeIfAbsent(commandLatencyId, this::completionTimer);
        completionTimer.record(completionLatency, TimeUnit.NANOSECONDS);
    }

    @Override
    public boolean isEnabled() {
        return options.isEnabled();
    }

    private CommandLatencyId createId(SocketAddress local, SocketAddress remote, ProtocolKeyword commandType) {
        return CommandLatencyId.create(options.localDistinction() ? local : LocalAddress.ANY, remote, commandType);
    }

    protected Timer completionTimer(CommandLatencyId commandLatencyId) {

        Timer.Builder timer = Timer.builder(METRIC_COMPLETION)
                .description("Latency between command send and command completion (complete response received")
                .tag(LABEL_COMMAND, commandLatencyId.commandType().name())
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
                .tag(LABEL_COMMAND, commandLatencyId.commandType().name())
                .tag(LABEL_LOCAL, commandLatencyId.localAddress().toString())
                .tag(LABEL_REMOTE, commandLatencyId.remoteAddress().toString()).tags(options.tags());

        if (options.isHistogram()) {
            timer.publishPercentileHistogram().publishPercentiles(options.targetPercentiles())
                    .minimumExpectedValue(options.minLatency()).maximumExpectedValue(options.maxLatency());
        }

        return timer.register(meterRegistry);
    }

}
