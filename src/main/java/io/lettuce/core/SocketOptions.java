/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
 * @author Bodong Ybd
 * @since 4.3
 */
public class SocketOptions {

    public static final long DEFAULT_CONNECT_TIMEOUT = 10;

    public static final TimeUnit DEFAULT_CONNECT_TIMEOUT_UNIT = TimeUnit.SECONDS;

    public static final Duration DEFAULT_CONNECT_TIMEOUT_DURATION = Duration.ofSeconds(DEFAULT_CONNECT_TIMEOUT);

    public static final boolean DEFAULT_SO_KEEPALIVE = false;

    public static final boolean DEFAULT_TCP_USER_TIMEOUT_ENABLED = false;

    public static final boolean DEFAULT_SO_NO_DELAY = true;

    private final Duration connectTimeout;

    private final KeepAliveOptions keepAlive;

    private final TcpUserTimeoutOptions tcpUserTimeout;

    private final boolean extendedKeepAlive;

    private final boolean tcpNoDelay;

    protected SocketOptions(Builder builder) {

        this.connectTimeout = builder.connectTimeout;
        this.keepAlive = builder.keepAlive;
        this.extendedKeepAlive = builder.extendedKeepAlive;
        this.tcpNoDelay = builder.tcpNoDelay;
        this.tcpUserTimeout = builder.tcpUserTimeout;
    }

    protected SocketOptions(SocketOptions original) {
        this.connectTimeout = original.getConnectTimeout();
        this.keepAlive = original.getKeepAlive();
        this.extendedKeepAlive = original.isExtendedKeepAlive();
        this.tcpNoDelay = original.isTcpNoDelay();
        this.tcpUserTimeout = original.getTcpUserTimeout();
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

        private KeepAliveOptions keepAlive = KeepAliveOptions.builder().enable(DEFAULT_SO_KEEPALIVE).build();

        private TcpUserTimeoutOptions tcpUserTimeout = TcpUserTimeoutOptions.builder().enable(DEFAULT_TCP_USER_TIMEOUT_ENABLED)
                .build();

        private boolean tcpNoDelay = DEFAULT_SO_NO_DELAY;

        private boolean extendedKeepAlive = false;

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
         * @param connectTimeoutUnit unit for {@code connectTimeout}, must not be {@code null}.
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
         * Set whether to enable TCP keepalive. Defaults to {@code false}. See {@link #DEFAULT_SO_KEEPALIVE}.
         *
         * @param keepAlive whether to enable or disable the TCP keepalive.
         * @return {@code this}
         * @see java.net.SocketOptions#SO_KEEPALIVE
         */
        public Builder keepAlive(boolean keepAlive) {

            this.keepAlive = KeepAliveOptions.builder().enable(keepAlive).build();
            this.extendedKeepAlive = false;

            return this;
        }

        /**
         * Configure TCP keepalive. Defaults to disabled. See {@link #DEFAULT_SO_KEEPALIVE}.
         *
         * @param keepAlive whether to enable or disable the TCP keepalive.
         * @return {@code this}
         * @since 6.1
         * @see KeepAliveOptions
         * @see java.net.SocketOptions#SO_KEEPALIVE
         */
        public Builder keepAlive(KeepAliveOptions keepAlive) {

            LettuceAssert.notNull(keepAlive, "KeepAlive options must not be null");

            this.keepAlive = keepAlive;
            this.extendedKeepAlive = true;

            return this;
        }

        /**
         * Configure TCP User Timeout. Defaults to disabled. See {@link #DEFAULT_TCP_USER_TIMEOUT_ENABLED}.
         *
         * @param tcpUserTimeout the TCP User Timeout options.
         * @return {@code this}
         * @since 6.2.7
         * @see TcpUserTimeoutOptions
         */
        public Builder tcpUserTimeout(TcpUserTimeoutOptions tcpUserTimeout) {

            LettuceAssert.notNull(tcpUserTimeout, "TcpUserTimeout options must not be null");

            this.tcpUserTimeout = tcpUserTimeout;

            return this;
        }

        /**
         * Set whether to disable/enable Nagle's algorithm. Defaults to {@code true} (Nagle disabled). See
         * {@link #DEFAULT_SO_NO_DELAY}.
         * <p>
         * Disabling TCP NoDelay delays TCP {@code ACK} packets to await more data input before confirming the packet.
         *
         * @param tcpNoDelay {@code false} to disable TCP NoDelay (enable Nagle's algorithm), {@code true} to enable TCP NoDelay
         *        (disable Nagle's algorithm).
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
     * Returns a builder to create new {@link SocketOptions} whose settings are replicated from the current
     * {@link SocketOptions}.
     *
     * @return a {@link SocketOptions.Builder} to create new {@link SocketOptions} whose settings are replicated from the
     *         current {@link SocketOptions}
     *
     * @since 5.3
     */
    public SocketOptions.Builder mutate() {

        SocketOptions.Builder builder = builder();

        builder.connectTimeout = this.getConnectTimeout();
        builder.keepAlive = this.getKeepAlive();
        builder.tcpNoDelay = this.isTcpNoDelay();

        return builder;
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
        return keepAlive.isEnabled();
    }

    /**
     * Returns the TCP keepalive options.
     *
     * @return TCP keepalive options
     * @see KeepAliveOptions
     */
    public KeepAliveOptions getKeepAlive() {
        return keepAlive;
    }

    boolean isExtendedKeepAlive() {
        return extendedKeepAlive;
    }

    /**
     * Returns whether to use TCP NoDelay.
     *
     * @return {@code true} to disable Nagle's algorithm, {@code false} to enable Nagle's algorithm.
     * @see java.net.SocketOptions#TCP_NODELAY
     */
    public boolean isTcpNoDelay() {
        return tcpNoDelay;
    }

    public boolean isEnableTcpUserTimeout() {
        return tcpUserTimeout.isEnabled();
    }

    public TcpUserTimeoutOptions getTcpUserTimeout() {
        return tcpUserTimeout;
    }

    /**
     * Extended Keep-Alive options (idle, interval, count). Extended options should not be used in code intended to be portable
     * as options are applied only when using NIO sockets with Java 11 or newer epoll sockets, or io_uring sockets. Not
     * applicable for kqueue in general or NIO sockets using Java 10 or earlier.
     * <p>
     * The time granularity of {@link #getIdle()} and {@link #getInterval()} is seconds.
     *
     * @since 6.1
     */
    public static class KeepAliveOptions {

        public static final int DEFAULT_COUNT = 9;

        public static final Duration DEFAULT_IDLE = Duration.ofHours(2);

        public static final Duration DEFAULT_INTERVAL = Duration.ofSeconds(75);

        private final int count;

        private final boolean enabled;

        private final Duration idle;

        private final Duration interval;

        private KeepAliveOptions(KeepAliveOptions.Builder builder) {

            this.count = builder.count;
            this.enabled = builder.enabled;
            this.idle = builder.idle;
            this.interval = builder.interval;
        }

        /**
         * Returns a new {@link KeepAliveOptions.Builder} to construct {@link KeepAliveOptions}.
         *
         * @return a new {@link KeepAliveOptions.Builder} to construct {@link KeepAliveOptions}.
         */
        public static KeepAliveOptions.Builder builder() {
            return new KeepAliveOptions.Builder();
        }

        /**
         * Builder for {@link KeepAliveOptions}.
         */
        public static class Builder {

            private int count = DEFAULT_COUNT;

            private boolean enabled = DEFAULT_SO_KEEPALIVE;

            private Duration idle = DEFAULT_IDLE;

            private Duration interval = DEFAULT_INTERVAL;

            private Builder() {
            }

            /**
             * Set the maximum number of keepalive probes TCP should send before dropping the connection. Defaults to {@code 9}.
             * See also {@link #DEFAULT_COUNT} and {@code TCP_KEEPCNT}.
             *
             * @param count the maximum number of keepalive probes TCP
             * @return {@code this}
             */
            public KeepAliveOptions.Builder count(int count) {

                LettuceAssert.isTrue(count >= 0, "Count must be greater 0");

                this.count = count;
                return this;
            }

            /**
             * Enable TCP keepalive. Defaults to disabled. See {@link #DEFAULT_SO_KEEPALIVE}.
             *
             * @return {@code this}
             * @see java.net.SocketOptions#SO_KEEPALIVE
             */
            public KeepAliveOptions.Builder enable() {
                return enable(true);
            }

            /**
             * Disable TCP keepalive. Defaults to disabled. See {@link #DEFAULT_SO_KEEPALIVE}.
             *
             * @return {@code this}
             * @see java.net.SocketOptions#SO_KEEPALIVE
             */
            public KeepAliveOptions.Builder disable() {
                return enable(false);
            }

            /**
             * Enable TCP keepalive. Defaults to {@code false}. See {@link #DEFAULT_SO_KEEPALIVE}.
             *
             * @param enabled whether to enable TCP keepalive.
             * @return {@code this}
             * @see java.net.SocketOptions#SO_KEEPALIVE
             */
            public KeepAliveOptions.Builder enable(boolean enabled) {

                this.enabled = enabled;
                return this;
            }

            /**
             * The time the connection needs to remain idle before TCP starts sending keepalive probes if keepalive is enabled.
             * Defaults to {@code 2 hours}. See also @link {@link #DEFAULT_IDLE} and {@code TCP_KEEPIDLE}.
             * <p>
             * The time granularity of is seconds.
             *
             * @param idle connection idle time, must be greater {@literal 0}.
             * @return {@code this}
             */
            public KeepAliveOptions.Builder idle(Duration idle) {

                LettuceAssert.notNull(idle, "Idle time must not be null");
                LettuceAssert.isTrue(!idle.isNegative(), "Idle time must not be begative");

                this.idle = idle;
                return this;
            }

            /**
             * The time between individual keepalive probes. Defaults to {@code 75 second}. See also {@link #DEFAULT_INTERVAL}
             * and {@code TCP_KEEPINTVL}.
             * <p>
             * The time granularity of is seconds.
             *
             * @param interval connection interval time, must be greater {@literal 0}
             * @return {@code this}
             */
            public KeepAliveOptions.Builder interval(Duration interval) {

                LettuceAssert.notNull(interval, "Idle time must not be null");
                LettuceAssert.isTrue(!interval.isNegative(), "Idle time must not be begative");

                this.interval = interval;
                return this;
            }

            /**
             * Create a new instance of {@link KeepAliveOptions}
             *
             * @return new instance of {@link KeepAliveOptions}
             */
            public KeepAliveOptions build() {
                return new KeepAliveOptions(this);
            }

        }

        /**
         * Returns a builder to create new {@link KeepAliveOptions} whose settings are replicated from the current
         * {@link KeepAliveOptions}.
         *
         * @return a {@link KeepAliveOptions.Builder} to create new {@link KeepAliveOptions} whose settings are replicated from
         *         the current {@link KeepAliveOptions}
         */
        public KeepAliveOptions.Builder mutate() {

            KeepAliveOptions.Builder builder = builder();

            builder.enabled = this.isEnabled();
            builder.count = this.getCount();
            builder.idle = this.getIdle();
            builder.interval = this.getInterval();

            return builder;
        }

        /**
         * Returns the maximum number of keepalive probes TCP should send before dropping the connection. Defaults to {@code 9}.
         * See also {@link #DEFAULT_COUNT} and {@code TCP_KEEPCNT}.
         *
         * @return the maximum number of keepalive probes TCP should send before dropping the connection.
         */
        public int getCount() {
            return count;
        }

        /**
         * Returns whether to enable TCP keepalive.
         *
         * @return whether to enable TCP keepalive
         * @see java.net.SocketOptions#SO_KEEPALIVE
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * The time the connection needs to remain idle before TCP starts sending keepalive probes if keepalive is enabled.
         * Defaults to {@code 2 hours}. See also @link {@link #DEFAULT_IDLE} and {@code TCP_KEEPIDLE}.
         * <p>
         * The time granularity of is seconds.
         *
         * @return the time the connection needs to remain idle before TCP starts sending keepalive probes.
         */
        public Duration getIdle() {
            return idle;
        }

        /**
         * The time between individual keepalive probes. Defaults to {@code 1 second}. See also {@link #DEFAULT_INTERVAL} and
         * {@code TCP_KEEPINTVL}.
         * <p>
         * The time granularity of is seconds.
         *
         * @return the time the connection needs to remain idle before TCP starts sending keepalive probes.
         */
        public Duration getInterval() {
            return interval;
        }

    }

    /**
     * TCP_USER_TIMEOUT comes from <a href="https://datatracker.ietf.org/doc/html/rfc5482">RFC5482</a> , configuring this
     * parameter can allow the user TCP to initiate a reconnection to solve this problem when the network is abnormal.
     * <p>
     * The timeout is currently only supported with epoll and io_uring native transports.
     *
     * @since 6.2.7
     */
    public static class TcpUserTimeoutOptions {

        /**
         * Recommended default TCP_KEEPIDLE(2 hour) + TCP_KEEPINTVL(75 s) * TCP_KEEPCNT(9) 2 * 3600 + 75 * 9 = 7875
         */
        public static final Duration DEFAULT_TCP_USER_TIMEOUT = Duration.ofSeconds(7875);

        private final Duration tcpUserTimeout;

        private final boolean enabled;

        private TcpUserTimeoutOptions(TcpUserTimeoutOptions.Builder builder) {

            this.tcpUserTimeout = builder.tcpUserTimeout;
            this.enabled = builder.enabled;
        }

        /**
         * Returns a new {@link TcpUserTimeoutOptions.Builder} to construct {@link TcpUserTimeoutOptions}.
         *
         * @return a new {@link TcpUserTimeoutOptions.Builder} to construct {@link TcpUserTimeoutOptions}.
         */
        public static TcpUserTimeoutOptions.Builder builder() {
            return new TcpUserTimeoutOptions.Builder();
        }

        /**
         * Builder class for {@link TcpUserTimeoutOptions}.
         */
        public static class Builder {

            private Duration tcpUserTimeout = DEFAULT_TCP_USER_TIMEOUT;

            private boolean enabled = DEFAULT_TCP_USER_TIMEOUT_ENABLED;

            private Builder() {
            }

            /**
             * Enable TCP User Timeout. Defaults to disabled. See {@link #DEFAULT_TCP_USER_TIMEOUT_ENABLED}.
             *
             * @return {@code this}
             */
            public TcpUserTimeoutOptions.Builder enable() {
                return enable(true);
            }

            /**
             * Disable TCP User Timeout. Defaults to disabled. See {@link #DEFAULT_TCP_USER_TIMEOUT_ENABLED}.
             *
             * @return {@code this}
             */
            public TcpUserTimeoutOptions.Builder disable() {
                return enable(false);
            }

            /**
             * Enable or disable TCP User Timeout. Defaults to disabled. See {@link #DEFAULT_TCP_USER_TIMEOUT_ENABLED}.
             *
             * @param enabled whether to enable TCP User Timeout.
             * @return {@code this}
             */
            public TcpUserTimeoutOptions.Builder enable(boolean enabled) {

                this.enabled = enabled;
                return this;
            }

            /**
             * The TCP User Timeout. Defaults to {@code 7875 second}. See also {@link #DEFAULT_TCP_USER_TIMEOUT}.
             * <p>
             * The time granularity of is seconds.
             *
             * @param tcpUserTimeout connection interval time, must be greater {@literal 0}
             * @return {@code this}
             */
            public TcpUserTimeoutOptions.Builder tcpUserTimeout(Duration tcpUserTimeout) {

                LettuceAssert.notNull(tcpUserTimeout, "Duration must not be null");
                LettuceAssert.isTrue(!tcpUserTimeout.isNegative(), "Duration must not be negative");

                this.tcpUserTimeout = tcpUserTimeout;
                return this;
            }

            /**
             * Create a new instance of {@link TcpUserTimeoutOptions}
             *
             * @return new instance of {@link TcpUserTimeoutOptions}
             */
            public TcpUserTimeoutOptions build() {
                return new TcpUserTimeoutOptions(this);
            }

        }

        /**
         * Returns a builder to create new {@link TcpUserTimeoutOptions} whose settings are replicated from the current
         * {@link TcpUserTimeoutOptions}.
         *
         * @return a {@link TcpUserTimeoutOptions.Builder} to create new {@link TcpUserTimeoutOptions} whose settings are
         *         replicated from the current {@link TcpUserTimeoutOptions}
         */
        public TcpUserTimeoutOptions.Builder mutate() {

            TcpUserTimeoutOptions.Builder builder = builder();

            builder.enabled = this.isEnabled();
            builder.tcpUserTimeout = this.getTcpUserTimeout();

            return builder;
        }

        /**
         * Returns whether to enable TCP User Timeout.
         *
         * @return whether to enable TCP User Timeout
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Returns the actual timeout.
         *
         * @return the actual timeout.
         */
        public Duration getTcpUserTimeout() {
            return tcpUserTimeout;
        }

    }

}
