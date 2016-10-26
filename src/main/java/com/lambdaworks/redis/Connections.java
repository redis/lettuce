/*
 * Copyright 2011-2016 the original author or authors.
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
package com.lambdaworks.redis;

import java.util.concurrent.ExecutionException;

import com.lambdaworks.redis.internal.LettuceAssert;

/**
 * Utility for checking a connection's state.
 * 
 * @author Mark Paluch
 * @since 3.0
 */
class Connections {

    /**
     * Utility constructor.
     */
    private Connections() {

    }

    /**
     * 
     * @param connection must be either a {@link com.lambdaworks.redis.RedisAsyncConnection} or
     *        {@link com.lambdaworks.redis.RedisConnection} and must not be {@literal null}
     * @return true if the connection is valid (ping works)
     * @throws java.lang.NullPointerException if connection is null
     * @throws java.lang.IllegalArgumentException if connection is not a supported type
     */
    public static final boolean isValid(Object connection) {

        LettuceAssert.notNull(connection, "Connection must not be null");
        if (connection instanceof RedisAsyncConnection<?, ?>) {
            RedisAsyncConnection<?, ?> redisAsyncConnection = (RedisAsyncConnection<?, ?>) connection;
            try {
                redisAsyncConnection.ping().get();
                return true;
            } catch (ExecutionException | RuntimeException e) {
                return false;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RedisCommandInterruptedException(e);
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
     * @param connection must be either a {@link com.lambdaworks.redis.RedisAsyncConnection} or
     *        {@link com.lambdaworks.redis.RedisConnection} and must not be {@literal null}
     * @return true if the connection is open.
     * @throws java.lang.NullPointerException if connection is null
     * @throws java.lang.IllegalArgumentException if connection is not a supported type
     */
    public static final boolean isOpen(Object connection) {

        LettuceAssert.notNull(connection, "Connection must not be null");
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
     * Closes silently a connection.
     * 
     * @param connection must be either a {@link com.lambdaworks.redis.RedisAsyncConnection} or
     *        {@link com.lambdaworks.redis.RedisConnection} and must not be {@literal null}
     * @throws java.lang.NullPointerException if connection is null
     * @throws java.lang.IllegalArgumentException if connection is not a supported type
     */
    public static void close(Object connection) {

        LettuceAssert.notNull(connection, "Connection must not be null");
        try {
            if (connection instanceof RedisAsyncConnection<?, ?>) {
                RedisAsyncConnection<?, ?> redisAsyncConnection = (RedisAsyncConnection<?, ?>) connection;
                redisAsyncConnection.close();
                return;
            }

            if (connection instanceof RedisConnection<?, ?>) {
                RedisConnection<?, ?> redisConnection = (RedisConnection<?, ?>) connection;
                redisConnection.close();
                return;
            }
        } catch (RuntimeException e) {
            return;
        }
        throw new IllegalArgumentException("Connection class " + connection.getClass() + " not supported");

    }
}
