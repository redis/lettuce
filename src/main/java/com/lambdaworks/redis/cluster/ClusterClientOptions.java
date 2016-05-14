package com.lambdaworks.redis.cluster;

import java.util.concurrent.TimeUnit;

import com.lambdaworks.redis.ClientOptions;

/**
 * Client Options to control the behavior of {@link RedisClusterClient}.
 * 
 * @author Mark Paluch
 */
public class ClusterClientOptions extends ClientOptions {

    public static final boolean DEFAULT_REFRESH_CLUSTER_VIEW = false;
    public static final long DEFAULT_REFRESH_PERIOD = 60;
    public static final TimeUnit DEFAULT_REFRESH_PERIOD_UNIT = TimeUnit.SECONDS;
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
            refreshOptions = new ClusterTopologyRefreshOptions.Builder()//
                    .enablePeriodicRefresh(builder.refreshClusterView)//
                    .refreshPeriod(builder.refreshPeriod, builder.refreshPeriodUnit)//
                    .closeStaleConnections(builder.closeStaleConnections)//
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
     * @param options the original
     * @return A new instance of {@link ClusterClientOptions} containing the values of {@literal options}
     */
    public static ClusterClientOptions copyOf(ClusterClientOptions options) {
        return new ClusterClientOptions(options);
    }

    /**
     * Builder for {@link ClusterClientOptions}.
     */
    public static class Builder extends ClientOptions.Builder {

        private boolean refreshClusterView = DEFAULT_REFRESH_CLUSTER_VIEW;
        private long refreshPeriod = DEFAULT_REFRESH_PERIOD;
        private TimeUnit refreshPeriodUnit = DEFAULT_REFRESH_PERIOD_UNIT;
        private boolean closeStaleConnections = DEFAULT_CLOSE_STALE_CONNECTIONS;
        private boolean validateClusterNodeMembership = DEFAULT_VALIDATE_CLUSTER_MEMBERSHIP;
        private int maxRedirects = DEFAULT_MAX_REDIRECTS;
        private ClusterTopologyRefreshOptions topologyRefreshOptions = null;

        /**
         * Enable regular cluster topology updates. The client starts updating the cluster topology in the intervals of
         * {@link Builder#refreshPeriod} /{@link Builder#refreshPeriodUnit}. Defaults to {@literal false}. See
         * {@link #DEFAULT_REFRESH_CLUSTER_VIEW}.
         *
         * @param refreshClusterView {@literal true} enable regular cluster topology updates or {@literal false} to disable
         *        auto-updating
         * @return {@code this}
         * @deprecated Use {@link #topologyRefreshOptions}, see
         *             {@link com.lambdaworks.redis.cluster.ClusterTopologyRefreshOptions.Builder#enablePeriodicRefresh(boolean)}
         */
        @Deprecated
        public Builder refreshClusterView(boolean refreshClusterView) {
            this.refreshClusterView = refreshClusterView;
            return this;
        }

        /**
         * Set the refresh period. Defaults to {@literal 60 SECONDS}. See {@link #DEFAULT_REFRESH_PERIOD} and
         * {@link #DEFAULT_REFRESH_PERIOD_UNIT}.
         *
         * @param refreshPeriod period for triggering topology updates
         * @param refreshPeriodUnit unit for {@code refreshPeriod}
         * @return {@code this}
         * @deprecated Use {@link #topologyRefreshOptions}, see
         *             {@link com.lambdaworks.redis.cluster.ClusterTopologyRefreshOptions.Builder#refreshPeriod(long, TimeUnit)}
         */
        @Deprecated
        public Builder refreshPeriod(long refreshPeriod, TimeUnit refreshPeriodUnit) {
            this.refreshPeriod = refreshPeriod;
            this.refreshPeriodUnit = refreshPeriodUnit;
            return this;
        }

        /**
         * Flag, whether to close stale connections when refreshing the cluster topology. Defaults to {@literal true}. Comes
         * only into effect if {@link #isRefreshClusterView()} is {@literal true}. See
         * {@link ClusterClientOptions#DEFAULT_CLOSE_STALE_CONNECTIONS}.
         *
         * @param closeStaleConnections {@literal true} if stale connections are cleaned up after cluster topology updates
         * @return {@code this}
         * @deprecated Use {@link #topologyRefreshOptions}, see
         *             {@link com.lambdaworks.redis.cluster.ClusterTopologyRefreshOptions.Builder#closeStaleConnections(boolean)}
         */
        @Deprecated
        public Builder closeStaleConnections(boolean closeStaleConnections) {
            this.closeStaleConnections = closeStaleConnections;
            return this;
        }

        /**
         * Validate the cluster node membership before allowing connections to a cluster node. Defaults to {@literal true}. See
         * {@link ClusterClientOptions#DEFAULT_VALIDATE_CLUSTER_MEMBERSHIP}.
         *
         * @param validateClusterNodeMembership {@literal true} if validation is enabled.
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
        public Builder requestQueueSize(int requestQueueSize) {
            super.requestQueueSize(requestQueueSize);
            return this;
        }

        @Override
        public Builder disconnectedBehavior(DisconnectedBehavior disconnectedBehavior) {
            super.disconnectedBehavior(disconnectedBehavior);
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
     * Flag, whether regular cluster topology updates are updated. The client starts updating the cluster topology in the
     * intervals of {@link #getRefreshPeriod()} /{@link #getRefreshPeriodUnit()}. Defaults to {@literal false}. Returns the
     * value from {@link ClusterTopologyRefreshOptions} if provided.
     * 
     * @return {@literal true} it the cluster topology view is updated periodically
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
    public long getRefreshPeriod() {
        return topologyRefreshOptions.getRefreshPeriod();
    }

    /**
     * Unit for the {@link #getRefreshPeriod()}. Defaults to {@link TimeUnit#SECONDS}. Returns the value from
     * {@link ClusterTopologyRefreshOptions} if provided.
     * 
     * @return unit for the {@link #getRefreshPeriod()}
     */
    public TimeUnit getRefreshPeriodUnit() {
        return topologyRefreshOptions.getRefreshPeriodUnit();
    }

    /**
     * Flag, whether to close stale connections when refreshing the cluster topology. Defaults to {@literal true}. Comes only
     * into effect if {@link #isRefreshClusterView()} is {@literal true}. Returns the value from
     * {@link ClusterTopologyRefreshOptions} if provided.
     * 
     * @return {@literal true} if stale connections are cleaned up after cluster topology updates
     */
    public boolean isCloseStaleConnections() {
        return topologyRefreshOptions.isCloseStaleConnections();
    }

    /**
     * Validate the cluster node membership before allowing connections to a cluster node. Defaults to {@literal true}.
     * 
     * @return {@literal true} if validation is enabled.
     */
    public boolean isValidateClusterNodeMembership() {
        return validateClusterNodeMembership;
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
     * The {@link ClusterTopologyRefreshOptions} for detailed control of topology updates.
     * 
     * @return the {@link ClusterTopologyRefreshOptions}.
     */
    public ClusterTopologyRefreshOptions getTopologyRefreshOptions() {
        return topologyRefreshOptions;
    }

    /**
     * Create a new {@link ClusterClientOptions} using default settings.
     *
     * @return a new instance of default cluster client client options.
     */
    public static ClusterClientOptions create() {
        return new Builder().build();
    }
}
