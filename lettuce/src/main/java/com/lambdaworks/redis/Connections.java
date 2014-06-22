package com.lambdaworks.redis;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.ExecutionException;

/**
 * Utility for checking a connection's state.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 14.05.14 22:05
 */
class Connections {

    /**
     * Utility constructor.
     */
    private Connections() {

    }

    /**
     * 
     * @param connection
     * @return true if the connection is valid (ping works).
     */
    public static final boolean isValid(Object connection) {

        checkNotNull(connection, "connection must not be null");
        if (connection instanceof RedisAsyncConnection<?, ?>) {
            RedisAsyncConnection<?, ?> redisAsyncConnection = (RedisAsyncConnection<?, ?>) connection;
            try {
                redisAsyncConnection.ping().get();
                return true;
            } catch (RuntimeException e) {
                return false;
            } catch (InterruptedException e) {
                return false;
            } catch (ExecutionException e) {
                return false;
            }
        }

        if (connection instanceof RedisConnection<?, ?>) {
            RedisConnection<?, ?> redisConnection = (RedisConnection<?, ?>) connection;
            try {
                redisConnection.ping();
                return true;
            } catch (RuntimeException e) {
                return false;
            }
        }

        throw new IllegalArgumentException("Connection class " + connection.getClass() + " not supported");
    }

    /**
     * 
     * @param connection
     * @return true if the connection is open.
     */
    public static final boolean isOpen(Object connection) {

        checkNotNull(connection, "connection must not be null");
        if (connection instanceof RedisAsyncConnection<?, ?>) {
            RedisAsyncConnection<?, ?> redisAsyncConnection = (RedisAsyncConnection<?, ?>) connection;
            return redisAsyncConnection.isOpen();
        }

        if (connection instanceof RedisConnection<?, ?>) {
            RedisConnection<?, ?> redisConnection = (RedisConnection<?, ?>) connection;
            return redisConnection.isOpen();
        }

        throw new IllegalArgumentException("Connection class " + connection.getClass() + " not supported");
    }

    /**
     * Closes a connection.
     * 
     * @param connection
     */
    public static void close(Object connection) {

        checkNotNull(connection, "connection must not be null");
        try {
            if (connection instanceof RedisAsyncConnection<?, ?>) {
                RedisAsyncConnection<?, ?> redisAsyncConnection = (RedisAsyncConnection<?, ?>) connection;
                redisAsyncConnection.close();
            }

            if (connection instanceof RedisConnection<?, ?>) {
                RedisConnection<?, ?> redisConnection = (RedisConnection<?, ?>) connection;
                redisConnection.close();
            }
        } catch (RuntimeException e) {
            // silent;
        }
        throw new IllegalArgumentException("Connection class " + connection.getClass() + " not supported");

    }
}
