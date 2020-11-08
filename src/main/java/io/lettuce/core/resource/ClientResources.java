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
package io.lettuce.core.resource;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import io.lettuce.core.event.EventBus;
import io.lettuce.core.event.EventPublisherOptions;
import io.lettuce.core.metrics.CommandLatencyCollector;
import io.lettuce.core.metrics.CommandLatencyCollectorOptions;
import io.lettuce.core.metrics.CommandLatencyRecorder;
import io.lettuce.core.tracing.Tracing;
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
 * <li>{@link CommandLatencyRecorder} to collect latency details. Enabled using
 * {@link io.lettuce.core.metrics.DefaultCommandLatencyCollector} when {@literal HdrHistogram} is on the classpath.</li>
 * <li>{@link EventBus} for client event dispatching</li>
 * <li>{@link EventLoopGroupProvider} to obtain particular {@link io.netty.channel.EventLoopGroup EventLoopGroups}</li>
 * <li>{@link EventExecutorGroup} to perform internal computation tasks</li>
 * <li>Reconnect {@link Delay}.</li>
 * <li>{@link DnsResolver} to collect latency details. Requires the {@literal LatencyUtils} library.</li>
 * <li>{@link Timer} for scheduling</li>
 * <li>{@link Tracing} to trace Redis commands.</li>
 * </ul>
 *
 * @author Mark Paluch
 * @author Mikhael Sokolov
 * @since 3.4
 * @see DefaultClientResources
 */
public interface ClientResources {

    /**
     * Create a new {@link ClientResources} using default settings.
     *
     * @return a new instance of a default client resources.
     */
    static ClientResources create() {
        return DefaultClientResources.create();
    }

    /**
     * Create a new {@link ClientResources} using default settings.
     *
     * @return a new instance of a default client resources.
     */
    static Builder builder() {
        return DefaultClientResources.builder();
    }

    /**
     * Builder for {@link ClientResources}.
     *
     * @since 5.1
     */
    interface Builder {

        /**
         * Sets the {@link CommandLatencyCollector} that can that can be used across different instances of the RedisClient.
         *
         * @param commandLatencyCollector the command latency collector, must not be {@code null}.
         * @return {@code this} {@link Builder}.
         * @deprecated since 6.0, use {@link #commandLatencyRecorder(CommandLatencyRecorder)} instead.
         */
        @Deprecated
        default Builder commandLatencyCollector(CommandLatencyCollector commandLatencyCollector) {
            return commandLatencyRecorder(commandLatencyCollector);
        }

        /**
         * Sets the {@link CommandLatencyRecorder} that can that can be used across different instances of the RedisClient.
         *
         * @param latencyRecorder the command latency recorder, must not be {@code null}.
         * @return {@code this} {@link Builder}.
         * @since 6.0
         */
        Builder commandLatencyRecorder(CommandLatencyRecorder latencyRecorder);

        /**
         * Sets the {@link CommandLatencyCollectorOptions} that can that can be used across different instances of the
         * RedisClient. The options are only effective if no {@code commandLatencyCollector} is provided.
         *
         * @param commandLatencyCollectorOptions the command latency collector options, must not be {@code null}.
         * @return {@code this} {@link Builder}.
         * @deprecated since 6.0. Configure {@link io.lettuce.core.metrics.CommandLatencyRecorder} directly using
         *             {@link CommandLatencyCollectorOptions}.
         */
        @Deprecated
        Builder commandLatencyCollectorOptions(CommandLatencyCollectorOptions commandLatencyCollectorOptions);

        /**
         * Sets the {@link EventPublisherOptions} to publish command latency metrics using the {@link EventBus} if the
         * {@link CommandLatencyRecorder} is an instance of {@link CommandLatencyCollector} that allows latency metric
         * retrieval.
         *
         * @param commandLatencyPublisherOptions the {@link EventPublisherOptions} to publish command latency metrics using the
         *        {@link EventBus}, must not be {@code null}.
         * @return {@code this} {@link Builder}.
         */
        Builder commandLatencyPublisherOptions(EventPublisherOptions commandLatencyPublisherOptions);

        /**
         * Sets the thread pool size (number of threads to use) for computation operations (default value is the number of
         * CPUs). The thread pool size is only effective if no {@code eventExecutorGroup} is provided.
         *
         * @param computationThreadPoolSize the thread pool size, must be greater {@code 0}.
         * @return {@code this} {@link Builder}.
         */
        Builder computationThreadPoolSize(int computationThreadPoolSize);

        /**
         * Sets the {@link DnsResolver} that is used to resolve hostnames to {@link java.net.InetAddress}. Defaults to
         * {@link DnsResolvers#JVM_DEFAULT}
         *
         * @param dnsResolver the DNS resolver, must not be {@code null}.
         * @return {@code this} {@link Builder}.
         * @since 4.3
         */
        Builder dnsResolver(DnsResolver dnsResolver);

        /**
         * Sets the {@link EventBus} that can that can be used across different instances of the RedisClient.
         *
         * @param eventBus the event bus, must not be {@code null}.
         * @return {@code this} {@link Builder}.
         */
        Builder eventBus(EventBus eventBus);

        /**
         * Sets a shared {@link EventExecutorGroup event executor group} that can be used across different instances of
         * {@link io.lettuce.core.RedisClient} and {@link io.lettuce.core.cluster.RedisClusterClient}. The provided
         * {@link EventExecutorGroup} instance will not be shut down when shutting down the client resources. You have to take
         * care of that. This is an advanced configuration that should only be used if you know what you are doing.
         *
         * @param eventExecutorGroup the shared eventExecutorGroup, must not be {@code null}.
         * @return {@code this} {@link Builder}.
         */
        Builder eventExecutorGroup(EventExecutorGroup eventExecutorGroup);

        /**
         * Sets a shared {@link EventLoopGroupProvider event executor provider} that can be used across different instances of
         * {@link io.lettuce.core.RedisClient} and {@link io.lettuce.core.cluster.RedisClusterClient}. The provided
         * {@link EventLoopGroupProvider} instance will not be shut down when shutting down the client resources. You have to
         * take care of that. This is an advanced configuration that should only be used if you know what you are doing.
         *
         * @param eventLoopGroupProvider the shared eventLoopGroupProvider, must not be {@code null}.
         * @return {@code this} {@link Builder}.
         */
        Builder eventLoopGroupProvider(EventLoopGroupProvider eventLoopGroupProvider);

        /**
         * Sets the thread pool size (number of threads to use) for I/O operations (default value is the number of CPUs). The
         * thread pool size is only effective if no {@code eventLoopGroupProvider} is provided.
         *
         * @param ioThreadPoolSize the thread pool size, must be greater {@code 0}.
         * @return {@code this} {@link Builder}.
         */
        Builder ioThreadPoolSize(int ioThreadPoolSize);

        /**
         * Sets the {@link NettyCustomizer} instance to customize netty components during connection.
         *
         * @param nettyCustomizer the netty customizer instance, must not be {@code null}.
         * @return this
         * @since 4.4
         */
        Builder nettyCustomizer(NettyCustomizer nettyCustomizer);

        /**
         * Sets the stateless reconnect {@link Delay} to delay reconnect attempts. Defaults to binary exponential delay capped
         * at {@literal 30 SECONDS}. {@code reconnectDelay} must be a stateless {@link Delay}.
         *
         * @param reconnectDelay the reconnect delay, must not be {@code null}.
         * @return this
         * @since 4.3
         */
        Builder reconnectDelay(Delay reconnectDelay);

        /**
         * Sets the stateful reconnect {@link Supplier} to delay reconnect attempts. Defaults to binary exponential delay capped
         * at {@literal 30 SECONDS}.
         *
         * @param reconnectDelay the reconnect delay, must not be {@code null}.
         * @return this
         * @since 4.3
         */
        Builder reconnectDelay(Supplier<Delay> reconnectDelay);

        /**
         * Sets the {@link SocketAddressResolver} that is used to resolve {@link io.lettuce.core.RedisURI} to
         * {@link java.net.SocketAddress}. Defaults to {@link SocketAddressResolver} using the configured {@link DnsResolver}.
         *
         * @param socketAddressResolver the socket address resolver, must not be {@code null}.
         * @return {@code this} {@link Builder}.
         * @since 5.1
         */
        Builder socketAddressResolver(SocketAddressResolver socketAddressResolver);

        /**
         * Sets a shared {@link Timer} that can be used across different instances of {@link io.lettuce.core.RedisClient} and
         * {@link io.lettuce.core.cluster.RedisClusterClient} The provided {@link Timer} instance will not be shut down when
         * shutting down the client resources. You have to take care of that. This is an advanced configuration that should only
         * be used if you know what you are doing.
         *
         * @param timer the shared {@link Timer}, must not be {@code null}.
         * @return {@code this} {@link Builder}.
         * @since 4.3
         */
        Builder timer(Timer timer);

        /**
         * Sets the {@link Tracing} instance to trace Redis calls.
         *
         * @param tracing the tracer infrastructure instance, must not be {@code null}.
         * @return this
         * @since 5.1
         */
        Builder tracing(Tracing tracing);

        /**
         * @return a new instance of {@link DefaultClientResources}.
         */
        ClientResources build();

    }

    /**
     * Return a builder to create new {@link ClientResources} whose settings are replicated from the current
     * {@link ClientResources}.
     *
     * @return a {@link ClientResources.Builder} to create new {@link ClientResources} whose settings are replicated from the
     *         current {@link ClientResources}
     *
     * @since 5.1
     */
    Builder mutate();

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
     * Return the {@link EventPublisherOptions} for latency event publishing.
     *
     * @return the {@link EventPublisherOptions} for latency event publishing.
     */
    EventPublisherOptions commandLatencyPublisherOptions();

    /**
     * Return the {@link CommandLatencyRecorder}.
     *
     * @return the command latency recorder.
     * @since 6.0
     */
    CommandLatencyRecorder commandLatencyRecorder();

    /**
     * Return the pool size (number of threads) for all computation tasks.
     *
     * @return the pool size (number of threads to use).
     */
    int computationThreadPoolSize();

    /**
     * Return the {@link DnsResolver}.
     *
     * @return the DNS resolver.
     * @since 4.3
     */
    DnsResolver dnsResolver();

    /**
     * Return the event bus used to publish events.
     *
     * @return the event bus
     */
    EventBus eventBus();

    /**
     * Return the {@link EventLoopGroupProvider} that provides access to the particular {@link io.netty.channel.EventLoopGroup
     * event loop groups}. lettuce requires at least two implementations: {@link io.netty.channel.nio.NioEventLoopGroup} for
     * TCP/IP connections and {@link io.netty.channel.epoll.EpollEventLoopGroup} for unix domain socket connections (epoll).
     *
     * You can use {@link DefaultEventLoopGroupProvider} as default implementation or implement an own
     * {@link EventLoopGroupProvider} to share existing {@link io.netty.channel.EventLoopGroup EventLoopGroup's} with lettuce.
     *
     * @return the {@link EventLoopGroupProvider} which provides access to the particular {@link io.netty.channel.EventLoopGroup
     *         event loop groups}
     */
    EventLoopGroupProvider eventLoopGroupProvider();

    /**
     * Return the computation pool used for internal operations. Such tasks are periodic Redis Cluster and Redis Sentinel
     * topology updates and scheduling of connection reconnection by {@link io.lettuce.core.protocol.ConnectionWatchdog}.
     *
     * @return the computation pool used for internal operations
     */
    EventExecutorGroup eventExecutorGroup();

    /**
     * Return the pool size (number of threads) for IO threads. The indicated size does not reflect the number for all IO
     * threads. TCP and socket connections (epoll) require different IO pool.
     *
     * @return the pool size (number of threads) for all IO tasks.
     */
    int ioThreadPoolSize();

    /**
     * Return the {@link NettyCustomizer} to customize netty components.
     *
     * @return the configured {@link NettyCustomizer}.
     * @since 4.4
     */
    NettyCustomizer nettyCustomizer();

    /**
     * Return the {@link Delay} for reconnect attempts. May return a different instance on each call.
     *
     * @return the reconnect {@link Delay}.
     * @since 4.3
     */
    Delay reconnectDelay();

    /**
     * Return the {@link SocketAddressResolver}.
     *
     * @return the socket address resolver.
     * @since 5.1
     */
    SocketAddressResolver socketAddressResolver();

    /**
     * Return the {@link Timer} to schedule events. A timer object may run single- or multi-threaded but must be used for
     * scheduling of short-running jobs only. Long-running jobs should be scheduled and executed using
     * {@link #eventExecutorGroup()}.
     *
     * @return the timer.
     * @since 4.3
     */
    Timer timer();

    /**
     * Return the {@link Tracing} instance to support tracing of Redis commands.
     *
     * @return the configured {@link Tracing}.
     * @since 5.1
     */
    Tracing tracing();

}
