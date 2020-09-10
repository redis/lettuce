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
package io.lettuce.core.protocol;

import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.lettuce.core.event.DefaultEventPublisherOptions;
import io.lettuce.core.event.EventBus;
import io.lettuce.core.event.EventPublisherOptions;
import io.lettuce.core.metrics.CommandLatencyCollector;
import io.lettuce.core.metrics.CommandLatencyId;
import io.lettuce.core.metrics.CommandLatencyRecorder;
import io.lettuce.core.metrics.CommandMetrics;
import io.lettuce.core.resource.*;
import io.lettuce.core.tracing.Tracing;
import io.netty.util.Timer;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.SucceededFuture;

/**
 * @author Mark Paluch
 */
public class EmptyClientResources implements ClientResources {

    private static final DefaultEventPublisherOptions PUBLISHER_OPTIONS = DefaultEventPublisherOptions.disabled();
    private static final EmptyCommandLatencyCollector LATENCY_COLLECTOR = new EmptyCommandLatencyCollector();
    public static final EmptyClientResources INSTANCE = new EmptyClientResources();

    @Override
    public Future<Boolean> shutdown() {
        return new SucceededFuture<>(GlobalEventExecutor.INSTANCE, true);
    }

    @Override
    public Future<Boolean> shutdown(long quietPeriod, long timeout, TimeUnit timeUnit) {
        return new SucceededFuture<>(GlobalEventExecutor.INSTANCE, true);
    }

    @Override
    public Builder mutate() {
        return null;
    }

    @Override
    public EventLoopGroupProvider eventLoopGroupProvider() {
        return null;
    }

    @Override
    public EventExecutorGroup eventExecutorGroup() {
        return null;
    }

    @Override
    public int ioThreadPoolSize() {
        return 0;
    }

    @Override
    public int computationThreadPoolSize() {
        return 0;
    }

    @Override
    public Timer timer() {
        return null;
    }

    @Override
    public EventBus eventBus() {
        return null;
    }

    @Override
    public EventPublisherOptions commandLatencyPublisherOptions() {
        return PUBLISHER_OPTIONS;
    }

    @Override
    public CommandLatencyRecorder commandLatencyRecorder() {
        return LATENCY_COLLECTOR;
    }

    @Override
    public DnsResolver dnsResolver() {
        return null;
    }

    @Override
    public SocketAddressResolver socketAddressResolver() {
        return null;
    }

    @Override
    public Delay reconnectDelay() {
        return null;
    }

    @Override
    public NettyCustomizer nettyCustomizer() {
        return null;
    }

    @Override
    public Tracing tracing() {
        return Tracing.disabled();
    }

    public static class EmptyCommandLatencyCollector implements CommandLatencyCollector {

        @Override
        public void shutdown() {

        }

        @Override
        public Map<CommandLatencyId, CommandMetrics> retrieveMetrics() {
            return null;
        }

        @Override
        public boolean isEnabled() {
            return false;
        }

        @Override
        public void recordCommandLatency(SocketAddress local, SocketAddress remote, ProtocolKeyword commandType,
                long firstResponseLatency, long completionLatency) {

        }
    }
}
