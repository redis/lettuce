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

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import io.lettuce.core.cluster.ClusterTopologyRefreshOptions.RefreshTrigger;

/**
 * @author Mark Paluch
 */
class ClusterTopologyRefreshOptionsUnitTests {

    @Test
    void testBuilder() {

        ClusterTopologyRefreshOptions options = ClusterTopologyRefreshOptions.builder()//
                .enablePeriodicRefresh(true).refreshPeriod(10, TimeUnit.MINUTES)//
                .dynamicRefreshSources(false) //
                .enableAdaptiveRefreshTrigger(RefreshTrigger.MOVED_REDIRECT)//
                .adaptiveRefreshTriggersTimeout(15, TimeUnit.MILLISECONDS)//
                .closeStaleConnections(false)//
                .refreshTriggersReconnectAttempts(2)//
                .build();

        assertThat(options.getRefreshPeriod()).isEqualTo(Duration.ofMinutes(10));
        assertThat(options.isCloseStaleConnections()).isEqualTo(false);
        assertThat(options.isPeriodicRefreshEnabled()).isTrue();
        assertThat(options.useDynamicRefreshSources()).isFalse();
        assertThat(options.getAdaptiveRefreshTimeout()).isEqualTo(Duration.ofMillis(15));
        assertThat(options.getAdaptiveRefreshTriggers()).containsOnly(RefreshTrigger.MOVED_REDIRECT);
        assertThat(options.getRefreshTriggersReconnectAttempts()).isEqualTo(2);
    }

    @Test
    void testCopy() {

        ClusterTopologyRefreshOptions master = ClusterTopologyRefreshOptions.builder()//
                .enablePeriodicRefresh(true).refreshPeriod(10, TimeUnit.MINUTES)//
                .dynamicRefreshSources(false) //
                .enableAdaptiveRefreshTrigger(RefreshTrigger.MOVED_REDIRECT)//
                .adaptiveRefreshTriggersTimeout(15, TimeUnit.MILLISECONDS)//
                .closeStaleConnections(false)//
                .refreshTriggersReconnectAttempts(2)//
                .build();

        ClusterTopologyRefreshOptions options = ClusterTopologyRefreshOptions.copyOf(master);

        assertThat(options.getRefreshPeriod()).isEqualTo(Duration.ofMinutes(10));
        assertThat(options.isCloseStaleConnections()).isEqualTo(false);
        assertThat(options.isPeriodicRefreshEnabled()).isTrue();
        assertThat(options.useDynamicRefreshSources()).isFalse();
        assertThat(options.getAdaptiveRefreshTimeout()).isEqualTo(Duration.ofMillis(15));
        assertThat(options.getAdaptiveRefreshTriggers()).containsOnly(RefreshTrigger.MOVED_REDIRECT);
        assertThat(options.getRefreshTriggersReconnectAttempts()).isEqualTo(2);
    }

    @Test
    void testDefault() {

        ClusterTopologyRefreshOptions options = ClusterTopologyRefreshOptions.create();

        assertThat(options.getRefreshPeriod()).isEqualTo(ClusterTopologyRefreshOptions.DEFAULT_REFRESH_PERIOD_DURATION);
        assertThat(options.isCloseStaleConnections()).isEqualTo(ClusterTopologyRefreshOptions.DEFAULT_CLOSE_STALE_CONNECTIONS);
        assertThat(options.isPeriodicRefreshEnabled()).isEqualTo(ClusterTopologyRefreshOptions.DEFAULT_PERIODIC_REFRESH_ENABLED)
                .isFalse();
        assertThat(options.useDynamicRefreshSources()).isEqualTo(ClusterTopologyRefreshOptions.DEFAULT_DYNAMIC_REFRESH_SOURCES)
                .isTrue();
        assertThat(options.getAdaptiveRefreshTimeout())
                .isEqualTo(ClusterTopologyRefreshOptions.DEFAULT_ADAPTIVE_REFRESH_TIMEOUT_DURATION);
        assertThat(options.getAdaptiveRefreshTriggers())
                .isEqualTo(ClusterTopologyRefreshOptions.DEFAULT_ADAPTIVE_REFRESH_TRIGGERS);
        assertThat(options.getRefreshTriggersReconnectAttempts())
                .isEqualTo(ClusterTopologyRefreshOptions.DEFAULT_REFRESH_TRIGGERS_RECONNECT_ATTEMPTS);
    }

    @Test
    void testEnabled() {

        ClusterTopologyRefreshOptions options = ClusterTopologyRefreshOptions.enabled();

        assertThat(options.isPeriodicRefreshEnabled()).isTrue();
        assertThat(options.useDynamicRefreshSources()).isTrue();
        assertThat(options.getAdaptiveRefreshTriggers()).contains(RefreshTrigger.ASK_REDIRECT, RefreshTrigger.MOVED_REDIRECT,
                RefreshTrigger.PERSISTENT_RECONNECTS);
    }

}
