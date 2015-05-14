package com.lambdaworks.redis;

import java.io.Serializable;

/**
 * Client Options to control the behavior of {@link RedisClient} and {@link com.lambdaworks.redis.cluster.RedisClusterClient}.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class ClientOptions implements Serializable {

    private final boolean pingBeforeActivateConnection;
    private final boolean autoReconnect;
    private final boolean cancelCommandsOnReconnectFailure;

    /**
     * Create a copy of {@literal options}
     * 
     * @param options
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
         * Enables or disables auto reconnection on connection loss
         * 
         * @param autoReconnect true/false
         * @return {@code this}
         */
        public Builder autoReconnect(boolean autoReconnect) {
            this.autoReconnect = autoReconnect;
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
         * Create a new instance of {@link ClientOptions}
         * 
         * @return new instance of {@link ClientOptions}
         */
        public ClientOptions build() {
            return new ClientOptions(this);
        }
    }

    private ClientOptions(Builder builder) {
        pingBeforeActivateConnection = builder.pingBeforeActivateConnection;
        cancelCommandsOnReconnectFailure = builder.cancelCommandsOnReconnectFailure;
        autoReconnect = builder.autoReconnect;
    }

    private ClientOptions(ClientOptions original) {
        this.pingBeforeActivateConnection = original.pingBeforeActivateConnection;
        this.autoReconnect = original.autoReconnect;
        this.cancelCommandsOnReconnectFailure = original.cancelCommandsOnReconnectFailure;
    }

    protected ClientOptions() {
        pingBeforeActivateConnection = false;
        autoReconnect = true;
        cancelCommandsOnReconnectFailure = false;
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
     * as a connection gets closed/reset without the intention to close it, the client will try to reconnect and re-issue the
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
     * If this flag is {@literal true} any queued commands will be canceled when a reconnect failes within the activation
     * sequence.
     * 
     * @return {@literal true} if commands should be cancelled on reconnect failures.
     */
    public boolean isCancelCommandsOnReconnectFailure() {
        return cancelCommandsOnReconnectFailure;
    }

}
