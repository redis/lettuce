package com.lambdaworks.redis;

import java.io.Serializable;

/**
 * Client Options to control the behavior of {@link RedisClient}.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class ClientOptions implements Serializable {

    private final boolean pingBeforeActivateConnection;
    private final boolean autoReconnect;
    private final boolean cancelCommandsOnReconnectFailure;
    private final boolean suspendReconnectOnProtocolFailure;
    private final int requestQueueSize;

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
     * Builder for {@link ClientOptions}.
     */
    public static class Builder {

        private boolean pingBeforeActivateConnection = false;
        private boolean autoReconnect = true;
        private boolean cancelCommandsOnReconnectFailure = false;
        private boolean suspendReconnectOnProtocolFailure = false;
        private int requestQueueSize = Integer.MAX_VALUE;

        /**
         * Sets the {@literal PING} before activate connection flag.
         * 
         * @param pingBeforeActivateConnection true/false
         * @return {@code this}
         */
        public Builder pingBeforeActivateConnection(boolean pingBeforeActivateConnection) {
            this.pingBeforeActivateConnection = pingBeforeActivateConnection;
            return this;
        }

        /**
         * Enables or disables auto reconnection on connection loss.
         * 
         * @param autoReconnect true/false
         * @return {@code this}
         */
        public Builder autoReconnect(boolean autoReconnect) {
            this.autoReconnect = autoReconnect;
            return this;
        }

        /**
         * Suspends reconnect when reconnects run into protocol failures (SSL verification, PING before connect fails).
         * 
         * @param suspendReconnectOnProtocolFailure true/false
         * @return {@code this}
         */
        public Builder suspendReconnectOnProtocolFailure(boolean suspendReconnectOnProtocolFailure) {
            this.suspendReconnectOnProtocolFailure = suspendReconnectOnProtocolFailure;
            return this;
        }

        /**
         * Allows cancelling queued commands in case a reconnect fails.
         * 
         * @param cancelCommandsOnReconnectFailure true/false
         * @return {@code this}
         */
        public Builder cancelCommandsOnReconnectFailure(boolean cancelCommandsOnReconnectFailure) {
            this.cancelCommandsOnReconnectFailure = cancelCommandsOnReconnectFailure;
            return this;
        }

        /**
         * Set the per-connection request queue size. The command invocation will lead to a {@link RedisException} if the queue
         * size is exceeded. Setting the {@code requestQueueSize} to a lower value will lead earlier to exceptions during
         * overload or while the connection is in a disconnected state. A higher value means hitting the boundary will take
         * longer to occur, but more requests will potentially be queued up and more heap space is used. Defaults to
         * {@link Integer#MAX_VALUE}.
         *
         * @param requestQueueSize the queue size.
         * @return {@code this}
         */
        public Builder requestQueueSize(int requestQueueSize) {
            this.requestQueueSize = requestQueueSize;
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

    protected ClientOptions(Builder builder) {
        pingBeforeActivateConnection = builder.pingBeforeActivateConnection;
        cancelCommandsOnReconnectFailure = builder.cancelCommandsOnReconnectFailure;
        autoReconnect = builder.autoReconnect;
        suspendReconnectOnProtocolFailure = builder.suspendReconnectOnProtocolFailure;
        requestQueueSize = builder.requestQueueSize;
    }

    protected ClientOptions(ClientOptions original) {
        this.pingBeforeActivateConnection = original.pingBeforeActivateConnection;
        this.autoReconnect = original.autoReconnect;
        this.cancelCommandsOnReconnectFailure = original.cancelCommandsOnReconnectFailure;
        this.suspendReconnectOnProtocolFailure = original.suspendReconnectOnProtocolFailure;
        this.requestQueueSize = original.requestQueueSize;
    }

    protected ClientOptions() {
        pingBeforeActivateConnection = false;
        autoReconnect = true;
        cancelCommandsOnReconnectFailure = false;
        suspendReconnectOnProtocolFailure = false;
        requestQueueSize = Integer.MAX_VALUE;
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
}
