/*
 * Copyright 2011-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lambdaworks.redis.protocol;

import java.util.concurrent.TimeUnit;

import com.lambdaworks.redis.event.DefaultEventPublisherOptions;
import com.lambdaworks.redis.event.EventBus;
import com.lambdaworks.redis.event.EventPublisherOptions;
import com.lambdaworks.redis.metrics.CommandLatencyCollector;
import com.lambdaworks.redis.metrics.DefaultCommandLatencyCollector;
import com.lambdaworks.redis.resource.*;

import io.netty.util.Timer;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.concurrent.SucceededFuture;

/**
 * @author Mark Paluch
 */
public class EmptyClientResources implements ClientResources {

    public static final DefaultEventPublisherOptions PUBLISHER_OPTIONS = DefaultEventPublisherOptions.disabled();
    public static final CommandLatencyCollector LATENCY_COLLECTOR = DefaultCommandLatencyCollector.disabled();
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
    public CommandLatencyCollector commandLatencyCollector() {
        return LATENCY_COLLECTOR;
    }

    @Override
    public DnsResolver dnsResolver() {
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
}
