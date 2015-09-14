package com.lambdaworks.redis.resource;

import java.util.concurrent.TimeUnit;

import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.Future;

/**
 * Client Configuration. The client configuration provides heavy-weight resources such as thread pools. {@link ClientResources}
 * can be shared across different client instances. Shared instances are not shut down by the client, only dedicated instances
 * are shut down.
 *
 * This interface defines the contract. See the {@link DefaultClientResources} class for the default implementation.
 *
 * The {@link ClientResources} instance is stateful. You have to shutdown the instance if you're no longer using it.
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

}
