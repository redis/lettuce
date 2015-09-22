package com.lambdaworks.redis.resource;

import java.util.concurrent.TimeUnit;

import com.lambdaworks.redis.event.EventBus;
import com.lambdaworks.redis.event.EventPublisherOptions;
import com.lambdaworks.redis.metrics.CommandLatencyCollector;

import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.Future;

/**
 * Client Configuration. The client configuration provides heavy-weight resources such as thread pools. {@link ClientResources}
 * can be shared across different client instances. Shared instances are not shut down by the client, only dedicated instances
 * are shut down.
 * <p>
 * This interface defines the contract. See the {@link DefaultClientResources} class for the default implementation.
 * </p>
 * <p>
 * The {@link ClientResources} instance is stateful. You have to shutdown the instance if you're no longer using it.
 * </p>
 *
 * {@link ClientResources} provide:
 * <ul>
 * <li>An instance of {@link EventLoopGroupProvider} to obtain particular {@link io.netty.channel.EventLoopGroup
 * EventLoopGroups}</li>
 * <li>An instance of {@link EventExecutorGroup} for performing internal computation tasks</li>
 * </ul>
 *
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 3.4
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
     * Return s the {@link EventLoopGroupProvider} which provides access to the particular
     * {@link io.netty.channel.EventLoopGroup event loop groups}. lettuce needs at least two implementations:
     * {@link io.netty.channel.nio.NioEventLoopGroup} for TCP/IP connections and
     * {@link io.netty.channel.epoll.EpollEventLoopGroup} for unix domain socket connections (epoll).
     * 
     * @return the {@link EventLoopGroupProvider} which provides access to the particular
     *         {@link io.netty.channel.EventLoopGroup event loop groups}
     */
    EventLoopGroupProvider eventLoopGroupProvider();

    /**
     * Returns the computation pool used for internal operations.
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

}
