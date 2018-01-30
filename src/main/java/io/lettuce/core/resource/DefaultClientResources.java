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

import static io.lettuce.core.resource.Futures.toBooleanPromise;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import reactor.core.scheduler.Schedulers;
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
import io.lettuce.core.metrics.DefaultCommandLatencyCollector;
import io.lettuce.core.metrics.DefaultCommandLatencyCollectorOptions;
import io.lettuce.core.resource.Delay.StatefulDelay;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import io.netty.util.concurrent.*;
import io.netty.util.internal.SystemPropertyUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Default instance of the client resources.
 * <p>
 * The {@link DefaultClientResources} instance is stateful, you have to shutdown the instance if you're no longer using it.
 * </p>
 * {@link DefaultClientResources} allow to configure:
 * <ul>
 * <li>the {@code ioThreadPoolSize}, alternatively</li>
 * <li>a {@code eventLoopGroupProvider} which is a provided instance of {@link EventLoopGroupProvider}. Higher precedence than
 * {@code ioThreadPoolSize}.</li>
 * <li>computationThreadPoolSize</li>
 * <li>a {@code eventExecutorGroup} which is a provided instance of {@link EventExecutorGroup}. Higher precedence than
 * {@code computationThreadPoolSize}.</li>
 * <li>an {@code eventBus} which is a provided instance of {@link EventBus}.</li>
 * <li>a {@code commandLatencyCollector} which is a provided instance of {@link io.lettuce.core.metrics.CommandLatencyCollector}
 * .</li>
 * <li>a {@code dnsResolver} which is a provided instance of {@link DnsResolver}.</li>
 * <li>a {@code timer} that is a provided instance of {@link io.netty.util.HashedWheelTimer}.</li>
 * <li>a {@code nettyCustomizer} that is a provided instance of {@link NettyCustomizer}.</li>
 * </ul>
 *
 * @author Mark Paluch
 * @since 3.4
 */
public class DefaultClientResources implements ClientResources {

    protected static final InternalLogger logger = InternalLoggerFactory.getInstance(DefaultClientResources.class);

    /**
     * Minimum number of I/O threads.
     */
    public static final int MIN_IO_THREADS = 3;

    /**
     * Minimum number of computation threads.
     */
    public static final int MIN_COMPUTATION_THREADS = 3;

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

    static {

        int threads = Math.max(
                1,
                SystemPropertyUtil.getInt("io.netty.eventLoopThreads",
                        Math.max(MIN_IO_THREADS, Runtime.getRuntime().availableProcessors())));

        DEFAULT_IO_THREADS = threads;
        DEFAULT_COMPUTATION_THREADS = threads;
        if (logger.isDebugEnabled()) {
            logger.debug("-Dio.netty.eventLoopThreads: {}", threads);
        }
    }

    private final boolean sharedEventLoopGroupProvider;
    private final EventLoopGroupProvider eventLoopGroupProvider;
    private final boolean sharedEventExecutor;
    private final EventExecutorGroup eventExecutorGroup;
    private final Timer timer;
    private final boolean sharedTimer;
    private final EventBus eventBus;
    private final CommandLatencyCollector commandLatencyCollector;
    private final boolean sharedCommandLatencyCollector;
    private final EventPublisherOptions commandLatencyPublisherOptions;
    private final MetricEventPublisher metricEventPublisher;
    private final DnsResolver dnsResolver;
    private final Supplier<Delay> reconnectDelay;
    private final NettyCustomizer nettyCustomizer;

    private volatile boolean shutdownCalled = false;

    protected DefaultClientResources(Builder builder) {

        if (builder.eventLoopGroupProvider == null) {
            int ioThreadPoolSize = builder.ioThreadPoolSize;

            if (ioThreadPoolSize < MIN_IO_THREADS) {
                logger.info("ioThreadPoolSize is less than {} ({}), setting to: {}", MIN_IO_THREADS, ioThreadPoolSize,
                        MIN_IO_THREADS);
                ioThreadPoolSize = MIN_IO_THREADS;
            }

            this.sharedEventLoopGroupProvider = false;
            this.eventLoopGroupProvider = new DefaultEventLoopGroupProvider(ioThreadPoolSize);

        } else {
            this.sharedEventLoopGroupProvider = true;
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
                    computationThreadPoolSize);
            sharedEventExecutor = false;
        } else {
            sharedEventExecutor = true;
            eventExecutorGroup = builder.eventExecutorGroup;
        }

        if (builder.timer == null) {
            timer = new HashedWheelTimer(new DefaultThreadFactory("lettuce-timer"));
            sharedTimer = false;
        } else {
            timer = builder.timer;
            sharedTimer = true;
        }

        if (builder.eventBus == null) {
            eventBus = new DefaultEventBus(Schedulers.fromExecutor(eventExecutorGroup));
        } else {
            eventBus = builder.eventBus;
        }

        if (builder.commandLatencyCollector == null) {
            if (DefaultCommandLatencyCollector.isAvailable()) {
                if (builder.commandLatencyCollectorOptions != null) {
                    commandLatencyCollector = new DefaultCommandLatencyCollector(builder.commandLatencyCollectorOptions);
                } else {
                    commandLatencyCollector = new DefaultCommandLatencyCollector(DefaultCommandLatencyCollectorOptions.create());
                }
            } else {
                logger.debug("LatencyUtils/HdrUtils are not available, metrics are disabled");
                builder.commandLatencyCollectorOptions = DefaultCommandLatencyCollectorOptions.disabled();
                commandLatencyCollector = DefaultCommandLatencyCollector.disabled();
            }

            sharedCommandLatencyCollector = false;
        } else {
            sharedCommandLatencyCollector = true;
            commandLatencyCollector = builder.commandLatencyCollector;
        }

        commandLatencyPublisherOptions = builder.commandLatencyPublisherOptions;

        if (commandLatencyCollector.isEnabled() && commandLatencyPublisherOptions != null) {
            metricEventPublisher = new DefaultCommandLatencyEventPublisher(eventExecutorGroup, commandLatencyPublisherOptions,
                    eventBus, commandLatencyCollector);
        } else {
            metricEventPublisher = null;
        }

        if (builder.dnsResolver == null) {
            dnsResolver = DnsResolvers.UNRESOLVED;
        } else {
            dnsResolver = builder.dnsResolver;
        }

        reconnectDelay = builder.reconnectDelay;
        nettyCustomizer = builder.nettyCustomizer;
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
     * Create a new {@link DefaultClientResources} using default settings.
     *
     * @return a new instance of a default client resources.
     */
    public static DefaultClientResources create() {
        return builder().build();
    }

    /**
     * Builder for {@link DefaultClientResources}.
     */
    public static class Builder {

        private int ioThreadPoolSize = DEFAULT_IO_THREADS;
        private int computationThreadPoolSize = DEFAULT_COMPUTATION_THREADS;
        private EventExecutorGroup eventExecutorGroup;
        private EventLoopGroupProvider eventLoopGroupProvider;
        private Timer timer;
        private EventBus eventBus;
        private CommandLatencyCollectorOptions commandLatencyCollectorOptions = DefaultCommandLatencyCollectorOptions.create();
        private CommandLatencyCollector commandLatencyCollector;
        private EventPublisherOptions commandLatencyPublisherOptions = DefaultEventPublisherOptions.create();
        private DnsResolver dnsResolver = DnsResolvers.UNRESOLVED;
        private Supplier<Delay> reconnectDelay = DEFAULT_RECONNECT_DELAY;
        private NettyCustomizer nettyCustomizer = DEFAULT_NETTY_CUSTOMIZER;

        private Builder() {
        }

        /**
         * Sets the thread pool size (number of threads to use) for I/O operations (default value is the number of CPUs). The
         * thread pool size is only effective if no {@code eventLoopGroupProvider} is provided.
         *
         * @param ioThreadPoolSize the thread pool size, must be greater {@code 0}.
         * @return {@code this} {@link Builder}.
         */
        public Builder ioThreadPoolSize(int ioThreadPoolSize) {

            LettuceAssert.isTrue(ioThreadPoolSize > 0, "I/O thread pool size must be greater zero");

            this.ioThreadPoolSize = ioThreadPoolSize;
            return this;
        }

        /**
         * Sets a shared {@link EventLoopGroupProvider event executor provider} that can be used across different instances of
         * {@link io.lettuce.core.RedisClient} and {@link io.lettuce.core.cluster.RedisClusterClient}. The provided
         * {@link EventLoopGroupProvider} instance will not be shut down when shutting down the client resources. You have to
         * take care of that. This is an advanced configuration that should only be used if you know what you are doing.
         *
         * @param eventLoopGroupProvider the shared eventLoopGroupProvider, must not be {@literal null}.
         * @return {@code this} {@link Builder}.
         */
        public Builder eventLoopGroupProvider(EventLoopGroupProvider eventLoopGroupProvider) {

            LettuceAssert.notNull(eventLoopGroupProvider, "EventLoopGroupProvider must not be null");

            this.eventLoopGroupProvider = eventLoopGroupProvider;
            return this;
        }

        /**
         * Sets the thread pool size (number of threads to use) for computation operations (default value is the number of
         * CPUs). The thread pool size is only effective if no {@code eventExecutorGroup} is provided.
         *
         * @param computationThreadPoolSize the thread pool size, must be greater {@code 0}.
         * @return {@code this} {@link Builder}.
         */
        public Builder computationThreadPoolSize(int computationThreadPoolSize) {

            LettuceAssert.isTrue(computationThreadPoolSize > 0, "Computation thread pool size must be greater zero");

            this.computationThreadPoolSize = computationThreadPoolSize;
            return this;
        }

        /**
         * Sets a shared {@link EventExecutorGroup event executor group} that can be used across different instances of
         * {@link io.lettuce.core.RedisClient} and {@link io.lettuce.core.cluster.RedisClusterClient}. The provided
         * {@link EventExecutorGroup} instance will not be shut down when shutting down the client resources. You have to take
         * care of that. This is an advanced configuration that should only be used if you know what you are doing.
         *
         * @param eventExecutorGroup the shared eventExecutorGroup, must not be {@literal null}.
         * @return {@code this} {@link Builder}.
         */
        public Builder eventExecutorGroup(EventExecutorGroup eventExecutorGroup) {

            LettuceAssert.notNull(eventExecutorGroup, "EventExecutorGroup must not be null");

            this.eventExecutorGroup = eventExecutorGroup;
            return this;
        }

        /**
         * Sets a shared {@link Timer} that can be used across different instances of {@link io.lettuce.core.RedisClient} and
         * {@link io.lettuce.core.cluster.RedisClusterClient} The provided {@link Timer} instance will not be shut down when
         * shutting down the client resources. You have to take care of that. This is an advanced configuration that should only
         * be used if you know what you are doing.
         *
         * @param timer the shared {@link Timer}, must not be {@literal null}.
         * @return {@code this} {@link Builder}.
         * @since 4.3
         */
        public Builder timer(Timer timer) {

            LettuceAssert.notNull(timer, "Timer must not be null");

            this.timer = timer;
            return this;
        }

        /**
         * Sets the {@link EventBus} that can that can be used across different instances of the RedisClient.
         *
         * @param eventBus the event bus, must not be {@literal null}.
         * @return {@code this} {@link Builder}.
         */
        public Builder eventBus(EventBus eventBus) {

            LettuceAssert.notNull(eventBus, "EventBus must not be null");

            this.eventBus = eventBus;
            return this;
        }

        /**
         * Sets the {@link EventPublisherOptions} to publish command latency metrics using the {@link EventBus}.
         *
         * @param commandLatencyPublisherOptions the {@link EventPublisherOptions} to publish command latency metrics using the
         *        {@link EventBus}, must not be {@literal null}.
         * @return {@code this} {@link Builder}.
         */
        public Builder commandLatencyPublisherOptions(EventPublisherOptions commandLatencyPublisherOptions) {

            LettuceAssert.notNull(commandLatencyPublisherOptions, "EventPublisherOptions must not be null");

            this.commandLatencyPublisherOptions = commandLatencyPublisherOptions;
            return this;
        }

        /**
         * Sets the {@link CommandLatencyCollectorOptions} that can that can be used across different instances of the
         * RedisClient. The options are only effective if no {@code commandLatencyCollector} is provided.
         *
         * @param commandLatencyCollectorOptions the command latency collector options, must not be {@link null}.
         * @return {@code this} {@link Builder}.
         */
        public Builder commandLatencyCollectorOptions(CommandLatencyCollectorOptions commandLatencyCollectorOptions) {

            LettuceAssert.notNull(commandLatencyCollectorOptions, "CommandLatencyCollectorOptions must not be null");

            this.commandLatencyCollectorOptions = commandLatencyCollectorOptions;
            return this;
        }

        /**
         * Sets the {@link CommandLatencyCollector} that can that can be used across different instances of the RedisClient.
         *
         * @param commandLatencyCollector the command latency collector, must not be {@literal null}.
         * @return {@code this} {@link Builder}.
         */
        public Builder commandLatencyCollector(CommandLatencyCollector commandLatencyCollector) {

            LettuceAssert.notNull(commandLatencyCollector, "CommandLatencyCollector must not be null");

            this.commandLatencyCollector = commandLatencyCollector;
            return this;
        }

        /**
         * Sets the {@link DnsResolver} that can that is used to resolve hostnames to {@link java.net.InetAddress}. Defaults to
         * {@link DnsResolvers#JVM_DEFAULT}
         *
         * @param dnsResolver the DNS resolver, must not be {@link null}.
         * @return {@code this} {@link Builder}.
         * @since 4.3
         */
        public Builder dnsResolver(DnsResolver dnsResolver) {

            LettuceAssert.notNull(dnsResolver, "DnsResolver must not be null");

            this.dnsResolver = dnsResolver;
            return this;
        }

        /**
         * Sets the stateless reconnect {@link Delay} to delay reconnect attempts. Defaults to binary exponential delay capped
         * at {@literal 30 SECONDS}. {@code reconnectDelay} must be a stateless {@link Delay}.
         *
         * @param reconnectDelay the reconnect delay, must not be {@literal null}.
         * @return this
         * @since 4.3
         */
        public Builder reconnectDelay(Delay reconnectDelay) {

            LettuceAssert.notNull(reconnectDelay, "Delay must not be null");
            LettuceAssert.isTrue(!(reconnectDelay instanceof StatefulDelay), "Delay must be a stateless instance.");

            return reconnectDelay(() -> reconnectDelay);
        }

        /**
         * Sets the stateful reconnect {@link Supplier} to delay reconnect attempts. Defaults to binary exponential delay capped
         * at {@literal 30 SECONDS}.
         *
         * @param reconnectDelay the reconnect delay, must not be {@literal null}.
         * @return this
         * @since 4.3
         */
        public Builder reconnectDelay(Supplier<Delay> reconnectDelay) {

            LettuceAssert.notNull(reconnectDelay, "Delay must not be null");

            this.reconnectDelay = reconnectDelay;
            return this;
        }

        /**
         * Sets the {@link NettyCustomizer} instance to customize netty components during connection.
         *
         * @param nettyCustomizer the netty customizer instance, must not be {@literal null}.
         * @return this
         * @since 4.4
         */
        public Builder nettyCustomizer(NettyCustomizer nettyCustomizer) {

            LettuceAssert.notNull(nettyCustomizer, "NettyCustomizer must not be null");

            this.nettyCustomizer = nettyCustomizer;
            return this;
        }

        /**
         *
         * @return a new instance of {@link DefaultClientResources}.
         */
        public DefaultClientResources build() {
            return new DefaultClientResources(this);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        if (!shutdownCalled) {
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

        shutdownCalled = true;
        DefaultPromise<Boolean> overall = new DefaultPromise<Boolean>(GlobalEventExecutor.INSTANCE);
        DefaultPromise<Boolean> lastRelease = new DefaultPromise<Boolean>(GlobalEventExecutor.INSTANCE);
        Futures.PromiseAggregator<Boolean, Promise<Boolean>> aggregator = new Futures.PromiseAggregator<Boolean, Promise<Boolean>>(
                overall);

        aggregator.expectMore(1);

        if (!sharedEventLoopGroupProvider) {
            aggregator.expectMore(1);
        }

        if (!sharedEventExecutor) {
            aggregator.expectMore(1);
        }

        aggregator.arm();

        if (metricEventPublisher != null) {
            metricEventPublisher.shutdown();
        }

        if (!sharedTimer) {
            timer.stop();
        }

        if (!sharedEventLoopGroupProvider) {
            Future<Boolean> shutdown = eventLoopGroupProvider.shutdown(quietPeriod, timeout, timeUnit);
            if (shutdown instanceof Promise) {
                aggregator.add((Promise<Boolean>) shutdown);
            } else {
                aggregator.add(toBooleanPromise(shutdown));
            }
        }

        if (!sharedEventExecutor) {
            Future<?> shutdown = eventExecutorGroup.shutdownGracefully(quietPeriod, timeout, timeUnit);
            aggregator.add(toBooleanPromise(shutdown));
        }

        if (!sharedCommandLatencyCollector) {
            commandLatencyCollector.shutdown();
        }

        aggregator.add(lastRelease);
        lastRelease.setSuccess(null);

        return toBooleanPromise(overall);
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
    public int computationThreadPoolSize() {
        return LettuceLists.newList(eventExecutorGroup.iterator()).size();
    }

    @Override
    public EventBus eventBus() {
        return eventBus;
    }

    @Override
    public Timer timer() {
        return timer;
    }

    @Override
    public CommandLatencyCollector commandLatencyCollector() {
        return commandLatencyCollector;
    }

    @Override
    public EventPublisherOptions commandLatencyPublisherOptions() {
        return commandLatencyPublisherOptions;
    }

    @Override
    public DnsResolver dnsResolver() {
        return dnsResolver;
    }

    @Override
    public Delay reconnectDelay() {
        return reconnectDelay.get();
    }

    @Override
    public NettyCustomizer nettyCustomizer() {
        return nettyCustomizer;
    }
}
