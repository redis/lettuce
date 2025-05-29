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
package io.lettuce.core.resource;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import io.lettuce.core.event.DefaultEventBus;
import io.lettuce.core.event.DefaultEventPublisherOptions;
import io.lettuce.core.event.EventBus;
import io.lettuce.core.event.EventPublisherOptions;
import io.lettuce.core.event.metrics.DefaultCommandLatencyEventPublisher;
import io.lettuce.core.event.metrics.MetricEventPublisher;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.internal.LettuceLists;
import io.lettuce.core.metrics.CommandLatencyCollector;
import io.lettuce.core.metrics.CommandLatencyCollectorOptions;
import io.lettuce.core.metrics.CommandLatencyRecorder;
import io.lettuce.core.metrics.DefaultCommandLatencyCollector;
import io.lettuce.core.metrics.DefaultCommandLatencyCollectorOptions;
import io.lettuce.core.metrics.MetricCollector;
import io.lettuce.core.resource.Delay.StatefulDelay;
import io.lettuce.core.tracing.Tracing;
import io.netty.resolver.AddressResolverGroup;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.ImmediateEventExecutor;
import io.netty.util.concurrent.PromiseCombiner;
import io.netty.util.internal.SystemPropertyUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import reactor.core.scheduler.Schedulers;

/**
 * Default instance of the client resources.
 * <p>
 * The {@link DefaultClientResources} instance is stateful, you have to shutdown the instance if you're no longer using it.
 * </p>
 * {@link DefaultClientResources} allow to configure:
 * <ul>
 * <li>a {@code addressResolverGroup} that is a provided instance of {@link AddressResolverGroup}.</li>
 * <li>a {@code commandLatencyRecorder} which is a provided instance of {@link io.lettuce.core.metrics.CommandLatencyRecorder}
 * <li>a {@code dnsResolver} which is a provided instance of {@link DnsResolver}.</li>
 * <li>an {@code eventBus} which is a provided instance of {@link EventBus}.</li> .</li>
 * <li>the {@code ioThreadPoolSize}, alternatively</li>
 * <li>a {@code eventLoopGroupProvider} which is a provided instance of {@link EventLoopGroupProvider}. Higher precedence than
 * {@code ioThreadPoolSize}.</li>
 * <li>computationThreadPoolSize</li>
 * <li>a {@code eventExecutorGroup} which is a provided instance of {@link EventExecutorGroup}. Higher precedence than
 * {@code computationThreadPoolSize}.</li>
 * <li>a {@code nettyCustomizer} that is a provided instance of {@link NettyCustomizer}.</li>
 * <li>a {@code socketAddressResolver} which is a provided instance of {@link SocketAddressResolver}.</li>
 * <li>a {@code threadFactoryProvider} to provide a {@link java.util.concurrent.ThreadFactory} for default timer, event loop and
 * event executor instances.</li>
 * <li>a {@code timer} that is a provided instance of {@link io.netty.util.HashedWheelTimer}.</li>
 * <li>a {@code tracing} that is a provided instance of {@link Tracing}.</li>
 * </ul>
 *
 * @author Mark Paluch
 * @author Yohei Ueki
 * @author Euiyoung Nam
 * @since 3.4
 */
public class DefaultClientResources implements ClientResources {

    protected static final InternalLogger logger = InternalLoggerFactory.getInstance(DefaultClientResources.class);

    /**
     * Minimum number of I/O threads.
     */
    public static final int MIN_IO_THREADS = 2;

    /**
     * Minimum number of computation threads.
     */
    public static final int MIN_COMPUTATION_THREADS = 2;

    public static final int DEFAULT_IO_THREADS;

    public static final int DEFAULT_COMPUTATION_THREADS;

    /**
     * Default delay {@link Supplier} for {@link Delay#exponential()} delay.
     */
    public static final Supplier<Delay> DEFAULT_RECONNECT_DELAY = Delay::exponential;

    /**
     * Default (no-op) {@link NettyCustomizer}.
     */
    public static final NettyCustomizer DEFAULT_NETTY_CUSTOMIZER = DefaultNettyCustomizer.INSTANCE;

    /**
     * Default {@link AddressResolverGroup}.
     */
    public static final AddressResolverGroup<?> DEFAULT_ADDRESS_RESOLVER_GROUP = AddressResolverGroupProvider
            .addressResolverGroup();

    static {

        int threads = Math.max(1, SystemPropertyUtil.getInt("io.netty.eventLoopThreads",
                Math.max(MIN_IO_THREADS, Runtime.getRuntime().availableProcessors())));

        DEFAULT_IO_THREADS = threads;
        DEFAULT_COMPUTATION_THREADS = threads;
        if (logger.isDebugEnabled()) {
            logger.debug("-Dio.netty.eventLoopThreads: {}", threads);
        }
    }

    private final AddressResolverGroup<?> addressResolverGroup;

    private final CommandLatencyRecorder commandLatencyRecorder;

    private final boolean sharedCommandLatencyRecorder;

    private final EventPublisherOptions commandLatencyPublisherOptions;

    private final DnsResolver dnsResolver;

    private final EventBus eventBus;

    private final boolean sharedEventLoopGroupProvider;

    private final EventLoopGroupProvider eventLoopGroupProvider;

    private final boolean sharedEventExecutor;

    private final EventExecutorGroup eventExecutorGroup;

    private final MetricEventPublisher metricEventPublisher;

    private final NettyCustomizer nettyCustomizer;

    private final Supplier<Delay> reconnectDelay;

    private final SocketAddressResolver socketAddressResolver;

    private final ThreadFactoryProvider threadFactoryProvider;

    private final Timer timer;

    private final boolean sharedTimer;

    private final Tracing tracing;

    private volatile boolean shutdownCalled = false;

    private volatile boolean shutdownCheck = true;

    protected DefaultClientResources(Builder builder) {

        addressResolverGroup = builder.addressResolverGroup;
        threadFactoryProvider = builder.threadFactoryProvider;

        if (builder.eventLoopGroupProvider == null) {
            int ioThreadPoolSize = builder.ioThreadPoolSize;

            if (ioThreadPoolSize < MIN_IO_THREADS) {
                logger.info("ioThreadPoolSize is less than {} ({}), setting to: {}", MIN_IO_THREADS, ioThreadPoolSize,
                        MIN_IO_THREADS);
                ioThreadPoolSize = MIN_IO_THREADS;
            }

            this.sharedEventLoopGroupProvider = false;
            this.eventLoopGroupProvider = new DefaultEventLoopGroupProvider(ioThreadPoolSize, threadFactoryProvider);

        } else {
            this.sharedEventLoopGroupProvider = builder.sharedEventLoopGroupProvider;
            this.eventLoopGroupProvider = builder.eventLoopGroupProvider;
        }

        if (builder.eventExecutorGroup == null) {
            int computationThreadPoolSize = builder.computationThreadPoolSize;
            if (computationThreadPoolSize < MIN_COMPUTATION_THREADS) {

                logger.info("computationThreadPoolSize is less than {} ({}), setting to: {}", MIN_COMPUTATION_THREADS,
                        computationThreadPoolSize, MIN_COMPUTATION_THREADS);
                computationThreadPoolSize = MIN_COMPUTATION_THREADS;
            }

            eventExecutorGroup = DefaultEventLoopGroupProvider.createEventLoopGroup(DefaultEventExecutorGroup.class,
                    computationThreadPoolSize, threadFactoryProvider);
            sharedEventExecutor = false;
        } else {
            sharedEventExecutor = builder.sharedEventExecutor;
            eventExecutorGroup = builder.eventExecutorGroup;
        }

        if (builder.timer == null) {
            timer = new HashedWheelTimer(threadFactoryProvider.getThreadFactory("lettuce-timer"));
            sharedTimer = false;
        } else {
            timer = builder.timer;
            sharedTimer = builder.sharedTimer;
        }

        if (builder.eventBus == null) {
            eventBus = new DefaultEventBus(Schedulers.fromExecutorService(eventExecutorGroup, "lettuce-event-bus"));
        } else {
            eventBus = builder.eventBus;
        }

        if (builder.commandLatencyRecorder == null) {
            if (DefaultCommandLatencyCollector.isAvailable()) {
                if (builder.commandLatencyCollectorOptions != null) {
                    commandLatencyRecorder = CommandLatencyCollector.create(builder.commandLatencyCollectorOptions);
                } else {
                    commandLatencyRecorder = CommandLatencyCollector.create(CommandLatencyCollectorOptions.create());
                }
            } else {
                logger.debug("LatencyUtils/HdrUtils are not available, metrics are disabled");
                builder.commandLatencyCollectorOptions = CommandLatencyCollectorOptions.disabled();
                commandLatencyRecorder = CommandLatencyRecorder.disabled();
            }

            sharedCommandLatencyRecorder = false;
        } else {
            sharedCommandLatencyRecorder = builder.sharedCommandLatencyCollector;
            commandLatencyRecorder = builder.commandLatencyRecorder;
        }

        commandLatencyPublisherOptions = builder.commandLatencyPublisherOptions;

        if (commandLatencyRecorder.isEnabled() && commandLatencyPublisherOptions != null
                && commandLatencyRecorder instanceof CommandLatencyCollector) {
            metricEventPublisher = new DefaultCommandLatencyEventPublisher(eventExecutorGroup, commandLatencyPublisherOptions,
                    eventBus, (CommandLatencyCollector) commandLatencyRecorder);
        } else {
            metricEventPublisher = null;
        }

        if (builder.dnsResolver == null) {
            dnsResolver = DnsResolvers.UNRESOLVED;
        } else {
            dnsResolver = builder.dnsResolver;
        }

        if (builder.socketAddressResolver == null) {
            socketAddressResolver = SocketAddressResolver.create(dnsResolver);
        } else {
            socketAddressResolver = builder.socketAddressResolver;
        }

        reconnectDelay = builder.reconnectDelay;
        nettyCustomizer = builder.nettyCustomizer;
        tracing = builder.tracing;

        if (!sharedTimer && timer instanceof HashedWheelTimer) {
            ((HashedWheelTimer) timer).start();
        }
    }

    /**
     * Create a new {@link DefaultClientResources} using default settings.
     *
     * @return a new instance of a default client resources.
     */
    public static DefaultClientResources create() {
        return builder().build();
    }

    /**
     * Returns a new {@link DefaultClientResources.Builder} to construct {@link DefaultClientResources}.
     *
     * @return a new {@link DefaultClientResources.Builder} to construct {@link DefaultClientResources}.
     */
    public static DefaultClientResources.Builder builder() {
        return new DefaultClientResources.Builder();
    }

    /**
     * Builder for {@link DefaultClientResources}.
     */
    public static class Builder implements ClientResources.Builder {

        private CommandLatencyCollectorOptions commandLatencyCollectorOptions = DefaultCommandLatencyCollectorOptions.create();

        private CommandLatencyRecorder commandLatencyRecorder;

        private EventPublisherOptions commandLatencyPublisherOptions = DefaultEventPublisherOptions.create();

        private boolean sharedCommandLatencyCollector;

        private int computationThreadPoolSize = DEFAULT_COMPUTATION_THREADS;

        private DnsResolver dnsResolver = DnsResolvers.UNRESOLVED;

        private EventBus eventBus;

        private EventExecutorGroup eventExecutorGroup;

        private boolean sharedEventExecutor;

        private boolean sharedEventLoopGroupProvider;

        private EventLoopGroupProvider eventLoopGroupProvider;

        private int ioThreadPoolSize = DEFAULT_IO_THREADS;

        private NettyCustomizer nettyCustomizer = DEFAULT_NETTY_CUSTOMIZER;

        private SocketAddressResolver socketAddressResolver;

        private Supplier<Delay> reconnectDelay = DEFAULT_RECONNECT_DELAY;

        private boolean sharedTimer;

        private ThreadFactoryProvider threadFactoryProvider = DefaultThreadFactoryProvider.INSTANCE;

        private Timer timer;

        private Tracing tracing = Tracing.disabled();

        private AddressResolverGroup<?> addressResolverGroup = DEFAULT_ADDRESS_RESOLVER_GROUP;

        private Runnable afterBuild;

        private Builder() {
        }

        Builder afterBuild(Runnable runnable) {
            if (this.afterBuild == null) {
                this.afterBuild = runnable;
            } else {
                Runnable previous = this.afterBuild;
                this.afterBuild = () -> {
                    previous.run();
                    runnable.run();
                };
            }

            return this;
        }

        /**
         * Sets the {@link AddressResolverGroup} for DNS resolution. This option is only effective if
         * {@link DnsResolvers#UNRESOLVED} is used as {@link DnsResolver}. Defaults to
         * {@link io.netty.resolver.DefaultAddressResolverGroup#INSTANCE} if {@literal netty-dns-resolver} is not available,
         * otherwise defaults to {@link io.netty.resolver.dns.DnsAddressResolverGroup}.
         *
         * @param addressResolverGroup the {@link AddressResolverGroup} instance, must not be {@code null}.
         * @return {@code this} {@link ClientResources.Builder}
         * @since 6.1
         */
        @Override
        public Builder addressResolverGroup(AddressResolverGroup<?> addressResolverGroup) {

            LettuceAssert.notNull(addressResolverGroup, "AddressResolverGroup must not be null");

            this.addressResolverGroup = addressResolverGroup;
            return this;
        }

        /**
         * Sets the {@link EventPublisherOptions} to publish command latency metrics using the {@link EventBus} if the
         * {@link CommandLatencyRecorder} is an instance of {@link CommandLatencyCollector} that allows latency metric
         * retrieval.
         *
         * @param commandLatencyPublisherOptions the {@link EventPublisherOptions} to publish command latency metrics using the
         *        {@link EventBus}, must not be {@code null}.
         * @return {@code this} {@link ClientResources.Builder}.
         */
        @Override
        public Builder commandLatencyPublisherOptions(EventPublisherOptions commandLatencyPublisherOptions) {

            LettuceAssert.notNull(commandLatencyPublisherOptions, "EventPublisherOptions must not be null");

            this.commandLatencyPublisherOptions = commandLatencyPublisherOptions;
            return this;
        }

        /**
         * Sets the {@link CommandLatencyCollectorOptions} that can be used across different instances of the RedisClient. The
         * options are only effective if no {@code commandLatencyCollector} is provided.
         *
         * @param commandLatencyCollectorOptions the command latency collector options, must not be {@code null}.
         * @return {@code this} {@link Builder}.
         * @deprecated since 6.0. Configure {@link io.lettuce.core.metrics.CommandLatencyRecorder} directly using
         *             {@link CommandLatencyCollectorOptions}.
         */
        @Override
        @Deprecated
        public Builder commandLatencyCollectorOptions(CommandLatencyCollectorOptions commandLatencyCollectorOptions) {

            LettuceAssert.notNull(commandLatencyCollectorOptions, "CommandLatencyCollectorOptions must not be null");

            this.commandLatencyCollectorOptions = commandLatencyCollectorOptions;
            return this;
        }

        /**
         * Sets the {@link CommandLatencyRecorder} that can be used across different instances of the RedisClient.
         *
         * @param commandLatencyRecorder the command latency recorder, must not be {@code null}.
         * @return {@code this} {@link Builder}.
         */
        @Override
        public Builder commandLatencyRecorder(CommandLatencyRecorder commandLatencyRecorder) {

            LettuceAssert.notNull(commandLatencyRecorder, "CommandLatencyRecorder must not be null");

            this.sharedCommandLatencyCollector = true;
            this.commandLatencyRecorder = commandLatencyRecorder;
            return this;
        }

        /**
         * Sets the thread pool size (number of threads to use) for computation operations (default value is the number of
         * CPUs). The thread pool size is only effective if no {@code eventExecutorGroup} is provided.
         *
         * @param computationThreadPoolSize the thread pool size, must be greater than {@code 0}.
         * @return {@code this} {@link Builder}.
         */
        @Override
        public Builder computationThreadPoolSize(int computationThreadPoolSize) {

            LettuceAssert.isTrue(computationThreadPoolSize > 0, "Computation thread pool size must be greater than zero");

            this.computationThreadPoolSize = computationThreadPoolSize;
            return this;
        }

        /**
         * Sets the {@link DnsResolver} that is used to resolve hostnames to {@link java.net.InetAddress}. Defaults to
         * {@link DnsResolvers#UNRESOLVED}
         *
         * @param dnsResolver the DNS resolver, must not be {@code null}.
         * @return {@code this} {@link Builder}.
         * @since 4.3
         * @deprecated since 6.1. Configure {@link AddressResolverGroup} instead.
         */
        @Deprecated
        @Override
        public Builder dnsResolver(DnsResolver dnsResolver) {

            LettuceAssert.notNull(dnsResolver, "DnsResolver must not be null");

            this.dnsResolver = dnsResolver;
            return this;
        }

        /**
         * Sets the {@link EventBus} that can be used across different instances of the RedisClient.
         *
         * @param eventBus the event bus, must not be {@code null}.
         * @return {@code this} {@link Builder}.
         */
        @Override
        public Builder eventBus(EventBus eventBus) {

            LettuceAssert.notNull(eventBus, "EventBus must not be null");

            this.eventBus = eventBus;
            return this;
        }

        /**
         * Sets a shared {@link EventLoopGroupProvider event executor provider} that can be used across different instances of
         * {@link io.lettuce.core.RedisClient} and {@link io.lettuce.core.cluster.RedisClusterClient}. The provided
         * {@link EventLoopGroupProvider} instance will not be shut down when shutting down the client resources. You have to
         * take care of that. This is an advanced configuration that should only be used if you know what you are doing.
         *
         * @param eventLoopGroupProvider the shared eventLoopGroupProvider, must not be {@code null}.
         * @return {@code this} {@link Builder}.
         */
        @Override
        public Builder eventLoopGroupProvider(EventLoopGroupProvider eventLoopGroupProvider) {

            LettuceAssert.notNull(eventLoopGroupProvider, "EventLoopGroupProvider must not be null");

            this.sharedEventLoopGroupProvider = true;
            this.eventLoopGroupProvider = eventLoopGroupProvider;
            return this;
        }

        /**
         * Sets a shared {@link EventExecutorGroup event executor group} that can be used across different instances of
         * {@link io.lettuce.core.RedisClient} and {@link io.lettuce.core.cluster.RedisClusterClient}. The provided
         * {@link EventExecutorGroup} instance will not be shut down when shutting down the client resources. You have to take
         * care of that. This is an advanced configuration that should only be used if you know what you are doing.
         *
         * @param eventExecutorGroup the shared eventExecutorGroup, must not be {@code null}.
         * @return {@code this} {@link Builder}.
         */
        @Override
        public Builder eventExecutorGroup(EventExecutorGroup eventExecutorGroup) {

            LettuceAssert.notNull(eventExecutorGroup, "EventExecutorGroup must not be null");

            this.sharedEventExecutor = true;
            this.eventExecutorGroup = eventExecutorGroup;
            return this;
        }

        /**
         * Sets the {@link NettyCustomizer} instance to customize netty components during connection.
         *
         * @param nettyCustomizer the netty customizer instance, must not be {@code null}.
         * @return this
         * @since 4.4
         */
        @Override
        public Builder nettyCustomizer(NettyCustomizer nettyCustomizer) {

            LettuceAssert.notNull(nettyCustomizer, "NettyCustomizer must not be null");

            this.nettyCustomizer = nettyCustomizer;
            return this;
        }

        /**
         * Sets the thread pool size (number of threads to use) for I/O operations (default value is the number of CPUs). The
         * thread pool size is only effective if no {@code eventLoopGroupProvider} is provided.
         *
         * @param ioThreadPoolSize the thread pool size, must be greater {@code 0}.
         * @return {@code this} {@link Builder}.
         */
        @Override
        public Builder ioThreadPoolSize(int ioThreadPoolSize) {

            LettuceAssert.isTrue(ioThreadPoolSize > 0, "I/O thread pool size must be greater zero");

            this.ioThreadPoolSize = ioThreadPoolSize;
            return this;
        }

        /**
         * Sets the stateless reconnect {@link Delay} to delay reconnect attempts. Defaults to binary exponential delay capped
         * at {@literal 30 SECONDS}. {@code reconnectDelay} must be a stateless {@link Delay}.
         *
         * @param reconnectDelay the reconnect delay, must not be {@code null}.
         * @return this
         * @since 4.3
         */
        @Override
        public Builder reconnectDelay(Delay reconnectDelay) {

            LettuceAssert.notNull(reconnectDelay, "Delay must not be null");
            LettuceAssert.isTrue(!(reconnectDelay instanceof StatefulDelay), "Delay must be a stateless instance.");

            return reconnectDelay(() -> reconnectDelay);
        }

        /**
         * Sets the stateful reconnect {@link Supplier} to delay reconnect attempts. Defaults to binary exponential delay capped
         * at {@literal 30 SECONDS}.
         *
         * @param reconnectDelay the reconnect delay, must not be {@code null}.
         * @return this
         * @since 4.3
         */
        @Override
        public Builder reconnectDelay(Supplier<Delay> reconnectDelay) {

            LettuceAssert.notNull(reconnectDelay, "Delay must not be null");

            this.reconnectDelay = reconnectDelay;
            return this;
        }

        /**
         * Sets the {@link SocketAddressResolver} that is used to resolve {@link io.lettuce.core.RedisURI} to
         * {@link java.net.SocketAddress}. Defaults to {@link SocketAddressResolver} using the configured {@link DnsResolver}.
         *
         * @param socketAddressResolver the socket address resolver, must not be {@code null}.
         * @return {@code this} {@link ClientResources.Builder}.
         * @since 5.1
         */
        @Override
        public ClientResources.Builder socketAddressResolver(SocketAddressResolver socketAddressResolver) {

            LettuceAssert.notNull(socketAddressResolver, "SocketAddressResolver must not be null");

            this.socketAddressResolver = socketAddressResolver;
            return this;
        }

        /**
         * Provide a default {@link ThreadFactoryProvider} to obtain {@link java.util.concurrent.ThreadFactory} for a
         * {@code poolName}.
         * <p>
         * Applies only to threading resources created by {@link DefaultClientResources} when not configuring {@link #timer()},
         * {@link #eventExecutorGroup()}, or {@link #eventLoopGroupProvider()}.
         *
         * @param threadFactoryProvider a provider to obtain a {@link java.util.concurrent.ThreadFactory} for a
         *        {@code poolName}, must not be {@code null}.
         * @return {@code this} {@link ClientResources.Builder}.
         * @since 6.1.1
         * @see #eventExecutorGroup(EventExecutorGroup)
         * @see #eventLoopGroupProvider(EventLoopGroupProvider)
         * @see #timer(Timer)
         */
        @Override
        public ClientResources.Builder threadFactoryProvider(ThreadFactoryProvider threadFactoryProvider) {

            LettuceAssert.notNull(threadFactoryProvider, "ThreadFactoryProvider must not be null");

            this.threadFactoryProvider = threadFactoryProvider;
            return this;
        }

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
        @Override
        public Builder timer(Timer timer) {

            LettuceAssert.notNull(timer, "Timer must not be null");

            this.sharedTimer = true;
            this.timer = timer;
            return this;
        }

        /**
         * Sets the {@link Tracing} instance to trace Redis calls.
         *
         * @param tracing the tracer infrastructure instance, must not be {@code null}.
         * @return this
         * @since 5.1
         */
        @Override
        public Builder tracing(Tracing tracing) {

            LettuceAssert.notNull(tracing, "Tracing must not be null");

            this.tracing = tracing;
            return this;
        }

        /**
         * @return a new instance of {@link DefaultClientResources}.
         */
        @Override
        public DefaultClientResources build() {

            DefaultClientResources resources = new DefaultClientResources(this);

            if (this.afterBuild != null) {
                this.afterBuild.run();
            }

            return resources;
        }

    }

    /**
     * Returns a builder to create new {@link DefaultClientResources} whose settings are replicated from the current
     * {@link DefaultClientResources}.
     * <p>
     * Note: The resulting {@link DefaultClientResources} retains shared state for {@link Timer},
     * {@link CommandLatencyRecorder}, {@link EventExecutorGroup}, and {@link EventLoopGroupProvider} if these are left
     * unchanged. Thus you need only to shut down the last created {@link ClientResources} instances. Shutdown affects any
     * previously created {@link ClientResources}.
     * </p>
     *
     * @return a {@link DefaultClientResources.Builder} to create new {@link DefaultClientResources} whose settings are
     *         replicated from the current {@link DefaultClientResources}.
     *
     * @since 5.1
     */
    @Override
    public DefaultClientResources.Builder mutate() {

        Builder builder = new Builder();

        builder.afterBuild(() -> this.shutdownCheck = false).commandLatencyRecorder(commandLatencyRecorder())
                .commandLatencyPublisherOptions(commandLatencyPublisherOptions()).dnsResolver(dnsResolver())
                .eventBus(eventBus()).eventExecutorGroup(eventExecutorGroup()).reconnectDelay(reconnectDelay)
                .socketAddressResolver(socketAddressResolver()).nettyCustomizer(nettyCustomizer())
                .threadFactoryProvider(threadFactoryProvider).timer(timer()).tracing(tracing())
                .addressResolverGroup(addressResolverGroup());

        builder.sharedCommandLatencyCollector = sharedEventLoopGroupProvider;
        builder.sharedEventExecutor = sharedEventExecutor;
        builder.sharedEventLoopGroupProvider = sharedEventLoopGroupProvider;
        builder.sharedTimer = sharedTimer;

        return builder;
    }

    @Override
    protected void finalize() throws Throwable {
        if (shutdownCheck && !shutdownCalled) {
            logger.warn(getClass().getName()
                    + " was not shut down properly, shutdown() was not called before it's garbage-collected. Call shutdown() or shutdown(long,long,TimeUnit) ");
        }
        super.finalize();
    }

    /**
     * Shutdown the {@link ClientResources}.
     *
     * @return eventually the success/failure of the shutdown without errors.
     */
    @Override
    public Future<Boolean> shutdown() {
        return shutdown(0, 2, TimeUnit.SECONDS);
    }

    /**
     * Shutdown the {@link ClientResources}.
     *
     * @param quietPeriod the quiet period as described in the documentation
     * @param timeout the maximum amount of time to wait until the executor is shutdown regardless if a task was submitted
     *        during the quiet period
     * @param timeUnit the unit of {@code quietPeriod} and {@code timeout}
     * @return eventually the success/failure of the shutdown without errors.
     */
    @SuppressWarnings("unchecked")
    public Future<Boolean> shutdown(long quietPeriod, long timeout, TimeUnit timeUnit) {

        logger.debug("Initiate shutdown ({}, {}, {})", quietPeriod, timeout, timeUnit);

        shutdownCalled = true;
        DefaultPromise<Void> voidPromise = new DefaultPromise<>(ImmediateEventExecutor.INSTANCE);
        PromiseCombiner aggregator = new PromiseCombiner(ImmediateEventExecutor.INSTANCE);

        if (metricEventPublisher != null) {
            metricEventPublisher.shutdown();
        }

        if (!sharedTimer) {
            timer.stop();
        }

        if (!sharedEventLoopGroupProvider) {
            Future<Boolean> shutdown = eventLoopGroupProvider.shutdown(quietPeriod, timeout, timeUnit);
            aggregator.add(shutdown);
        }

        if (!sharedEventExecutor) {
            Future<?> shutdown = eventExecutorGroup.shutdownGracefully(quietPeriod, timeout, timeUnit);
            aggregator.add(shutdown);
        }

        if (!sharedCommandLatencyRecorder && commandLatencyRecorder instanceof MetricCollector) {
            ((MetricCollector<?>) commandLatencyRecorder).shutdown();
        }

        aggregator.finish(voidPromise);

        return PromiseAdapter.toBooleanPromise(voidPromise);
    }

    @Override
    public CommandLatencyRecorder commandLatencyRecorder() {
        return commandLatencyRecorder;
    }

    @Override
    public EventPublisherOptions commandLatencyPublisherOptions() {
        return commandLatencyPublisherOptions;
    }

    @Override
    public int computationThreadPoolSize() {
        return LettuceLists.newList(eventExecutorGroup.iterator()).size();
    }

    /**
     * @deprecated since 6.7 replaced by{@link AddressResolverGroup} instead.
     **/
    @Deprecated
    @Override
    public DnsResolver dnsResolver() {
        return dnsResolver;
    }

    @Override
    public EventBus eventBus() {
        return eventBus;
    }

    @Override
    public EventLoopGroupProvider eventLoopGroupProvider() {
        return eventLoopGroupProvider;
    }

    @Override
    public EventExecutorGroup eventExecutorGroup() {
        return eventExecutorGroup;
    }

    @Override
    public int ioThreadPoolSize() {
        return eventLoopGroupProvider.threadPoolSize();
    }

    @Override
    public NettyCustomizer nettyCustomizer() {
        return nettyCustomizer;
    }

    @Override
    public Delay reconnectDelay() {
        return reconnectDelay.get();
    }

    @Override
    public SocketAddressResolver socketAddressResolver() {
        return socketAddressResolver;
    }

    @Override
    public Timer timer() {
        return timer;
    }

    @Override
    public Tracing tracing() {
        return tracing;
    }

    @Override
    public AddressResolverGroup<?> addressResolverGroup() {
        return addressResolverGroup;
    }

}
