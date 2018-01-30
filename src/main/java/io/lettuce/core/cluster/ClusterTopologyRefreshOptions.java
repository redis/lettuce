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
package io.lettuce.core.cluster;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

import io.lettuce.core.internal.LettuceAssert;

/**
 * Options to control the Cluster topology refreshing of {@link RedisClusterClient}.
 *
 * @author Mark Paluch
 * @since 4.2
 */
public class ClusterTopologyRefreshOptions {

    public static final boolean DEFAULT_PERIODIC_REFRESH_ENABLED = false;
    public static final long DEFAULT_REFRESH_PERIOD = 60;
    public static final TimeUnit DEFAULT_REFRESH_PERIOD_UNIT = TimeUnit.SECONDS;
    public static final Duration DEFAULT_REFRESH_PERIOD_DURATION = Duration.ofSeconds(DEFAULT_REFRESH_PERIOD);
    public static final boolean DEFAULT_DYNAMIC_REFRESH_SOURCES = true;
    public static final Set<RefreshTrigger> DEFAULT_ADAPTIVE_REFRESH_TRIGGERS = Collections.emptySet();
    public static final long DEFAULT_ADAPTIVE_REFRESH_TIMEOUT = 30;
    public static final TimeUnit DEFAULT_ADAPTIVE_REFRESH_TIMEOUT_UNIT = TimeUnit.SECONDS;
    public static final Duration DEFAULT_ADAPTIVE_REFRESH_TIMEOUT_DURATION = Duration
            .ofSeconds(DEFAULT_ADAPTIVE_REFRESH_TIMEOUT);
    public static final int DEFAULT_REFRESH_TRIGGERS_RECONNECT_ATTEMPTS = 5;
    public static final boolean DEFAULT_CLOSE_STALE_CONNECTIONS = true;

    private final boolean periodicRefreshEnabled;
    private final Duration refreshPeriod;
    private final boolean closeStaleConnections;
    private final boolean dynamicRefreshSources;
    private final Set<RefreshTrigger> adaptiveRefreshTriggers;
    private final Duration adaptiveRefreshTimeout;
    private final int refreshTriggersReconnectAttempts;

    protected ClusterTopologyRefreshOptions(Builder builder) {

        this.periodicRefreshEnabled = builder.periodicRefreshEnabled;
        this.refreshPeriod = builder.refreshPeriod;
        this.closeStaleConnections = builder.closeStaleConnections;
        this.dynamicRefreshSources = builder.dynamicRefreshSources;
        this.adaptiveRefreshTriggers = Collections.unmodifiableSet(new HashSet<>(builder.adaptiveRefreshTriggers));
        this.adaptiveRefreshTimeout = builder.adaptiveRefreshTimeout;
        this.refreshTriggersReconnectAttempts = builder.refreshTriggersReconnectAttempts;
    }

    protected ClusterTopologyRefreshOptions(ClusterTopologyRefreshOptions original) {

        this.periodicRefreshEnabled = original.periodicRefreshEnabled;
        this.refreshPeriod = original.refreshPeriod;
        this.closeStaleConnections = original.closeStaleConnections;
        this.dynamicRefreshSources = original.dynamicRefreshSources;
        this.adaptiveRefreshTriggers = Collections.unmodifiableSet(new HashSet<>(original.adaptiveRefreshTriggers));
        this.adaptiveRefreshTimeout = original.adaptiveRefreshTimeout;
        this.refreshTriggersReconnectAttempts = original.refreshTriggersReconnectAttempts;
    }

    /**
     * Create a copy of {@literal options}.
     *
     * @param options the original
     * @return A new instance of {@link ClusterTopologyRefreshOptions} containing the values of {@literal options}
     */
    public static ClusterTopologyRefreshOptions copyOf(ClusterTopologyRefreshOptions options) {
        return new ClusterTopologyRefreshOptions(options);
    }

    /**
     * Returns a new {@link ClusterTopologyRefreshOptions.Builder} to construct {@link ClusterTopologyRefreshOptions}.
     *
     * @return a new {@link ClusterTopologyRefreshOptions.Builder} to construct {@link ClusterTopologyRefreshOptions}.
     */
    public static ClusterTopologyRefreshOptions.Builder builder() {
        return new ClusterTopologyRefreshOptions.Builder();
    }

    /**
     * Create a new {@link ClusterTopologyRefreshOptions} using default settings.
     *
     * @return a new instance of default cluster client client options.
     */
    public static ClusterTopologyRefreshOptions create() {
        return builder().build();
    }

    /**
     * Create a new {@link ClusterTopologyRefreshOptions} using default settings with enabled periodic and adaptive refresh.
     *
     * @return a new instance of default cluster client client options.
     */
    public static ClusterTopologyRefreshOptions enabled() {
        return builder().enablePeriodicRefresh().enableAllAdaptiveRefreshTriggers().build();
    }

    /**
     * Builder for {@link ClusterTopologyRefreshOptions}.
     */
    public static class Builder {

        private boolean periodicRefreshEnabled = DEFAULT_PERIODIC_REFRESH_ENABLED;
        private Duration refreshPeriod = DEFAULT_REFRESH_PERIOD_DURATION;
        private boolean closeStaleConnections = DEFAULT_CLOSE_STALE_CONNECTIONS;
        private boolean dynamicRefreshSources = DEFAULT_DYNAMIC_REFRESH_SOURCES;
        private Set<RefreshTrigger> adaptiveRefreshTriggers = new HashSet<>(DEFAULT_ADAPTIVE_REFRESH_TRIGGERS);
        private Duration adaptiveRefreshTimeout = DEFAULT_ADAPTIVE_REFRESH_TIMEOUT_DURATION;
        private int refreshTriggersReconnectAttempts = DEFAULT_REFRESH_TRIGGERS_RECONNECT_ATTEMPTS;

        private Builder() {
        }

        /**
         * Enables periodic cluster topology updates. The client starts updating the cluster topology in the intervals of
         * {@link Builder#refreshPeriod}. Defaults to {@literal false}. See {@link #DEFAULT_PERIODIC_REFRESH_ENABLED}.
         *
         * @return {@code this}
         */
        public Builder enablePeriodicRefresh() {
            return enablePeriodicRefresh(true);
        }

        /**
         * Enable regular cluster topology updates. The client starts updating the cluster topology in the intervals of
         * {@link Builder#refreshPeriod}. Defaults to {@literal false}. See {@link #DEFAULT_PERIODIC_REFRESH_ENABLED}.
         *
         * @param enabled {@literal true} enable regular cluster topology updates or {@literal false} to disable auto-updating
         * @return {@code this}
         */
        public Builder enablePeriodicRefresh(boolean enabled) {
            this.periodicRefreshEnabled = enabled;
            return this;
        }

        /**
         * Enables periodic refresh and sets the refresh period. Defaults to {@literal 60 SECONDS}. See
         * {@link #DEFAULT_REFRESH_PERIOD} and {@link #DEFAULT_REFRESH_PERIOD_UNIT}. This method is a shortcut for
         * {@link #refreshPeriod(long, TimeUnit)} and {@link #enablePeriodicRefresh()}.
         *
         * @param refreshPeriod period for triggering topology updates, must be greater {@literal 0}
         * @return {@code this}
         * @since 5.0
         */
        public Builder enablePeriodicRefresh(Duration refreshPeriod) {
            return refreshPeriod(refreshPeriod).enablePeriodicRefresh();
        }

        /**
         * Enables periodic refresh and sets the refresh period. Defaults to {@literal 60 SECONDS}. See
         * {@link #DEFAULT_REFRESH_PERIOD} and {@link #DEFAULT_REFRESH_PERIOD_UNIT}. This method is a shortcut for
         * {@link #refreshPeriod(long, TimeUnit)} and {@link #enablePeriodicRefresh()}.
         *
         * @param refreshPeriod period for triggering topology updates, must be greater {@literal 0}
         * @param refreshPeriodUnit unit for {@code refreshPeriod}, must not be {@literal null}
         * @return {@code this}
         * @deprecated since 5.0, use {@link #enablePeriodicRefresh(Duration)}.
         */
        @Deprecated
        public Builder enablePeriodicRefresh(long refreshPeriod, TimeUnit refreshPeriodUnit) {
            return refreshPeriod(refreshPeriod, refreshPeriodUnit).enablePeriodicRefresh();
        }

        /**
         * Set the refresh period. Defaults to {@literal 60 SECONDS}. See {@link #DEFAULT_REFRESH_PERIOD} and
         * {@link #DEFAULT_REFRESH_PERIOD_UNIT}.
         *
         * @param refreshPeriod period for triggering topology updates, must be greater {@literal 0}
         * @return {@code this}
         * @since 5.0
         */
        public Builder refreshPeriod(Duration refreshPeriod) {

            LettuceAssert.notNull(refreshPeriod, "RefreshPeriod duration must not be null");
            LettuceAssert.isTrue(refreshPeriod.toNanos() > 0, "RefreshPeriod must be greater 0");

            this.refreshPeriod = refreshPeriod;
            return this;
        }

        /**
         * Set the refresh period. Defaults to {@literal 60 SECONDS}. See {@link #DEFAULT_REFRESH_PERIOD} and
         * {@link #DEFAULT_REFRESH_PERIOD_UNIT}.
         *
         * @param refreshPeriod period for triggering topology updates, must be greater {@literal 0}
         * @param refreshPeriodUnit unit for {@code refreshPeriod}, must not be {@literal null}
         * @return {@code this}
         * @deprecated since 5.0, use {@link #refreshPeriod(Duration)}.
         */
        @Deprecated
        public Builder refreshPeriod(long refreshPeriod, TimeUnit refreshPeriodUnit) {

            LettuceAssert.isTrue(refreshPeriod > 0, "RefreshPeriod must be greater 0");
            LettuceAssert.notNull(refreshPeriodUnit, "TimeUnit must not be null");

            return refreshPeriod(Duration.ofNanos(refreshPeriodUnit.toNanos(refreshPeriod)));
        }

        /**
         * Flag, whether to close stale connections when refreshing the cluster topology. Defaults to {@literal true}. Comes
         * only into effect if {@link #isPeriodicRefreshEnabled()} is {@literal true}. See
         * {@link ClusterTopologyRefreshOptions#DEFAULT_CLOSE_STALE_CONNECTIONS}.
         *
         * @param closeStaleConnections {@literal true} if stale connections are cleaned up after cluster topology updates
         * @return {@code this}
         */
        public Builder closeStaleConnections(boolean closeStaleConnections) {
            this.closeStaleConnections = closeStaleConnections;
            return this;
        }

        /**
         * Discover cluster nodes from topology and use the discovered nodes as source for the cluster topology. Using dynamic
         * refresh will query all discovered nodes for the cluster topology and calculate the number of clients for each node.If
         * set to {@literal false}, only the initial seed nodes will be used as sources for topology discovery and the number of
         * clients will be obtained only for the initial seed nodes. This can be useful when using Redis Cluster with many
         * nodes. Defaults to {@literal true}. See {@link ClusterTopologyRefreshOptions#DEFAULT_DYNAMIC_REFRESH_SOURCES}.
         *
         * @param dynamicRefreshSources {@literal true} to discover and query all cluster nodes for obtaining the cluster
         *        topology
         * @return {@code this}
         */
        public Builder dynamicRefreshSources(boolean dynamicRefreshSources) {
            this.dynamicRefreshSources = dynamicRefreshSources;
            return this;
        }

        /**
         * Enables adaptive topology refreshing using one or more {@link RefreshTrigger triggers}. Adaptive refresh triggers
         * initiate topology view updates based on events happened during Redis Cluster operations. Adaptive triggers lead to an
         * immediate topology refresh. Adaptive triggered refreshes are rate-limited using a timeout since events can happen on
         * a large scale. Adaptive refresh triggers are disabled by default. See also
         * {@link #adaptiveRefreshTriggersTimeout(long, TimeUnit)} and {@link RefreshTrigger}.
         *
         * @param refreshTrigger one or more {@link RefreshTrigger} to enabled
         * @return {@code this}
         */
        public Builder enableAdaptiveRefreshTrigger(RefreshTrigger... refreshTrigger) {

            LettuceAssert.notNull(refreshTrigger, "RefreshTriggers must not be null");
            LettuceAssert.noNullElements(refreshTrigger, "RefreshTriggers must not contain null elements");

            adaptiveRefreshTriggers.addAll(Arrays.asList(refreshTrigger));
            return this;
        }

        /**
         * Enables adaptive topology refreshing using all {@link RefreshTrigger triggers}. Adaptive refresh triggers initiate
         * topology view updates based on events happened during Redis Cluster operations. Adaptive triggers lead to an
         * immediate topology refresh. Adaptive triggered refreshes are rate-limited using a timeout since events can happen on
         * a large scale. Adaptive refresh triggers are disabled by default. See also
         * {@link #adaptiveRefreshTriggersTimeout(long, TimeUnit)} and {@link RefreshTrigger}.
         *
         * @return {@code this}
         */
        public Builder enableAllAdaptiveRefreshTriggers() {
            adaptiveRefreshTriggers.addAll(EnumSet.allOf(RefreshTrigger.class));
            return this;
        }

        /**
         * Set the timeout for adaptive topology updates. This timeout is to rate-limit topology updates initiated by refresh
         * triggers to one topology refresh per timeout. Defaults to {@literal 30 SECONDS}. See {@link #DEFAULT_REFRESH_PERIOD}
         * and {@link #DEFAULT_REFRESH_PERIOD_UNIT}.
         *
         * @param timeout timeout for rate-limit adaptive topology updates, must be greater than {@literal 0}.
         * @return {@code this}
         * @since 5.0
         */
        public Builder adaptiveRefreshTriggersTimeout(Duration timeout) {

            LettuceAssert.notNull(refreshPeriod, "Adaptive refresh triggers timeout must not be null");
            LettuceAssert.isTrue(refreshPeriod.toNanos() > 0, "Adaptive refresh triggers timeout must be greater 0");

            this.adaptiveRefreshTimeout = timeout;
            return this;
        }

        /**
         * Set the timeout for adaptive topology updates. This timeout is to rate-limit topology updates initiated by refresh
         * triggers to one topology refresh per timeout. Defaults to {@literal 30 SECONDS}. See {@link #DEFAULT_REFRESH_PERIOD}
         * and {@link #DEFAULT_REFRESH_PERIOD_UNIT}.
         *
         * @param timeout timeout for rate-limit adaptive topology updates
         * @param unit unit for {@code timeout}
         * @return {@code this}
         * @deprecated since 5.0, use {@link #adaptiveRefreshTriggersTimeout(Duration)}.
         */
        @Deprecated
        public Builder adaptiveRefreshTriggersTimeout(long timeout, TimeUnit unit) {

            LettuceAssert.isTrue(timeout > 0, "Triggers timeout must be greater 0");
            LettuceAssert.notNull(unit, "TimeUnit must not be null");

            return adaptiveRefreshTriggersTimeout(Duration.ofNanos(unit.toNanos(timeout)));
        }

        /**
         * Set the threshold for the {@link RefreshTrigger#PERSISTENT_RECONNECTS}. Topology updates based on persistent
         * reconnects lead only to a refresh if the reconnect process tries at least {@code refreshTriggersReconnectAttempts}.
         * See {@link #DEFAULT_REFRESH_TRIGGERS_RECONNECT_ATTEMPTS}.
         *
         * @param refreshTriggersReconnectAttempts number of reconnect attempts for a connection before a n adaptive topology
         *        refresh is triggered
         * @return {@code this}
         */
        public Builder refreshTriggersReconnectAttempts(int refreshTriggersReconnectAttempts) {
            this.refreshTriggersReconnectAttempts = refreshTriggersReconnectAttempts;
            return this;
        }

        /**
         * Create a new instance of {@link ClusterTopologyRefreshOptions}
         *
         * @return new instance of {@link ClusterTopologyRefreshOptions}
         */
        public ClusterTopologyRefreshOptions build() {
            return new ClusterTopologyRefreshOptions(this);
        }
    }

    /**
     * Flag, whether regular cluster topology updates are updated. The client starts updating the cluster topology in the
     * intervals of {@link #getRefreshPeriod()}. Defaults to {@literal false}.
     *
     * @return {@literal true} it the cluster topology view is updated periodically
     */
    public boolean isPeriodicRefreshEnabled() {
        return periodicRefreshEnabled;
    }

    /**
     * Period between the regular cluster topology updates. Defaults to {@literal 60}.
     *
     * @return the period between the regular cluster topology updates
     */
    public Duration getRefreshPeriod() {
        return refreshPeriod;
    }

    /**
     * Flag, whether to close stale connections when refreshing the cluster topology. Defaults to {@literal true}. Comes only
     * into effect if {@link #isPeriodicRefreshEnabled()} is {@literal true}.
     *
     * @return {@literal true} if stale connections are cleaned up after cluster topology updates
     */
    public boolean isCloseStaleConnections() {
        return closeStaleConnections;
    }

    /**
     * Discover cluster nodes from topology and use the discovered nodes as source for the cluster topology. Using dynamic
     * refresh will query all discovered nodes for the cluster topology and calculate the number of clients for each node.If set
     * to {@literal false}, only the initial seed nodes will be used as sources for topology discovery and the number of clients
     * will be obtained only for the initial seed nodes. This can be useful when using Redis Cluster with many nodes.
     *
     * @return {@link true} if dynamic refresh sources are enabled
     */
    public boolean useDynamicRefreshSources() {
        return dynamicRefreshSources;
    }

    /**
     * Returns the set of {@link RefreshTrigger triggers}. Adaptive refresh triggers initiate topology view updates based on
     * events happened during Redis Cluster operations. Adaptive triggers lead to an immediate topology refresh. Adaptive
     * triggered refreshes are rate-limited using a timeout since events can happen on a large scale. Adaptive refresh triggers
     * are disabled by default.
     *
     * @return the set of {@link RefreshTrigger triggers}
     */
    public Set<RefreshTrigger> getAdaptiveRefreshTriggers() {
        return adaptiveRefreshTriggers;
    }

    /**
     * Timeout between adaptive cluster topology updates. Defaults to {@literal 30}.
     *
     * @return the period between the regular cluster topology updates
     */
    public Duration getAdaptiveRefreshTimeout() {
        return adaptiveRefreshTimeout;
    }

    /**
     * Threshold for {@link RefreshTrigger#PERSISTENT_RECONNECTS}. Topology updates based on persistent reconnects lead only to
     * a refresh if the reconnect process tries at least {@code refreshTriggersReconnectAttempts}. See
     * {@link #DEFAULT_REFRESH_TRIGGERS_RECONNECT_ATTEMPTS}.
     *
     * @return umber of reconnect attempts for a connection before a n adaptive topology refresh is triggered
     */
    public int getRefreshTriggersReconnectAttempts() {
        return refreshTriggersReconnectAttempts;
    }

    /**
     * Available refresh triggers to signal early topology refreshing.
     */
    public enum RefreshTrigger {

        /**
         * Redis responds with a {@code MOVED} redirection to a command.
         */
        MOVED_REDIRECT,

        /**
         * Redis responds with a {@code ASK} redirection to a command.
         */
        ASK_REDIRECT,

        /**
         * Connections to a particular host run into persistent reconnects (more than one attempt).
         */
        PERSISTENT_RECONNECTS,
    }
}
