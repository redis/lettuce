package com.lambdaworks.redis.cluster;

import java.util.concurrent.TimeUnit;

import com.lambdaworks.redis.ClientOptions;

/**
 * Client Options to control the behavior of {@link RedisClusterClient}.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class ClusterClientOptions extends ClientOptions {
    private final boolean refreshClusterView;
    private final long refreshPeriod;
    private final TimeUnit refreshPeriodUnit;
    private final boolean closeStaleConnections;
    private final boolean validateClusterNodeMembership;

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

        private boolean refreshClusterView = false;
        private long refreshPeriod = 60;
        private TimeUnit refreshPeriodUnit = TimeUnit.SECONDS;
        private boolean closeStaleConnections = true;
        private boolean validateClusterNodeMembership = true;

        /**
         * Enable regular cluster topology updates. The client starts updating the cluster topology in the intervals of
         * {@link Builder#refreshPeriod} /{@link Builder#refreshPeriodUnit}. Defaults to {@literal false}.
         *
         * @param refreshClusterView {@literal true} enable regular cluster topology updates or {@literal false} to disable
         *        auto-updating
         * @return {@code this}
         */
        public Builder refreshClusterView(boolean refreshClusterView) {
            this.refreshClusterView = refreshClusterView;
            return this;
        }

        /**
         * Set the refresh period. Defaults to {@literal 60 SECONDS}.
         *
         * @param refreshPeriod period for triggering topology updates
         * @param refreshPeriodUnit unit for {@code refreshPeriod}
         * @return {@code this}
         */
        public Builder refreshPeriod(long refreshPeriod, TimeUnit refreshPeriodUnit) {
            this.refreshPeriod = refreshPeriod;
            this.refreshPeriodUnit = refreshPeriodUnit;
            return this;
        }

        /**
         * Flag, whether to close stale connections when refreshing the cluster topology. Defaults to {@literal true}. Comes
         * only into effect if {@link #isRefreshClusterView()} is {@literal true}.
         *
         * @param closeStaleConnections {@literal true} if stale connections are cleaned up after cluster topology updates
         * @return {@code this}
         */
        public Builder closeStaleConnections(boolean closeStaleConnections) {
            this.closeStaleConnections = closeStaleConnections;
            return this;
        }

        /**
         * Validate the cluster node membership before allowing connections to a cluster node. Defaults to {@literal true}.
         *
         * @param validateClusterNodeMembership {@literal true} if validation is enabled.
         * @return {@code this}
         */
        public Builder validateClusterNodeMembership(boolean validateClusterNodeMembership) {
            this.validateClusterNodeMembership = validateClusterNodeMembership;
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

    protected ClusterClientOptions(Builder builder) {
        super(builder);
        this.refreshClusterView = builder.refreshClusterView;
        this.refreshPeriod = builder.refreshPeriod;
        this.refreshPeriodUnit = builder.refreshPeriodUnit;
        this.closeStaleConnections = builder.closeStaleConnections;
        this.validateClusterNodeMembership = builder.validateClusterNodeMembership;
    }

    protected ClusterClientOptions(ClusterClientOptions original) {
        super(original);
        this.refreshClusterView = original.refreshClusterView;
        this.refreshPeriod = original.refreshPeriod;
        this.refreshPeriodUnit = original.refreshPeriodUnit;
        this.closeStaleConnections = original.closeStaleConnections;
        this.validateClusterNodeMembership = original.validateClusterNodeMembership;
    }

    protected ClusterClientOptions() {
        this.refreshClusterView = false;
        this.refreshPeriod = 60;
        this.refreshPeriodUnit = TimeUnit.SECONDS;
        this.closeStaleConnections = true;
        this.validateClusterNodeMembership = true;
    }

    /**
     * Flag, whether regular cluster topology updates are updated. The client starts updating the cluster topology in the
     * intervals of {@link #getRefreshPeriod()} /{@link #getRefreshPeriodUnit()}. Defaults to {@literal false}.
     * 
     * @return {@literal true} it the cluster topology view is updated periodically
     */
    public boolean isRefreshClusterView() {
        return refreshClusterView;
    }

    /**
     * Period between the regular cluster topology updates. Defaults to {@literal 60}.
     * 
     * @return the period between the regular cluster topology updates
     */
    public long getRefreshPeriod() {
        return refreshPeriod;
    }

    /**
     * Unit for the {@link #getRefreshPeriod()}. Defaults to {@link TimeUnit#SECONDS}.
     * 
     * @return unit for the {@link #getRefreshPeriod()}
     */
    public TimeUnit getRefreshPeriodUnit() {
        return refreshPeriodUnit;
    }

    /**
     * Flag, whether to close stale connections when refreshing the cluster topology. Defaults to {@literal true}. Comes only
     * into effect if {@link #isRefreshClusterView()} is {@literal true}.
     * 
     * @return {@literal true} if stale connections are cleaned up after cluster topology updates
     */
    public boolean isCloseStaleConnections() {
        return closeStaleConnections;
    }

    /**
     * Validate the cluster node membership before allowing connections to a cluster node. Defaults to {@literal true}.
     * 
     * @return {@literal true} if validation is enabled.
     */
    public boolean isValidateClusterNodeMembership() {
        return validateClusterNodeMembership;
    }
}
