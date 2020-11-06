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
package io.lettuce.core.metrics;

import io.lettuce.core.protocol.ProtocolKeyword;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.net.SocketAddress;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author Steven Sheehy
 */
public class MicrometerCommandLatencyCollector implements CommandLatencyCollector {

    private static final Duration MIN_LATENCY = Duration.ofMillis(1L);

    private static final Duration MAX_LATENCY = Duration.ofMinutes(5L);

    static final String COMPLETION_METRIC = "lettuce.command.completion";

    static final String FIRST_RESPONSE_METRIC = "lettuce.command.firstresponse";

    static final String COMMAND_LABEL = "command";

    private final boolean enableHistogram;

    private final MeterRegistry meterRegistry;

    private final Map<ProtocolKeyword, Timer> completionTimers = new ConcurrentHashMap<>();

    private final Map<ProtocolKeyword, Timer> firstResponseTimers = new ConcurrentHashMap<>();

    public MicrometerCommandLatencyCollector(MeterRegistry meterRegistry, boolean enableHistogram) {
        this.enableHistogram = enableHistogram;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void recordCommandLatency(SocketAddress local, SocketAddress remote, ProtocolKeyword protocolKeyword,
            long firstResponseLatency, long completionLatency) {

        Timer firstResponseTimer = firstResponseTimers.computeIfAbsent(protocolKeyword, this::firstResponseTimer);
        firstResponseTimer.record(firstResponseLatency, TimeUnit.NANOSECONDS);

        Timer completionTimer = completionTimers.computeIfAbsent(protocolKeyword, this::completionTimer);
        completionTimer.record(completionLatency, TimeUnit.NANOSECONDS);
    }

    private Timer completionTimer(ProtocolKeyword protocolKeyword) {
        Timer.Builder timer = Timer.builder(COMPLETION_METRIC)
                .description("Latency between command send and command completion (complete response received")
                .tag(COMMAND_LABEL, protocolKeyword.name());

        if (enableHistogram) {
            timer.publishPercentileHistogram()
                    .minimumExpectedValue(MIN_LATENCY)
                    .maximumExpectedValue(MAX_LATENCY);
        }

        return timer.register(meterRegistry);
    }

    private Timer firstResponseTimer(ProtocolKeyword protocolKeyword) {
        Timer.Builder timer = Timer.builder(FIRST_RESPONSE_METRIC)
                .description("Latency between command send and first response (first response received)")
                .tag(COMMAND_LABEL, protocolKeyword.name());

        if (enableHistogram) {
            timer.publishPercentileHistogram()
                    .minimumExpectedValue(MIN_LATENCY)
                    .maximumExpectedValue(MAX_LATENCY);
        }

        return timer.register(meterRegistry);
    }

    @Override
    public void shutdown() {
    }

    @Override
    public Map<CommandLatencyId, CommandMetrics> retrieveMetrics() {
        return Collections.emptyMap();
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
