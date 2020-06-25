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
package io.lettuce.core.cluster;

import java.time.Duration;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.SslOptions;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.internal.LettuceAssert;

/**
 * Client Options to control the behavior of {@link RedisClusterClient}.
 *
 * @author Mark Paluch
 */
@SuppressWarnings("serial")
public class ClusterClientOptions extends ClientOptions {

    public static final boolean DEFAULT_REFRESH_CLUSTER_VIEW = false;

    public static final long DEFAULT_REFRESH_PERIOD = 60;

    public static final Duration DEFAULT_REFRESH_PERIOD_DURATION = Duration.ofSeconds(DEFAULT_REFRESH_PERIOD);

    public static final boolean DEFAULT_CLOSE_STALE_CONNECTIONS = true;

    public static final boolean DEFAULT_VALIDATE_CLUSTER_MEMBERSHIP = true;

    public static final int DEFAULT_MAX_REDIRECTS = 5;

    private final boolean validateClusterNodeMembership;

    private final int maxRedirects;

    private final ClusterTopologyRefreshOptions topologyRefreshOptions;

    protected ClusterClientOptions(Builder builder) {

        super(builder);

        this.validateClusterNodeMembership = builder.validateClusterNodeMembership;
        this.maxRedirects = builder.maxRedirects;

        ClusterTopologyRefreshOptions refreshOptions = builder.topologyRefreshOptions;

        if (refreshOptions == null) {
            refreshOptions = ClusterTopologyRefreshOptions.builder() //
                    .enablePeriodicRefresh(DEFAULT_REFRESH_CLUSTER_VIEW) //
                    .refreshPeriod(DEFAULT_REFRESH_PERIOD_DURATION) //
                    .closeStaleConnections(builder.closeStaleConnections) //
                    .build();
        }

        this.topologyRefreshOptions = refreshOptions;
    }

    protected ClusterClientOptions(ClusterClientOptions original) {

        super(original);

        this.validateClusterNodeMembership = original.validateClusterNodeMembership;
        this.maxRedirects = original.maxRedirects;
        this.topologyRefreshOptions = original.topologyRefreshOptions;
    }

    /**
     * Create a copy of {@literal options}.
     *
     * @param options the original.
     * @return A new instance of {@link ClusterClientOptions} containing the values of {@literal options}.
     */
    public static ClusterClientOptions copyOf(ClusterClientOptions options) {
        return new ClusterClientOptions(options);
    }

    /**
     * Returns a new {@link ClusterClientOptions.Builder} to construct {@link ClusterClientOptions}.
     *
     * @return a new {@link ClusterClientOptions.Builder} to construct {@link ClusterClientOptions}.
     */
    public static ClusterClientOptions.Builder builder() {
        return new ClusterClientOptions.Builder();
    }

    /**
     * Returns a new {@link ClusterClientOptions.Builder} initialized from {@link ClientOptions} to construct
     * {@link ClusterClientOptions}.
     *
     * @return a new {@link ClusterClientOptions.Builder} to construct {@link ClusterClientOptions}.
     * @since 5.1.6
     */
    public static ClusterClientOptions.Builder builder(ClientOptions clientOptions) {

        LettuceAssert.notNull(clientOptions, "ClientOptions must not be null");

        if (clientOptions instanceof ClusterClientOptions) {
            return ((ClusterClientOptions) clientOptions).mutate();
        }

        Builder builder = new Builder();
        builder.autoReconnect(clientOptions.isAutoReconnect()).bufferUsageRatio(clientOptions.getBufferUsageRatio())
                .cancelCommandsOnReconnectFailure(clientOptions.isCancelCommandsOnReconnectFailure())
                .disconnectedBehavior(clientOptions.getDisconnectedBehavior())
                .publishOnScheduler(clientOptions.isPublishOnScheduler())
                .pingBeforeActivateConnection(clientOptions.isPingBeforeActivateConnection())
                .requestQueueSize(clientOptions.getRequestQueueSize()).socketOptions(clientOptions.getSocketOptions())
                .sslOptions(clientOptions.getSslOptions())
                .suspendReconnectOnProtocolFailure(clientOptions.isSuspendReconnectOnProtocolFailure())
                .timeoutOptions(clientOptions.getTimeoutOptions());

        return builder;
    }

    /**
     * Create a new {@link ClusterClientOptions} using default settings.
     *
     * @return a new instance of default cluster client client options.
     */
    public static ClusterClientOptions create() {
        return builder().build();
    }

    /**
     * Builder for {@link ClusterClientOptions}.
     */
    public static class Builder extends ClientOptions.Builder {

        private boolean closeStaleConnections = DEFAULT_CLOSE_STALE_CONNECTIONS;

        private boolean validateClusterNodeMembership = DEFAULT_VALIDATE_CLUSTER_MEMBERSHIP;

        private int maxRedirects = DEFAULT_MAX_REDIRECTS;

        private ClusterTopologyRefreshOptions topologyRefreshOptions = null;

        protected Builder() {
        }

        /**
         * Validate the cluster node membership before allowing connections to a cluster node. Defaults to {@code true}. See
         * {@link ClusterClientOptions#DEFAULT_VALIDATE_CLUSTER_MEMBERSHIP}.
         *
         * @param validateClusterNodeMembership {@code true} if validation is enabled.
         * @return {@code this}
         */
        public Builder validateClusterNodeMembership(boolean validateClusterNodeMembership) {
            this.validateClusterNodeMembership = validateClusterNodeMembership;
            return this;
        }

        /**
         * Number of maximal cluster redirects ({@literal -MOVED} and {@literal -ASK}) to follow in case a key was moved from
         * one node to another node. Defaults to {@literal 5}. See {@link ClusterClientOptions#DEFAULT_MAX_REDIRECTS}.
         *
         * @param maxRedirects the limit of maximal cluster redirects
         * @return {@code this}
         */
        public Builder maxRedirects(int maxRedirects) {
            this.maxRedirects = maxRedirects;
            return this;
        }

        /**
         * Sets the {@link ClusterTopologyRefreshOptions} for detailed control of topology updates.
         *
         * @param topologyRefreshOptions the {@link ClusterTopologyRefreshOptions}
         * @return {@code this}
         */
        public Builder topologyRefreshOptions(ClusterTopologyRefreshOptions topologyRefreshOptions) {
            this.topologyRefreshOptions = topologyRefreshOptions;
            return this;
        }

        @Override
        public Builder pingBeforeActivateConnection(boolean pingBeforeActivateConnection) {
            super.pingBeforeActivateConnection(pingBeforeActivateConnection);
            return this;
        }

        @Override
        public Builder autoReconnect(boolean autoReconnect) {
            super.autoReconnect(autoReconnect);
            return this;
        }

        @Override
        public Builder suspendReconnectOnProtocolFailure(boolean suspendReconnectOnProtocolFailure) {
            super.suspendReconnectOnProtocolFailure(suspendReconnectOnProtocolFailure);
            return this;
        }

        @Override
        public Builder cancelCommandsOnReconnectFailure(boolean cancelCommandsOnReconnectFailure) {
            super.cancelCommandsOnReconnectFailure(cancelCommandsOnReconnectFailure);
            return this;
        }

        @Override
        public Builder publishOnScheduler(boolean publishOnScheduler) {
            super.publishOnScheduler(publishOnScheduler);
            return this;
        }

        @Override
        public Builder requestQueueSize(int requestQueueSize) {
            super.requestQueueSize(requestQueueSize);
            return this;
        }

        @Override
        public Builder disconnectedBehavior(DisconnectedBehavior disconnectedBehavior) {
            super.disconnectedBehavior(disconnectedBehavior);
            return this;
        }

        @Override
        public Builder socketOptions(SocketOptions socketOptions) {
            super.socketOptions(socketOptions);
            return this;
        }

        @Override
        public Builder sslOptions(SslOptions sslOptions) {
            super.sslOptions(sslOptions);
            return this;
        }

        @Override
        public Builder timeoutOptions(TimeoutOptions timeoutOptions) {
            super.timeoutOptions(timeoutOptions);
            return this;
        }

        @Override
        public Builder bufferUsageRatio(int bufferUsageRatio) {
            super.bufferUsageRatio(bufferUsageRatio);
            return this;
        }

        /**
         * Create a new instance of {@link ClusterClientOptions}
         *
         * @return new instance of {@link ClusterClientOptions}
         */
        public ClusterClientOptions build() {
            return new ClusterClientOptions(this);
        }

    }

    /**
     * Returns a builder to create new {@link ClusterClientOptions} whose settings are replicated from the current
     * {@link ClusterClientOptions}.
     *
     * @return a {@link ClusterClientOptions.Builder} to create new {@link ClusterClientOptions} whose settings are replicated
     *         from the current {@link ClusterClientOptions}.
     * @since 5.1
     */
    public ClusterClientOptions.Builder mutate() {

        Builder builder = new Builder();

        builder.autoReconnect(isAutoReconnect()).bufferUsageRatio(getBufferUsageRatio())
                .cancelCommandsOnReconnectFailure(isCancelCommandsOnReconnectFailure())
                .disconnectedBehavior(getDisconnectedBehavior()).publishOnScheduler(isPublishOnScheduler())
                .pingBeforeActivateConnection(isPingBeforeActivateConnection()).requestQueueSize(getRequestQueueSize())
                .socketOptions(getSocketOptions()).sslOptions(getSslOptions())
                .suspendReconnectOnProtocolFailure(isSuspendReconnectOnProtocolFailure()).timeoutOptions(getTimeoutOptions())
                .validateClusterNodeMembership(isValidateClusterNodeMembership()).maxRedirects(getMaxRedirects())
                .topologyRefreshOptions(getTopologyRefreshOptions());

        return builder;
    }

    /**
     * Flag, whether regular cluster topology updates are updated. The client starts updating the cluster topology in the
     * intervals of {@link #getRefreshPeriod()}. Defaults to {@code false}. Returns the value from
     * {@link ClusterTopologyRefreshOptions} if provided.
     *
     * @return {@code true} it the cluster topology view is updated periodically.
     */
    public boolean isRefreshClusterView() {
        return topologyRefreshOptions.isPeriodicRefreshEnabled();
    }

    /**
     * Period between the regular cluster topology updates. Defaults to {@literal 60}. Returns the value from
     * {@link ClusterTopologyRefreshOptions} if provided.
     *
     * @return the period between the regular cluster topology updates.
     */
    public Duration getRefreshPeriod() {
        return topologyRefreshOptions.getRefreshPeriod();
    }

    /**
     * Flag, whether to close stale connections when refreshing the cluster topology. Defaults to {@code true}. Comes only into
     * effect if {@link #isRefreshClusterView()} is {@code true}. Returns the value from {@link ClusterTopologyRefreshOptions}
     * if provided.
     *
     * @return {@code true} if stale connections are cleaned up after cluster topology updates.
     */
    public boolean isCloseStaleConnections() {
        return topologyRefreshOptions.isCloseStaleConnections();
    }

    /**
     * Validate the cluster node membership before allowing connections to a cluster node. Defaults to {@code true}.
     *
     * @return {@code true} if validation is enabled.
     */
    public boolean isValidateClusterNodeMembership() {
        return validateClusterNodeMembership;
    }

    /**
     * Number of maximal of cluster redirects ({@literal -MOVED} and {@literal -ASK}) to follow in case a key was moved from one
     * node to another node. Defaults to {@literal 5}. See {@link ClusterClientOptions#DEFAULT_MAX_REDIRECTS}.
     *
     * @return the maximal number of followed cluster redirects.
     */
    public int getMaxRedirects() {
        return maxRedirects;
    }

    /**
     * The {@link ClusterTopologyRefreshOptions} for detailed control of topology updates.
     *
     * @return the {@link ClusterTopologyRefreshOptions}.
     */
    public ClusterTopologyRefreshOptions getTopologyRefreshOptions() {
        return topologyRefreshOptions;
    }

}
