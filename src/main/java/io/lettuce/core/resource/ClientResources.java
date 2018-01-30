/*
 * Copyright 2011-2018 the original author or authors.
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
package io.lettuce.core.resource;

import java.util.concurrent.TimeUnit;

import io.lettuce.core.event.EventBus;
import io.lettuce.core.event.EventPublisherOptions;
import io.lettuce.core.metrics.CommandLatencyCollector;
import io.netty.util.Timer;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.Future;

/**
 * Strategy interface to provide all the infrastructure building blocks like environment settings and thread pools so that the
 * client can work with it properly. {@link ClientResources} can be shared amongst multiple client instances if created outside
 * the client creation. Implementations of {@link ClientResources} are stateful and must be {@link #shutdown()} after they are
 * no longer in use.
 *
 * {@link ClientResources} provides in particular:
 * <ul>
 * <li>{@link EventLoopGroupProvider} to obtain particular {@link io.netty.channel.EventLoopGroup EventLoopGroups}</li>
 * <li>{@link EventExecutorGroup} to perform internal computation tasks</li>
 * <li>{@link Timer} for scheduling</li>
 * <li>{@link EventBus} for client event dispatching</li>
 * <li>{@link EventPublisherOptions}</li>
 * <li>{@link CommandLatencyCollector} to collect latency details. Requires the {@literal HdrHistogram} library.</li>
 * <li>{@link DnsResolver} to collect latency details. Requires the {@literal LatencyUtils} library.</li>
 * <li>Reconnect {@link Delay}.</li>
 * </ul>
 *
 * @author Mark Paluch
 * @since 3.4
 * @see DefaultClientResources
 */
public interface ClientResources {

    /**
     * Shutdown the {@link ClientResources}.
     *
     * @return eventually the success/failure of the shutdown without errors.
     */
    Future<Boolean> shutdown();

    /**
     * Shutdown the {@link ClientResources}.
     *
     * @param quietPeriod the quiet period as described in the documentation
     * @param timeout the maximum amount of time to wait until the executor is shutdown regardless if a task was submitted
     *        during the quiet period
     * @param timeUnit the unit of {@code quietPeriod} and {@code timeout}
     * @return eventually the success/failure of the shutdown without errors.
     */
    Future<Boolean> shutdown(long quietPeriod, long timeout, TimeUnit timeUnit);

    /**
     * Returns the {@link EventLoopGroupProvider} that provides access to the particular {@link io.netty.channel.EventLoopGroup
     * event loop groups}. lettuce requires at least two implementations: {@link io.netty.channel.nio.NioEventLoopGroup} for
     * TCP/IP connections and {@link io.netty.channel.epoll.EpollEventLoopGroup} for unix domain socket connections (epoll).
     *
     * You can use {@link DefaultEventLoopGroupProvider} as default implementation or implement an own
     * {@link EventLoopGroupProvider} to share existing {@link io.netty.channel.EventLoopGroup EventLoopGroup's} with lettuce.
     *
     * @return the {@link EventLoopGroupProvider} which provides access to the particular
     *         {@link io.netty.channel.EventLoopGroup event loop groups}
     */
    EventLoopGroupProvider eventLoopGroupProvider();

    /**
     * Returns the computation pool used for internal operations. Such tasks are periodic Redis Cluster and Redis Sentinel
     * topology updates and scheduling of connection reconnection by {@link io.lettuce.core.protocol.ConnectionWatchdog}.
     *
     * @return the computation pool used for internal operations
     */
    EventExecutorGroup eventExecutorGroup();

    /**
     * Returns the pool size (number of threads) for IO threads. The indicated size does not reflect the number for all IO
     * threads. TCP and socket connections (epoll) require different IO pool.
     *
     * @return the pool size (number of threads) for all IO tasks.
     */
    int ioThreadPoolSize();

    /**
     * Returns the pool size (number of threads) for all computation tasks.
     *
     * @return the pool size (number of threads to use).
     */
    int computationThreadPoolSize();

    /**
     * Returns the {@link Timer} to schedule events. A timer object may run single- or multi-threaded but must be used for
     * scheduling of short-running jobs only. Long-running jobs should be scheduled and executed using
     * {@link #eventExecutorGroup()}.
     *
     * @return the timer.
     * @since 4.3
     */
    Timer timer();

    /**
     * Returns the event bus used to publish events.
     *
     * @return the event bus
     */
    EventBus eventBus();

    /**
     * Returns the {@link EventPublisherOptions} for latency event publishing.
     *
     * @return the {@link EventPublisherOptions} for latency event publishing
     */
    EventPublisherOptions commandLatencyPublisherOptions();

    /**
     * Returns the {@link CommandLatencyCollector}.
     *
     * @return the command latency collector
     */
    CommandLatencyCollector commandLatencyCollector();

    /**
     * Returns the {@link DnsResolver}.
     *
     * @return the DNS resolver
     * @since 4.3
     */
    DnsResolver dnsResolver();

    /**
     * Returns the {@link Delay} for reconnect attempts. May return a different instance on each call.
     *
     * @return the reconnect {@link Delay}.
     * @since 4.3
     */
    Delay reconnectDelay();

    /**
     * Returns the {@link NettyCustomizer} to customize netty components.
     *
     * @return the configured {@link NettyCustomizer}.
     * @since 4.4
     */
    NettyCustomizer nettyCustomizer();
}
