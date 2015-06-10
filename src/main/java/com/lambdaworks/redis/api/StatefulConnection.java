package com.lambdaworks.redis.api;

import java.util.concurrent.TimeUnit;

import com.lambdaworks.redis.ClientOptions;
import com.lambdaworks.redis.protocol.RedisCommand;

/**
 * A stateful connection providing command dispatching, timeouts and open/close methods.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
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
     * instance is returned after the call.
     *
     * @param command the redis command
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
}
