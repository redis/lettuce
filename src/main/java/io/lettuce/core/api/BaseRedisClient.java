package io.lettuce.core.api;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import io.lettuce.core.RedisConnectionStateListener;
import io.lettuce.core.annotations.Experimental;
import io.lettuce.core.event.command.CommandListener;
import io.lettuce.core.resource.ClientResources;
import io.netty.util.concurrent.EventExecutorGroup;

/**
 * @author Ali Takavci
 * @since 7.4
 */
@Experimental
// This is same interface that AbstractRedisClient expose as public
public interface BaseRedisClient extends AutoCloseable {

    /**
     * Add a listener for the RedisConnectionState. The listener is notified every time a connect/disconnect/IO exception
     * happens. The listeners are not bound to a specific connection, so every time a connection event happens on any
     * connection, the listener will be notified. The corresponding netty channel handler (async connection) is passed on the
     * event.
     *
     * @param listener must not be {@code null}.
     */
    void addListener(RedisConnectionStateListener listener);

    /**
     * Removes a listener.
     *
     * @param listener must not be {@code null}.
     */
    void removeListener(RedisConnectionStateListener listener);

    /**
     * Add a listener for Redis Command events. The listener is notified on each command start/success/failure.
     *
     * @param listener must not be {@code null}.
     * @since 6.1
     */
    void addListener(CommandListener listener);

    /**
     * Removes a listener.
     *
     * @param listener must not be {@code null}.
     * @since 6.1
     */
    void removeListener(CommandListener listener);

    /**
     * Shutdown this client and close all open connections once this method is called. Once all connections are closed, the
     * associated {@link ClientResources} are shut down/released gracefully considering quiet time and the shutdown timeout. The
     * client should be discarded after calling shutdown. The shutdown is executed without quiet time and a timeout of 2
     * {@link TimeUnit#SECONDS}.
     *
     * @see EventExecutorGroup#shutdownGracefully(long, long, TimeUnit)
     */
    void shutdown();

    /**
     * Shutdown this client and close all open connections once this method is called. Once all connections are closed, the
     * associated {@link ClientResources} are shut down/released gracefully considering quiet time and the shutdown timeout. The
     * client should be discarded after calling shutdown.
     *
     * @param quietPeriod the quiet period to allow the executor gracefully shut down.
     * @param timeout the maximum amount of time to wait until the backing executor is shutdown regardless if a task was
     *        submitted during the quiet period.
     * @since 5.0
     * @see EventExecutorGroup#shutdownGracefully(long, long, TimeUnit)
     */
    void shutdown(Duration quietPeriod, Duration timeout);

    /**
     * Shutdown this client and close all open connections once this method is called. Once all connections are closed, the
     * associated {@link ClientResources} are shut down/released gracefully considering quiet time and the shutdown timeout. The
     * client should be discarded after calling shutdown.
     *
     * @param quietPeriod the quiet period to allow the executor gracefully shut down.
     * @param timeout the maximum amount of time to wait until the backing executor is shutdown regardless if a task was
     *        submitted during the quiet period.
     * @param timeUnit the unit of {@code quietPeriod} and {@code timeout}.
     * @see EventExecutorGroup#shutdownGracefully(long, long, TimeUnit)
     */
    void shutdown(long quietPeriod, long timeout, TimeUnit timeUnit);

    /**
     * Shutdown this client and close all open connections asynchronously. Once all connections are closed, the associated
     * {@link ClientResources} are shut down/released gracefully considering quiet time and the shutdown timeout. The client
     * should be discarded after calling shutdown. The shutdown is executed without quiet time and a timeout of 2
     * {@link TimeUnit#SECONDS}.
     *
     * @since 4.4
     * @see EventExecutorGroup#shutdownGracefully(long, long, TimeUnit)
     */
    CompletableFuture<Void> shutdownAsync();

    /**
     * Shutdown this client and close all open connections asynchronously. Once all connections are closed, the associated
     * {@link ClientResources} are shut down/released gracefully considering quiet time and the shutdown timeout. The client
     * should be discarded after calling shutdown.
     *
     * @param quietPeriod the quiet period to allow the executor gracefully shut down.
     * @param timeout the maximum amount of time to wait until the backing executor is shutdown regardless if a task was
     *        submitted during the quiet period.
     * @param timeUnit the unit of {@code quietPeriod} and {@code timeout}.
     * @since 4.4
     * @see EventExecutorGroup#shutdownGracefully(long, long, TimeUnit)
     */
    CompletableFuture<Void> shutdownAsync(long quietPeriod, long timeout, TimeUnit timeUnit);

}
