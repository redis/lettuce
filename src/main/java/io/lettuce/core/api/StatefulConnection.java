package io.lettuce.core.api;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisConnectionStateListener;
import io.lettuce.core.protocol.RedisCommand;
import io.lettuce.core.resource.ClientResources;

/**
 * A stateful connection providing command dispatching, timeouts and open/close methods.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 4.0
 */
public interface StatefulConnection<K, V> extends AutoCloseable, AsyncCloseable {

    /**
     * Add a listener for the {@link RedisConnectionStateListener}. The listener is notified every time a connect/disconnect/IO
     * exception happens. The listener is called on the event loop thread so code within the listener methods must not block.
     *
     * @param listener must not be {@code null}.
     * @since 6.2
     */
    void addListener(RedisConnectionStateListener listener);

    /**
     * Removes a listener.
     *
     * @param listener must not be {@code null}.
     * @since 6.2
     */
    void removeListener(RedisConnectionStateListener listener);

    /**
     * Set the default command timeout for this connection. A zero timeout value indicates to not time out.
     *
     * @param timeout Command timeout.
     * @since 5.0
     */
    void setTimeout(Duration timeout);

    /**
     * @return the timeout.
     */
    Duration getTimeout();

    /**
     * Dispatch a command. Write a command on the channel. The command may be changed/wrapped during write and the written
     * instance is returned after the call. This command does not wait until the command completes and does not guarantee
     * whether the command is executed successfully.
     *
     * @param command the Redis command.
     * @param <T> result type
     * @return the written Redis command.
     */
    <T> RedisCommand<K, V, T> dispatch(RedisCommand<K, V, T> command);

    /**
     * Dispatch multiple command in a single write on the channel. The commands may be changed/wrapped during write and the
     * written instance is returned after the call. This command does not wait until the command completes and does not
     * guarantee whether the command is executed successfully.
     *
     * @param commands the Redis commands.
     * @return the written Redis commands.
     * @since 5.0
     */
    Collection<RedisCommand<K, V, ?>> dispatch(Collection<? extends RedisCommand<K, V, ?>> commands);

    /**
     * Close the connection. The connection will become not usable anymore as soon as this method was called.
     */
    void close();

    /**
     * Request to close the connection and return the {@link CompletableFuture} that is notified about its progress. The
     * connection will become not usable anymore as soon as this method was called.
     *
     * @return a {@link CompletableFuture} that is notified once the operation completes, either because the operation was
     *         successful or because of an error.
     * @since 5.1
     */
    @Override
    CompletableFuture<Void> closeAsync();

    /**
     * @return true if the connection is open (connected and not closed).
     */
    boolean isOpen();

    /**
     * @return the client options valid for this connection.
     */
    ClientOptions getOptions();

    /**
     * @return the client resources used for this connection.
     */
    ClientResources getResources();

    /**
     * Disable or enable auto-flush behavior. Default is {@code true}. If autoFlushCommands is disabled, multiple commands can
     * be issued without writing them actually to the transport. Commands are buffered until a {@link #flushCommands()} is
     * issued. After calling {@link #flushCommands()} commands are sent to the transport and executed by Redis.
     *
     * @param autoFlush state of autoFlush.
     */
    void setAutoFlushCommands(boolean autoFlush);

    /**
     * Flush pending commands. This commands forces a flush on the channel and can be used to buffer ("pipeline") commands to
     * achieve batching. No-op if channel is not connected.
     */
    void flushCommands();

}
