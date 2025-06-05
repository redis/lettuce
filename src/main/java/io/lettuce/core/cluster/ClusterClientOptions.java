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
package io.lettuce.core.cluster;

import java.nio.charset.Charset;
import java.time.Duration;
import java.util.function.Predicate;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.SslOptions;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.protocol.DecodeBufferPolicy;
import io.lettuce.core.protocol.ProtocolVersion;
import io.lettuce.core.protocol.ReadOnlyCommands;

/**
 * Client Options to control the behavior of {@link RedisClusterClient}.
 *
 * @author Mark Paluch
 */
@SuppressWarnings("serial")
public class ClusterClientOptions extends ClientOptions {

    public static final boolean DEFAULT_CLOSE_STALE_CONNECTIONS = true;

    public static final ReadOnlyCommands.ReadOnlyPredicate DEFAULT_READ_ONLY_COMMANDS = ClusterReadOnlyCommands.asPredicate();

    public static final int DEFAULT_MAX_REDIRECTS = 5;

    public static final boolean DEFAULT_REFRESH_CLUSTER_VIEW = false;

    public static final long DEFAULT_REFRESH_PERIOD = 60;

    public static final Duration DEFAULT_REFRESH_PERIOD_DURATION = Duration.ofSeconds(DEFAULT_REFRESH_PERIOD);

    /** Since Lettuce 7.0 validation is by default disabled. */
    public static final boolean DEFAULT_VALIDATE_CLUSTER_MEMBERSHIP = false;

    public static final Predicate<RedisClusterNode> DEFAULT_NODE_FILTER = node -> true;

    private final int maxRedirects;

    private final ClusterTopologyRefreshOptions topologyRefreshOptions;

    private final boolean validateClusterNodeMembership;

    private final Predicate<RedisClusterNode> nodeFilter;

    protected ClusterClientOptions(Builder builder) {

        super(builder);

        ClusterTopologyRefreshOptions refreshOptions = builder.topologyRefreshOptions;

        if (refreshOptions == null) {
            refreshOptions = ClusterTopologyRefreshOptions.builder() //
                    .enablePeriodicRefresh(DEFAULT_REFRESH_CLUSTER_VIEW) //
                    .refreshPeriod(DEFAULT_REFRESH_PERIOD_DURATION) //
                    .closeStaleConnections(builder.closeStaleConnections) //
                    .build();
        }

        this.topologyRefreshOptions = refreshOptions;
        this.maxRedirects = builder.maxRedirects;
        this.validateClusterNodeMembership = builder.validateClusterNodeMembership;
        this.nodeFilter = builder.nodeFilter;
    }

    protected ClusterClientOptions(ClusterClientOptions original) {

        super(original);

        this.maxRedirects = original.maxRedirects;
        this.topologyRefreshOptions = original.topologyRefreshOptions;
        this.validateClusterNodeMembership = original.validateClusterNodeMembership;
        this.nodeFilter = original.nodeFilter;
    }

    /**
     * Create a copy of {@literal options}.
     *
     * @param options the original
     * @return A new instance of {@link ClusterClientOptions} containing the values of {@literal options}
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
        builder.autoReconnect(clientOptions.isAutoReconnect())
                .cancelCommandsOnReconnectFailure(clientOptions.isCancelCommandsOnReconnectFailure())
                .decodeBufferPolicy(clientOptions.getDecodeBufferPolicy())
                .disconnectedBehavior(clientOptions.getDisconnectedBehavior())
                .reauthenticateBehavior(clientOptions.getReauthenticateBehaviour())
                .pingBeforeActivateConnection(clientOptions.isPingBeforeActivateConnection())
                .publishOnScheduler(clientOptions.isPublishOnScheduler())
                .protocolVersion(clientOptions.getConfiguredProtocolVersion())
                .requestQueueSize(clientOptions.getRequestQueueSize()).scriptCharset(clientOptions.getScriptCharset())
                .socketOptions(clientOptions.getSocketOptions()).sslOptions(clientOptions.getSslOptions())
                .suspendReconnectOnProtocolFailure(clientOptions.isSuspendReconnectOnProtocolFailure())
                .timeoutOptions(clientOptions.getTimeoutOptions());

        return builder;
    }

    /**
     * Create a new {@link ClusterClientOptions} using default settings.
     *
     * @return a new instance of default cluster client options.
     */
    public static ClusterClientOptions create() {
        return builder().build();
    }

    /**
     * Builder for {@link ClusterClientOptions}.
     */
    public static class Builder extends ClientOptions.Builder {

        private boolean closeStaleConnections = DEFAULT_CLOSE_STALE_CONNECTIONS;

        private int maxRedirects = DEFAULT_MAX_REDIRECTS;

        private boolean validateClusterNodeMembership = DEFAULT_VALIDATE_CLUSTER_MEMBERSHIP;

        private Predicate<RedisClusterNode> nodeFilter = DEFAULT_NODE_FILTER;

        private ClusterTopologyRefreshOptions topologyRefreshOptions = null;

        protected Builder() {
            readOnlyCommands(DEFAULT_READ_ONLY_COMMANDS);
        }

        @Override
        public Builder autoReconnect(boolean autoReconnect) {
            super.autoReconnect(autoReconnect);
            return this;
        }

        /**
         * @param bufferUsageRatio the buffer usage ratio. Must be between {@code 0} and {@code 2^31-1}, typically a value
         *        between 1 and 10 representing 50% to 90%.
         * @return {@code this}
         * @deprecated since 6.0 in favor of {@link DecodeBufferPolicy}.
         */
        @Override
        @Deprecated
        public Builder bufferUsageRatio(int bufferUsageRatio) {
            super.bufferUsageRatio(bufferUsageRatio);
            return this;
        }

        /**
         *
         * @param cancelCommandsOnReconnectFailure true/false
         * @return
         * @deprecated since 6.2, to be removed with 7.0. This feature is unsafe and may cause protocol offsets if true (i.e.
         *             Redis commands are completed with previous command values).
         */
        @Override
        @Deprecated
        public Builder cancelCommandsOnReconnectFailure(boolean cancelCommandsOnReconnectFailure) {
            super.cancelCommandsOnReconnectFailure(cancelCommandsOnReconnectFailure);
            return this;
        }

        @Override
        public Builder decodeBufferPolicy(DecodeBufferPolicy decodeBufferPolicy) {
            super.decodeBufferPolicy(decodeBufferPolicy);
            return this;
        }

        @Override
        public Builder disconnectedBehavior(DisconnectedBehavior disconnectedBehavior) {
            super.disconnectedBehavior(disconnectedBehavior);
            return this;
        }

        @Override
        public Builder reauthenticateBehavior(ReauthenticateBehavior reauthenticateBehavior) {
            super.reauthenticateBehavior(reauthenticateBehavior);
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

        @Override
        public Builder pingBeforeActivateConnection(boolean pingBeforeActivateConnection) {
            super.pingBeforeActivateConnection(pingBeforeActivateConnection);
            return this;
        }

        @Override
        public Builder protocolVersion(ProtocolVersion protocolVersion) {
            super.protocolVersion(protocolVersion);
            return this;
        }

        @Override
        public Builder suspendReconnectOnProtocolFailure(boolean suspendReconnectOnProtocolFailure) {
            super.suspendReconnectOnProtocolFailure(suspendReconnectOnProtocolFailure);
            return this;
        }

        @Override
        public Builder publishOnScheduler(boolean publishOnScheduler) {
            super.publishOnScheduler(publishOnScheduler);
            return this;
        }

        @Override
        public Builder readOnlyCommands(ReadOnlyCommands.ReadOnlyPredicate readOnlyCommands) {

            super.readOnlyCommands(readOnlyCommands);
            return this;
        }

        @Override
        public Builder requestQueueSize(int requestQueueSize) {
            super.requestQueueSize(requestQueueSize);
            return this;
        }

        @Override
        public Builder scriptCharset(Charset scriptCharset) {
            super.scriptCharset(scriptCharset);
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

        /**
         * Validate the cluster node membership before allowing connections to a cluster node. Defaults to {@code false}. See
         * {@link ClusterClientOptions#DEFAULT_VALIDATE_CLUSTER_MEMBERSHIP}.
         * <p/>
         * Since 7.0, validation is disabled by default, as it is causing problems in some upgrade scenarios. In scenarios where
         * upgraded nodes are added to the cluster the ASK / MOVED replies usually come before the topology is refreshed and -
         * respectively - this validation would fail.
         *
         * @param validateClusterNodeMembership {@code true} if validation is enabled.
         * @return {@code this}
         */
        public Builder validateClusterNodeMembership(boolean validateClusterNodeMembership) {
            this.validateClusterNodeMembership = validateClusterNodeMembership;
            return this;
        }

        /**
         * Provide a {@link Predicate node filter} to filter cluster nodes from
         * {@link io.lettuce.core.cluster.models.partitions.Partitions}.
         *
         * @param nodeFilter must not be {@code null}.
         * @return {@code this}
         * @since 6.1.6
         */
        public Builder nodeFilter(Predicate<RedisClusterNode> nodeFilter) {

            LettuceAssert.notNull(nodeFilter, "NodeFilter must not be null");
            this.nodeFilter = nodeFilter;
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
     *
     * @since 5.1
     */
    public ClusterClientOptions.Builder mutate() {

        Builder builder = new Builder();

        builder.autoReconnect(isAutoReconnect()).cancelCommandsOnReconnectFailure(isCancelCommandsOnReconnectFailure())
                .decodeBufferPolicy(getDecodeBufferPolicy()).disconnectedBehavior(getDisconnectedBehavior())
                .reauthenticateBehavior(getReauthenticateBehaviour()).maxRedirects(getMaxRedirects())
                .publishOnScheduler(isPublishOnScheduler()).pingBeforeActivateConnection(isPingBeforeActivateConnection())
                .protocolVersion(getConfiguredProtocolVersion()).readOnlyCommands(getReadOnlyCommands())
                .requestQueueSize(getRequestQueueSize()).scriptCharset(getScriptCharset()).socketOptions(getSocketOptions())
                .sslOptions(getSslOptions()).suspendReconnectOnProtocolFailure(isSuspendReconnectOnProtocolFailure())
                .timeoutOptions(getTimeoutOptions()).topologyRefreshOptions(getTopologyRefreshOptions())
                .validateClusterNodeMembership(isValidateClusterNodeMembership()).nodeFilter(getNodeFilter());

        return builder;
    }

    /**
     * Flag, whether to close stale connections when refreshing the cluster topology. Defaults to {@code true}. Comes only into
     * effect if {@link #isRefreshClusterView()} is {@code true}. Returns the value from {@link ClusterTopologyRefreshOptions}
     * if provided.
     *
     * @return {@code true} if stale connections are cleaned up after cluster topology updates
     */
    public boolean isCloseStaleConnections() {
        return topologyRefreshOptions.isCloseStaleConnections();
    }

    /**
     * Number of maximal of cluster redirects ({@literal -MOVED} and {@literal -ASK}) to follow in case a key was moved from one
     * node to another node. Defaults to {@literal 5}. See {@link ClusterClientOptions#DEFAULT_MAX_REDIRECTS}.
     *
     * @return the maximal number of followed cluster redirects
     */
    public int getMaxRedirects() {
        return maxRedirects;
    }

    /**
     * Flag, whether regular cluster topology updates are updated. The client starts updating the cluster topology in the
     * intervals of {@link #getRefreshPeriod()}. Defaults to {@code false}. Returns the value from
     * {@link ClusterTopologyRefreshOptions} if provided.
     *
     * @return {@code true} it the cluster topology view is updated periodically
     */
    public boolean isRefreshClusterView() {
        return topologyRefreshOptions.isPeriodicRefreshEnabled();
    }

    /**
     * Period between the regular cluster topology updates. Defaults to {@literal 60}. Returns the value from
     * {@link ClusterTopologyRefreshOptions} if provided.
     *
     * @return the period between the regular cluster topology updates
     */
    public Duration getRefreshPeriod() {
        return topologyRefreshOptions.getRefreshPeriod();
    }

    /**
     * The {@link ClusterTopologyRefreshOptions} for detailed control of topology updates.
     *
     * @return the {@link ClusterTopologyRefreshOptions}.
     */
    public ClusterTopologyRefreshOptions getTopologyRefreshOptions() {
        return topologyRefreshOptions;
    }

    /**
     * Validate the cluster node membership before allowing connections to a cluster node. Defaults to {@code false}.
     *
     * @return {@code true} if validation is enabled.
     */
    public boolean isValidateClusterNodeMembership() {
        return validateClusterNodeMembership;
    }

    /**
     * The {@link Predicate node filter} to filter Redis Cluster nodes from
     * {@link io.lettuce.core.cluster.models.partitions.Partitions}.
     *
     * @return the {@link Predicate node filter} to filter Redis Cluster nodes from
     *         {@link io.lettuce.core.cluster.models.partitions.Partitions}.
     * @since 6.1.6
     */
    public Predicate<RedisClusterNode> getNodeFilter() {
        return nodeFilter;
    }

}
