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

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import io.lettuce.core.internal.LettuceAssert;

/**
 * Options to configure low-level socket options for the connections kept to Redis servers.
 *
 * @author Mark Paluch
 * @since 4.3
 */
public class SocketOptions {

    public static final long DEFAULT_CONNECT_TIMEOUT = 10;
    public static final TimeUnit DEFAULT_CONNECT_TIMEOUT_UNIT = TimeUnit.SECONDS;
    public static final Duration DEFAULT_CONNECT_TIMEOUT_DURATION = Duration.ofSeconds(DEFAULT_CONNECT_TIMEOUT);

    public static final boolean DEFAULT_SO_KEEPALIVE = false;
    public static final boolean DEFAULT_SO_NO_DELAY = false;

    private final Duration connectTimeout;
    private final boolean keepAlive;
    private final boolean tcpNoDelay;

    protected SocketOptions(Builder builder) {

        this.connectTimeout = builder.connectTimeout;
        this.keepAlive = builder.keepAlive;
        this.tcpNoDelay = builder.tcpNoDelay;
    }

    protected SocketOptions(SocketOptions original) {
        this.connectTimeout = original.getConnectTimeout();
        this.keepAlive = original.isKeepAlive();
        this.tcpNoDelay = original.isTcpNoDelay();
    }

    /**
     * Create a copy of {@literal options}
     *
     * @param options the original
     * @return A new instance of {@link SocketOptions} containing the values of {@literal options}
     */
    public static SocketOptions copyOf(SocketOptions options) {
        return new SocketOptions(options);
    }

    /**
     * Returns a new {@link SocketOptions.Builder} to construct {@link SocketOptions}.
     *
     * @return a new {@link SocketOptions.Builder} to construct {@link SocketOptions}.
     */
    public static SocketOptions.Builder builder() {
        return new SocketOptions.Builder();
    }

    /**
     * Create a new {@link SocketOptions} using default settings.
     *
     * @return a new instance of default cluster client client options.
     */
    public static SocketOptions create() {
        return builder().build();
    }

    /**
     * Builder for {@link SocketOptions}.
     */
    public static class Builder {

        private Duration connectTimeout = DEFAULT_CONNECT_TIMEOUT_DURATION;
        private boolean keepAlive = DEFAULT_SO_KEEPALIVE;
        private boolean tcpNoDelay = DEFAULT_SO_NO_DELAY;

        private Builder() {
        }

        /**
         * Set connection timeout. Defaults to {@literal 10 SECONDS}. See {@link #DEFAULT_CONNECT_TIMEOUT} and
         * {@link #DEFAULT_CONNECT_TIMEOUT_UNIT}.
         *
         * @param connectTimeout connection timeout, must be greater {@literal 0}.
         * @return {@code this}
         * @since 5.0
         */
        public Builder connectTimeout(Duration connectTimeout) {

            LettuceAssert.notNull(connectTimeout, "Connection timeout must not be null");
            LettuceAssert.isTrue(connectTimeout.toNanos() > 0, "Connect timeout must be greater 0");

            this.connectTimeout = connectTimeout;
            return this;
        }

        /**
         * Set connection timeout. Defaults to {@literal 10 SECONDS}. See {@link #DEFAULT_CONNECT_TIMEOUT} and
         * {@link #DEFAULT_CONNECT_TIMEOUT_UNIT}.
         *
         * @param connectTimeout connection timeout, must be greater {@literal 0}.
         * @param connectTimeoutUnit unit for {@code connectTimeout}, must not be {@literal null}.
         * @return {@code this}
         * @deprecated since 5.0, use {@link #connectTimeout(Duration)}
         */
        @Deprecated
        public Builder connectTimeout(long connectTimeout, TimeUnit connectTimeoutUnit) {

            LettuceAssert.isTrue(connectTimeout > 0, "Connect timeout must be greater 0");
            LettuceAssert.notNull(connectTimeoutUnit, "TimeUnit must not be null");

            return connectTimeout(Duration.ofNanos(connectTimeoutUnit.toNanos(connectTimeout)));
        }

        /**
         * Sets whether to enable TCP keepalive. Defaults to {@literal false}. See {@link #DEFAULT_SO_KEEPALIVE}.
         *
         * @param keepAlive whether to enable or disable the TCP keepalive.
         * @return {@code this}
         * @see java.net.SocketOptions#SO_KEEPALIVE
         */
        public Builder keepAlive(boolean keepAlive) {

            this.keepAlive = keepAlive;
            return this;
        }

        /**
         * Sets whether to disable Nagle's algorithm. Defaults to {@literal false} (Nagle enabled). See
         * {@link #DEFAULT_SO_NO_DELAY}.
         *
         * @param tcpNoDelay {@literal true} to disable Nagle's algorithm, {@link false} to enable Nagle's algorithm.
         * @return {@code this}
         * @see java.net.SocketOptions#TCP_NODELAY
         */
        public Builder tcpNoDelay(boolean tcpNoDelay) {

            this.tcpNoDelay = tcpNoDelay;
            return this;
        }

        /**
         * Create a new instance of {@link SocketOptions}
         *
         * @return new instance of {@link SocketOptions}
         */
        public SocketOptions build() {
            return new SocketOptions(this);
        }
    }

    /**
     * Returns the connection timeout.
     *
     * @return the connection timeout.
     */
    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * Returns whether to enable TCP keepalive.
     *
     * @return whether to enable TCP keepalive
     * @see java.net.SocketOptions#SO_KEEPALIVE
     */
    public boolean isKeepAlive() {
        return keepAlive;
    }

    /**
     * Returns whether to use TCP NoDelay.
     *
     * @return {@literal true} to disable Nagle's algorithm, {@link false} to enable Nagle's algorithm.
     * @see java.net.SocketOptions#TCP_NODELAY
     */
    public boolean isTcpNoDelay() {
        return tcpNoDelay;
    }
}
