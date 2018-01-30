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
package io.lettuce.core;

import java.net.SocketAddress;

/**
 * Exception for connection failures.
 *
 * @author Mark Paluch
 */
@SuppressWarnings("serial")
public class RedisConnectionException extends RedisException {

    /**
     * Create a {@code RedisConnectionException} with the specified detail message.
     *
     * @param msg the detail message.
     */
    public RedisConnectionException(String msg) {
        super(msg);
    }

    /**
     * Create a {@code RedisConnectionException} with the specified detail message and nested exception.
     *
     * @param msg the detail message.
     * @param cause the nested exception.
     */
    public RedisConnectionException(String msg, Throwable cause) {
        super(msg, cause);
    }

    /**
     * Create a new {@link RedisConnectionException} given {@link SocketAddress} and the {@link Throwable cause}.
     *
     * @param remoteAddress remote socket address.
     * @param cause the nested exception.
     * @return the {@link RedisConnectionException}.
     * @since 4.4
     */
    public static RedisConnectionException create(SocketAddress remoteAddress, Throwable cause) {
        return new RedisConnectionException(String.format("Unable to connect to %s", remoteAddress), cause);
    }

    /**
     * @param error the error message.
     * @return {@literal true} if the {@code error} message indicates Redis protected mode.
     * @since 5.0.1
     */
    public static boolean isProtectedMode(String error) {
        return error != null && error.startsWith("DENIED");
    }
}
