package com.lambdaworks.redis.api;

import java.util.concurrent.TimeUnit;

import com.lambdaworks.redis.ClientOptions;
import com.lambdaworks.redis.protocol.RedisCommand;

/**
 * A stateful connection providing command dispatching, timeouts and open/close methods.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 4.0
 */
public interface StatefulConnection<K, V> extends AutoCloseable {

    /**
     * Set the default command timeout for this connection.
     *
     * @param timeout Command timeout.
     * @param unit Unit of time for the timeout.
     */
    void setTimeout(long timeout, TimeUnit unit);

    /**
     * @return the timeout unit.
     */
    TimeUnit getTimeoutUnit();

    /**
     * @return the timeout.
     */
    long getTimeout();

    /**
     * Dispatch a command. Write a command on the channel. The command may be changed/wrapped during write and the written
     * instance is returned after the call. This command does not wait until the command completes and does not guarantee
     * whether the command is executed successfully.
     *
     * @param command the Redis command
     * @param <T> result type
     * @param <C> command type
     * @return the written redis command
     */
    <T, C extends RedisCommand<K, V, T>> C dispatch(C command);

    /**
     * Close the connection. The connection will become not usable anymore as soon as this method was called.
     */
    void close();

    /**
     * @return true if the connection is open (connected and not closed).
     */
    boolean isOpen();

    /**
     *
     * @return the client options valid for this connection.
     */
    ClientOptions getOptions();

    /**
     * Reset the command state. Queued commands will be canceled and the internal state will be reset. This is useful when the
     * internal state machine gets out of sync with the connection.
     */
    void reset();

    /**
     * Disable or enable auto-flush behavior. Default is {@literal true}. If autoFlushCommands is disabled, multiple commands
     * can be issued without writing them actually to the transport. Commands are buffered until a {@link #flushCommands()} is
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
