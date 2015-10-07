package com.lambdaworks.redis.resource;

import static com.lambdaworks.redis.resource.Futures.toBooleanPromise;

import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;
import com.lambdaworks.redis.event.*;
import com.lambdaworks.redis.event.metrics.DefaultCommandLatencyEventPublisher;
import com.lambdaworks.redis.event.metrics.MetricEventPublisher;
import com.lambdaworks.redis.metrics.CommandLatencyCollector;
import com.lambdaworks.redis.metrics.CommandLatencyCollectorOptions;
import com.lambdaworks.redis.metrics.DefaultCommandLatencyCollector;
import com.lambdaworks.redis.metrics.DefaultCommandLatencyCollectorOptions;

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
 * <li>a {@code commandLatencyCollector} which is a provided instance of
 * {@link com.lambdaworks.redis.metrics.CommandLatencyCollector}.</li>
 * </ul>
 *
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 3.4
 */
public class DefaultClientResources implements ClientResources {

    protected static final InternalLogger logger = InternalLoggerFactory.getInstance(DefaultClientResources.class);

    public static final int MIN_IO_THREADS = 3;
    public static final int MIN_COMPUTATION_THREADS = 3;

    public static final int DEFAULT_IO_THREADS;
    public static final int DEFAULT_COMPUTATION_THREADS;

    static {
        int threads = Math.max(1,
                SystemPropertyUtil.getInt("io.netty.eventLoopThreads", Runtime.getRuntime().availableProcessors()));

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
    private final EventBus eventBus;
    private final CommandLatencyCollector commandLatencyCollector;
    private final boolean sharedCommandLatencyCollector;
    private final EventPublisherOptions commandLatencyPublisherOptions;
    private final MetricEventPublisher metricEventPublisher;

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
            if (computationThreadPoolSize < MIN_IO_THREADS) {

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

        if (builder.eventBus == null) {
            eventBus = new DefaultEventBus(new RxJavaEventExecutorGroupScheduler(eventExecutorGroup));
        } else {
            eventBus = builder.eventBus;
        }

        if (builder.commandLatencyCollector == null) {
            if (builder.commandLatencyCollectorOptions != null) {
                commandLatencyCollector = new DefaultCommandLatencyCollector(builder.commandLatencyCollectorOptions);
            } else {
                commandLatencyCollector = new DefaultCommandLatencyCollector(DefaultCommandLatencyCollectorOptions.create());
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

    }

    /**
     * Builder for {@link DefaultClientResources}.
     */
    public static class Builder {

        private int ioThreadPoolSize = DEFAULT_IO_THREADS;
        private int computationThreadPoolSize = DEFAULT_COMPUTATION_THREADS;
        private EventExecutorGroup eventExecutorGroup;
        private EventLoopGroupProvider eventLoopGroupProvider;
        private EventBus eventBus;
        private CommandLatencyCollectorOptions commandLatencyCollectorOptions = DefaultCommandLatencyCollectorOptions.create();
        private CommandLatencyCollector commandLatencyCollector;
        private EventPublisherOptions commandLatencyPublisherOptions = DefaultEventPublisherOptions.create();

        public Builder() {
        }

        /**
         * Sets the thread pool size (number of threads to use) for I/O operations (default value is the number of CPUs). The
         * thread pool size is only effective if no {@code eventLoopGroupProvider} is provided.
         *
         * @param ioThreadPoolSize the thread pool size
         * @return this
         */
        public Builder ioThreadPoolSize(int ioThreadPoolSize) {
            this.ioThreadPoolSize = ioThreadPoolSize;
            return this;
        }

        /**
         * Sets a shared {@link EventLoopGroupProvider event executor provider} that can be used across different instances of
         * the RedisClient. The provided {@link EventLoopGroupProvider} instance will not be shut down when shutting down the
         * client resources. You have to take care of that. This is an advanced configuration that should only be used if you
         * know what you are doing.
         *
         * @param eventLoopGroupProvider the shared eventLoopGroupProvider
         * @return this
         */
        public Builder eventLoopGroupProvider(EventLoopGroupProvider eventLoopGroupProvider) {
            this.eventLoopGroupProvider = eventLoopGroupProvider;
            return this;
        }

        /**
         * Sets the thread pool size (number of threads to use) for computation operations (default value is the number of
         * CPUs). The thread pool size is only effective if no {@code eventExecutorGroup} is provided.
         *
         * @param computationThreadPoolSize the thread pool size
         * @return this
         */
        public Builder computationThreadPoolSize(int computationThreadPoolSize) {
            this.computationThreadPoolSize = computationThreadPoolSize;
            return this;
        }

        /**
         * Sets a shared {@link EventExecutorGroup event executor group} that can be used across different instances of the
         * RedisClient. The provided {@link EventExecutorGroup} instance will not be shut down when shutting down the client
         * resources. You have to take care of that. This is an advanced configuration that should only be used if you know what
         * you are doing.
         *
         * @param eventExecutorGroup the shared eventExecutorGroup
         * @return this
         */
        public Builder eventExecutorGroup(EventExecutorGroup eventExecutorGroup) {
            this.eventExecutorGroup = eventExecutorGroup;
            return this;
        }

        /**
         * Sets the {@link EventBus} that can that can be used across different instances of the RedisClient.
         *
         * @param eventBus the event bus
         * @return this
         */
        public Builder eventBus(EventBus eventBus) {
            this.eventBus = eventBus;
            return this;
        }

        /**
         * Sets the {@link EventPublisherOptions} to publish command latency metrics using the {@link EventBus}.
         *
         * @param commandLatencyPublisherOptions the {@link EventPublisherOptions} to publish command latency metrics using the
         *        {@link EventBus}.
         * @return this
         */
        public Builder commandLatencyPublisherOptions(EventPublisherOptions commandLatencyPublisherOptions) {
            this.commandLatencyPublisherOptions = commandLatencyPublisherOptions;
            return this;
        }

        /**
         * Sets the {@link CommandLatencyCollectorOptions} that can that can be used across different instances of the
         * RedisClient. The options are only effective if no {@code commandLatencyCollector} is provided.
         *
         * @param commandLatencyCollectorOptions the command latency collector options
         * @return this
         */
        public Builder commandLatencyCollectorOptions(CommandLatencyCollectorOptions commandLatencyCollectorOptions) {
            this.commandLatencyCollectorOptions = commandLatencyCollectorOptions;
            return this;
        }

        /**
         * Sets the {@link CommandLatencyCollector} that can that can be used across different instances of the RedisClient.
         *
         * @param commandLatencyCollector the command latency collector
         * @return this
         */
        public Builder commandLatencyCollector(CommandLatencyCollector commandLatencyCollector) {
            this.commandLatencyCollector = commandLatencyCollector;
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
        return shutdown(2, 15, TimeUnit.SECONDS);
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
        return Lists.newArrayList(eventExecutorGroup.iterator()).size();
    }

    @Override
    public EventBus eventBus() {
        return eventBus;
    }

    @Override
    public CommandLatencyCollector commandLatencyCollector() {
        return commandLatencyCollector;
    }

    @Override
    public EventPublisherOptions commandLatencyPublisherOptions() {
        return commandLatencyPublisherOptions;
    }

    /**
     * Create a new {@link DefaultClientResources} using default settings.
     * 
     * @return a new instance of a default client resources.
     */
    public static DefaultClientResources create() {
        return new Builder().build();
    }

}
