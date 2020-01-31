/*
 * Copyright 2011-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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

import java.io.Serializable;

import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.resource.ClientResources;

/**
 * Client Options to control the behavior of {@link RedisClient}.
 *
 * @author Mark Paluch
 * @author Gavin Cook
 */
@SuppressWarnings("serial")
public class ClientOptions implements Serializable {

    public static final boolean DEFAULT_PING_BEFORE_ACTIVATE_CONNECTION = false;
    public static final boolean DEFAULT_AUTO_RECONNECT = true;
    public static final boolean DEFAULT_CANCEL_CMD_RECONNECT_FAIL = false;
    public static final boolean DEFAULT_PUBLISH_ON_SCHEDULER = false;
    public static final boolean DEFAULT_SUSPEND_RECONNECT_PROTO_FAIL = false;
    public static final int DEFAULT_REQUEST_QUEUE_SIZE = Integer.MAX_VALUE;
    public static final DisconnectedBehavior DEFAULT_DISCONNECTED_BEHAVIOR = DisconnectedBehavior.DEFAULT;
    public static final SocketOptions DEFAULT_SOCKET_OPTIONS = SocketOptions.create();
    public static final SslOptions DEFAULT_SSL_OPTIONS = SslOptions.create();
    public static final TimeoutOptions DEFAULT_TIMEOUT_OPTIONS = TimeoutOptions.create();
    public static final int DEFAULT_BUFFER_USAGE_RATIO = 3;

    private final boolean pingBeforeActivateConnection;
    private final boolean autoReconnect;
    private final boolean cancelCommandsOnReconnectFailure;
    private final boolean publishOnScheduler;
    private final boolean suspendReconnectOnProtocolFailure;
    private final int requestQueueSize;
    private final DisconnectedBehavior disconnectedBehavior;
    private final SocketOptions socketOptions;
    private final SslOptions sslOptions;
    private final TimeoutOptions timeoutOptions;
    private final int bufferUsageRatio;

    protected ClientOptions(Builder builder) {
        this.pingBeforeActivateConnection = builder.pingBeforeActivateConnection;
        this.cancelCommandsOnReconnectFailure = builder.cancelCommandsOnReconnectFailure;
        this.publishOnScheduler = builder.publishOnScheduler;
        this.autoReconnect = builder.autoReconnect;
        this.suspendReconnectOnProtocolFailure = builder.suspendReconnectOnProtocolFailure;
        this.requestQueueSize = builder.requestQueueSize;
        this.disconnectedBehavior = builder.disconnectedBehavior;
        this.socketOptions = builder.socketOptions;
        this.sslOptions = builder.sslOptions;
        this.timeoutOptions = builder.timeoutOptions;
        this.bufferUsageRatio = builder.bufferUsageRatio;
    }

    protected ClientOptions(ClientOptions original) {
        this.pingBeforeActivateConnection = original.isPingBeforeActivateConnection();
        this.autoReconnect = original.isAutoReconnect();
        this.cancelCommandsOnReconnectFailure = original.isCancelCommandsOnReconnectFailure();
        this.publishOnScheduler = original.isPublishOnScheduler();
        this.suspendReconnectOnProtocolFailure = original.isSuspendReconnectOnProtocolFailure();
        this.requestQueueSize = original.getRequestQueueSize();
        this.disconnectedBehavior = original.getDisconnectedBehavior();
        this.socketOptions = original.getSocketOptions();
        this.sslOptions = original.getSslOptions();
        this.timeoutOptions = original.getTimeoutOptions();
        this.bufferUsageRatio = original.getBufferUsageRatio();
    }

    /**
     * Create a copy of {@literal options}
     *
     * @param options the original
     * @return A new instance of {@link ClientOptions} containing the values of {@literal options}
     */
    public static ClientOptions copyOf(ClientOptions options) {
        return new ClientOptions(options);
    }

    /**
     * Returns a new {@link ClientOptions.Builder} to construct {@link ClientOptions}.
     *
     * @return a new {@link ClientOptions.Builder} to construct {@link ClientOptions}.
     */
    public static ClientOptions.Builder builder() {
        return new ClientOptions.Builder();
    }

    /**
     * Create a new instance of {@link ClientOptions} with default settings.
     *
     * @return a new instance of {@link ClientOptions} with default settings
     */
    public static ClientOptions create() {
        return builder().build();
    }

    /**
     * Builder for {@link ClientOptions}.
     */
    public static class Builder {

        private boolean pingBeforeActivateConnection = DEFAULT_PING_BEFORE_ACTIVATE_CONNECTION;
        private boolean autoReconnect = DEFAULT_AUTO_RECONNECT;
        private boolean cancelCommandsOnReconnectFailure = DEFAULT_CANCEL_CMD_RECONNECT_FAIL;
        private boolean publishOnScheduler = DEFAULT_PUBLISH_ON_SCHEDULER;
        private boolean suspendReconnectOnProtocolFailure = DEFAULT_SUSPEND_RECONNECT_PROTO_FAIL;
        private int requestQueueSize = DEFAULT_REQUEST_QUEUE_SIZE;
        private DisconnectedBehavior disconnectedBehavior = DEFAULT_DISCONNECTED_BEHAVIOR;
        private SocketOptions socketOptions = DEFAULT_SOCKET_OPTIONS;
        private SslOptions sslOptions = DEFAULT_SSL_OPTIONS;
        private TimeoutOptions timeoutOptions = DEFAULT_TIMEOUT_OPTIONS;
        private int bufferUsageRatio = DEFAULT_BUFFER_USAGE_RATIO;

        protected Builder() {
        }

        /**
         * Sets the {@literal PING} before activate connection flag. Defaults to {@literal false}. See
         * {@link #DEFAULT_PING_BEFORE_ACTIVATE_CONNECTION}.
         *
         * @param pingBeforeActivateConnection true/false
         * @return {@code this}
         */
        public Builder pingBeforeActivateConnection(boolean pingBeforeActivateConnection) {
            this.pingBeforeActivateConnection = pingBeforeActivateConnection;
            return this;
        }

        /**
         * Enables or disables auto reconnection on connection loss. Defaults to {@literal true}. See
         * {@link #DEFAULT_AUTO_RECONNECT}.
         *
         * @param autoReconnect true/false
         * @return {@code this}
         */
        public Builder autoReconnect(boolean autoReconnect) {
            this.autoReconnect = autoReconnect;
            return this;
        }

        /**
         * Suspends reconnect when reconnects run into protocol failures (SSL verification, PING before connect fails). Defaults
         * to {@literal false}. See {@link #DEFAULT_SUSPEND_RECONNECT_PROTO_FAIL}.
         *
         * @param suspendReconnectOnProtocolFailure true/false
         * @return {@code this}
         */
        public Builder suspendReconnectOnProtocolFailure(boolean suspendReconnectOnProtocolFailure) {
            this.suspendReconnectOnProtocolFailure = suspendReconnectOnProtocolFailure;
            return this;
        }

        /**
         * Allows cancelling queued commands in case a reconnect fails.Defaults to {@literal false}. See
         * {@link #DEFAULT_CANCEL_CMD_RECONNECT_FAIL}.
         *
         * @param cancelCommandsOnReconnectFailure true/false
         * @return {@code this}
         */
        public Builder cancelCommandsOnReconnectFailure(boolean cancelCommandsOnReconnectFailure) {
            this.cancelCommandsOnReconnectFailure = cancelCommandsOnReconnectFailure;
            return this;
        }

        /**
         * Use a dedicated {@link reactor.core.scheduler.Scheduler} to emit reactive data signals. Enabling this option can be
         * useful for reactive sequences that require a significant amount of processing with a single/a few Redis connections.
         * <p>
         * A single Redis connection operates on a single thread. Operations that require a significant amount of processing can
         * lead to a single-threaded-like behavior for all consumers of the Redis connection. When enabled, data signals will be
         * emitted using a different thread served by {@link ClientResources#eventExecutorGroup()}. Defaults to {@literal false}
         * , see {@link #DEFAULT_PUBLISH_ON_SCHEDULER}.
         *
         * @param publishOnScheduler true/false
         * @return {@code this}
         * @since 5.2
         * @see org.reactivestreams.Subscriber#onNext(Object)
         * @see ClientResources#eventExecutorGroup()
         */
        public Builder publishOnScheduler(boolean publishOnScheduler) {
            this.publishOnScheduler = publishOnScheduler;
            return this;
        }

        /**
         * Set the per-connection request queue size. The command invocation will lead to a {@link RedisException} if the queue
         * size is exceeded. Setting the {@code requestQueueSize} to a lower value will lead earlier to exceptions during
         * overload or while the connection is in a disconnected state. A higher value means hitting the boundary will take
         * longer to occur, but more requests will potentially be queued up and more heap space is used. Defaults to
         * {@link Integer#MAX_VALUE}. See {@link #DEFAULT_REQUEST_QUEUE_SIZE}.
         *
         * @param requestQueueSize the queue size.
         * @return {@code this}
         */
        public Builder requestQueueSize(int requestQueueSize) {
            this.requestQueueSize = requestQueueSize;
            return this;
        }

        /**
         * Sets the behavior for command invocation when connections are in a disconnected state. Defaults to {@literal true}.
         * See {@link #DEFAULT_DISCONNECTED_BEHAVIOR}.
         *
         * @param disconnectedBehavior must not be {@literal null}.
         * @return {@code this}
         */
        public Builder disconnectedBehavior(DisconnectedBehavior disconnectedBehavior) {

            LettuceAssert.notNull(disconnectedBehavior, "DisconnectedBehavior must not be null");
            this.disconnectedBehavior = disconnectedBehavior;
            return this;
        }

        /**
         * Sets the low-level {@link SocketOptions} for the connections kept to Redis servers. See
         * {@link #DEFAULT_SOCKET_OPTIONS}.
         *
         * @param socketOptions must not be {@literal null}.
         * @return {@code this}
         */
        public Builder socketOptions(SocketOptions socketOptions) {

            LettuceAssert.notNull(socketOptions, "SocketOptions must not be null");
            this.socketOptions = socketOptions;
            return this;
        }

        /**
         * Sets the {@link SslOptions} for SSL connections kept to Redis servers. See {@link #DEFAULT_SSL_OPTIONS}.
         *
         * @param sslOptions must not be {@literal null}.
         * @return {@code this}
         */
        public Builder sslOptions(SslOptions sslOptions) {

            LettuceAssert.notNull(sslOptions, "SslOptions must not be null");
            this.sslOptions = sslOptions;
            return this;
        }

        /**
         * Sets the {@link TimeoutOptions} to expire and cancel commands. See {@link #DEFAULT_TIMEOUT_OPTIONS}.
         *
         * @param timeoutOptions must not be {@literal null}.
         * @return {@code this}
         * @since 5.1
         */
        public Builder timeoutOptions(TimeoutOptions timeoutOptions) {

            LettuceAssert.notNull(timeoutOptions, "TimeoutOptions must not be null");
            this.timeoutOptions = timeoutOptions;
            return this;
        }

        /**
         * Buffer usage ratio for {@link io.lettuce.core.protocol.CommandHandler}. This ratio controls how often bytes are
         * discarded during decoding. In particular, when buffer usage reaches {@code bufferUsageRatio / bufferUsageRatio + 1}.
         * E.g. setting {@code bufferUsageRatio} to {@literal 3}, will discard read bytes once the buffer usage reaches 75
         * percent. See {@link #DEFAULT_BUFFER_USAGE_RATIO}.
         *
         * @param bufferUsageRatio must greater between 0 and 2^31-1, typically a value between 1 and 10 representing 50% to
         *        90%.
         * @return {@code this}
         * @since 5.2
         */
        public Builder bufferUsageRatio(int bufferUsageRatio) {

            LettuceAssert.isTrue(bufferUsageRatio > 0 && bufferUsageRatio < Integer.MAX_VALUE,
                    "BufferUsageRatio must grater than 0");

            this.bufferUsageRatio = bufferUsageRatio;
            return this;
        }

        /**
         * Create a new instance of {@link ClientOptions}.
         *
         * @return new instance of {@link ClientOptions}
         */
        public ClientOptions build() {
            return new ClientOptions(this);
        }
    }

    /**
     * Returns a builder to create new {@link ClientOptions} whose settings are replicated from the current
     * {@link ClientOptions}.
     *
     * @return a {@link ClientOptions.Builder} to create new {@link ClientOptions} whose settings are replicated from the
     *         current {@link ClientOptions}.
     *
     * @since 5.1
     */
    public ClientOptions.Builder mutate() {
        Builder builder = new Builder();

        builder.autoReconnect(isAutoReconnect()).bufferUsageRatio(getBufferUsageRatio())
                .cancelCommandsOnReconnectFailure(isCancelCommandsOnReconnectFailure())
                .disconnectedBehavior(getDisconnectedBehavior()).publishOnScheduler(isPublishOnScheduler())
                .pingBeforeActivateConnection(isPingBeforeActivateConnection()).requestQueueSize(getRequestQueueSize())
                .socketOptions(getSocketOptions()).sslOptions(getSslOptions())
                .suspendReconnectOnProtocolFailure(isSuspendReconnectOnProtocolFailure()).timeoutOptions(getTimeoutOptions());

        return builder;
    }

    /**
     * Enables initial {@literal PING} barrier before any connection is usable. If {@literal true} (default is {@literal false}
     * ), every connection and reconnect will issue a {@literal PING} command and awaits its response before the connection is
     * activated and enabled for use. If the check fails, the connect/reconnect is treated as failure.
     *
     * @return {@literal true} if {@literal PING} barrier is enabled.
     */
    public boolean isPingBeforeActivateConnection() {
        return pingBeforeActivateConnection;
    }

    /**
     * Controls auto-reconnect behavior on connections. If auto-reconnect is {@literal true} (default), it is enabled. As soon
     * as a connection gets closed/reset without the intention to close it, the client will try to reconnect and re-issue any
     * queued commands.
     *
     * This flag has also the effect that disconnected connections will refuse commands and cancel these with an exception.
     *
     * @return {@literal true} if auto-reconnect is enabled.
     */
    public boolean isAutoReconnect() {
        return autoReconnect;
    }

    /**
     * If this flag is {@literal true} any queued commands will be canceled when a reconnect fails within the activation
     * sequence. Default is {@literal false}.
     *
     * @return {@literal true} if commands should be cancelled on reconnect failures.
     */
    public boolean isCancelCommandsOnReconnectFailure() {
        return cancelCommandsOnReconnectFailure;
    }

    /**
     * Use a dedicated {@link reactor.core.scheduler.Scheduler} to emit reactive data signals. Enabling this option can be
     * useful for reactive sequences that require a significant amount of processing with a single/a few Redis connections.
     * <p>
     * A single Redis connection operates on a single thread. Operations that require a significant amount of processing can
     * lead to a single-threaded-like behavior for all consumers of the Redis connection. When enabled, data signals will be
     * emitted using a different thread served by {@link ClientResources#eventExecutorGroup()}. Defaults to {@literal false} ,
     * see {@link #DEFAULT_PUBLISH_ON_SCHEDULER}.
     *
     * @return {@literal true} to use a dedicated {@link reactor.core.scheduler.Scheduler}
     * @since 5.2
     */
    public boolean isPublishOnScheduler() {
        return publishOnScheduler;
    }

    /**
     * If this flag is {@literal true} the reconnect will be suspended on protocol errors. Protocol errors are errors while SSL
     * negotiation or when PING before connect fails.
     *
     * @return {@literal true} if reconnect will be suspended on protocol errors.
     */
    public boolean isSuspendReconnectOnProtocolFailure() {
        return suspendReconnectOnProtocolFailure;
    }

    /**
     * Request queue size for a connection. This value applies per connection. The command invocation will throw a
     * {@link RedisException} if the queue size is exceeded and a new command is requested. Defaults to
     * {@link Integer#MAX_VALUE}.
     *
     * @return the request queue size.
     */
    public int getRequestQueueSize() {
        return requestQueueSize;
    }

    /**
     * Behavior for command invocation when connections are in a disconnected state. Defaults to
     * {@link DisconnectedBehavior#DEFAULT true}. See {@link #DEFAULT_DISCONNECTED_BEHAVIOR}.
     *
     * @return the behavior for command invocation when connections are in a disconnected state
     */
    public DisconnectedBehavior getDisconnectedBehavior() {
        return disconnectedBehavior;
    }

    /**
     * Returns the {@link SocketOptions}.
     *
     * @return the {@link SocketOptions}.
     */
    public SocketOptions getSocketOptions() {
        return socketOptions;
    }

    /**
     * Returns the {@link SslOptions}.
     *
     * @return the {@link SslOptions}.
     */
    public SslOptions getSslOptions() {
        return sslOptions;
    }

    /**
     * Returns the {@link TimeoutOptions}.
     *
     * @return the {@link TimeoutOptions}.
     * @since 5.1
     */
    public TimeoutOptions getTimeoutOptions() {
        return timeoutOptions;
    }

    /**
     * Buffer usage ratio for {@link io.lettuce.core.protocol.CommandHandler}. This ratio controls how often bytes are discarded
     * during decoding. In particular, when buffer usage reaches {@code bufferUsageRatio / bufferUsageRatio + 1}. E.g. setting
     * {@code bufferUsageRatio} to {@literal 3}, will discard read bytes once the buffer usage reaches 75 percent.
     *
     * @return the buffer usage ratio.
     * @since 5.2
     */
    public int getBufferUsageRatio() {
        return bufferUsageRatio;
    }

    /**
     * Behavior of connections in disconnected state.
     */
    public enum DisconnectedBehavior {

        /**
         * Accept commands when auto-reconnect is enabled, reject commands when auto-reconnect is disabled.
         */
        DEFAULT,

        /**
         * Accept commands in disconnected state.
         */
        ACCEPT_COMMANDS,

        /**
         * Reject commands in disconnected state.
         */
        REJECT_COMMANDS,
    }
}
