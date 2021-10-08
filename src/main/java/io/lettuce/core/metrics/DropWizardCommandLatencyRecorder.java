/*
 * Copyright 2011-2021 the original author or authors.
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

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.ProtocolKeyword;
import io.netty.channel.local.LocalAddress;

/**
 * DropWizard implementation of {@link CommandLatencyRecorder}
 *
 * @author Michael Bell
 * @since 6.1
 */
public class DropWizardCommandLatencyRecorder implements CommandLatencyRecorder {

    static final String LABEL_COMMAND = "command";

    static final String METRIC_COMPLETION = "lettuce.command.completion";

    static final String METRIC_FIRST_RESPONSE = "lettuce.command.firstresponse";

    private final MetricRegistry metricRegistry;

    private final DropWizardOptions options;

    private final Map<CommandLatencyId, Timer> completionTimers = new ConcurrentHashMap<>();

    private final Map<CommandLatencyId, Timer> firstResponseTimers = new ConcurrentHashMap<>();

    /**
     * Create a new {@link DropWizardCommandLatencyRecorder} instance given {@link MetricRegistry} and {@link DropWizardOptions}.
     *
     * @param metricRegistry
     * @param options
     */
    public DropWizardCommandLatencyRecorder(MetricRegistry metricRegistry, DropWizardOptions options) {

        LettuceAssert.notNull(metricRegistry, "MetricRegistry must not be null");
        LettuceAssert.notNull(options, "DropWizardOptions must not be null");

        this.metricRegistry = metricRegistry;
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
        firstResponseTimer.update(firstResponseLatency, TimeUnit.NANOSECONDS);

        Timer completionTimer = completionTimers.computeIfAbsent(commandLatencyId, this::completionTimer);
        completionTimer.update(completionLatency, TimeUnit.NANOSECONDS);
    }

    @Override
    public boolean isEnabled() {
        return options.isEnabled();
    }

    private CommandLatencyId createId(SocketAddress local, SocketAddress remote, ProtocolKeyword commandType) {
        return CommandLatencyId.create(options.localDistinction() ? local : LocalAddress.ANY, remote, commandType);
    }

    protected Timer completionTimer(CommandLatencyId commandLatencyId) {
        return metricRegistry.register(newTimerName(METRIC_COMPLETION, commandLatencyId), newTimer());
    }

    protected Timer firstResponseTimer(CommandLatencyId commandLatencyId) {
        return metricRegistry.register(newTimerName(METRIC_FIRST_RESPONSE, commandLatencyId), newTimer());
    }

    protected Timer newTimer() {
        return new Timer(options.reservoir().get());
    }

    protected String newTimerName(String baseName, CommandLatencyId commandLatencyId) {
        final String name =  MetricRegistry.name(baseName, LABEL_COMMAND,
                commandLatencyId.commandType().name());
        return options.isIncludeAddress() ? MetricRegistry.name(name, commandLatencyId.remoteAddress().toString()) : name;
    }
}
