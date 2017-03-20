/*
 * Copyright 2011-2017 the original author or authors.
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

import java.net.SocketAddress;

/**
 * Exception for connection failures.
 *
 * @author Mark Paluch
 */
@SuppressWarnings("serial")
public class RedisConnectionException extends RedisException {

    public RedisConnectionException(String msg) {
        super(msg);
    }

    public RedisConnectionException(String msg, Throwable e) {
        super(msg, e);
    }

    /**
     * Create a new {@link RedisConnectionException} given {@link SocketAddress} and the {@link Throwable cause}.
     *
     * @param remoteAddress
     * @param cause
     * @return
     * @since 4.4
     */
    public static RedisConnectionException create(SocketAddress remoteAddress, Throwable cause) {
        return new RedisConnectionException(String.format("Unable to connect to %s", remoteAddress), cause);
    }
}
