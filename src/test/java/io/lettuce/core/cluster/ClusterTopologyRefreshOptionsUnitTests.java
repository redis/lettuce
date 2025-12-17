package io.lettuce.core.cluster;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.*;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.cluster.ClusterTopologyRefreshOptions.RefreshTrigger;

/**
 * Unit tests for {@link ClusterTopologyRefreshOptions}.
 *
 * @author Mark Paluch
 */
@Tag(UNIT_TEST)
class ClusterTopologyRefreshOptionsUnitTests {

    @Test
    void testBuilder() {

        ClusterTopologyRefreshOptions options = ClusterTopologyRefreshOptions.builder()//
                .enablePeriodicRefresh(true).refreshPeriod(10, TimeUnit.MINUTES)//
                .dynamicRefreshSources(false) //
                .disableAllAdaptiveRefreshTriggers()//
                .enableAdaptiveRefreshTrigger(RefreshTrigger.MOVED_REDIRECT)//
                .adaptiveRefreshTriggersTimeout(15, TimeUnit.MILLISECONDS)//
                .closeStaleConnections(false)//
                .refreshTriggersReconnectAttempts(2)//
                .maxTopologyRefreshSources(100)//
                .build();

        assertThat(options.getRefreshPeriod()).isEqualTo(Duration.ofMinutes(10));
        assertThat(options.isCloseStaleConnections()).isEqualTo(false);
        assertThat(options.isPeriodicRefreshEnabled()).isTrue();
        assertThat(options.useDynamicRefreshSources()).isFalse();
        assertThat(options.getAdaptiveRefreshTimeout()).isEqualTo(Duration.ofMillis(15));
        assertThat(options.getAdaptiveRefreshTriggers()).containsOnly(RefreshTrigger.MOVED_REDIRECT);
        assertThat(options.getRefreshTriggersReconnectAttempts()).isEqualTo(2);
        assertThat(options.getMaxTopologyRefreshSources()).isEqualTo(100);
    }

    @Test
    void testCopy() {

        ClusterTopologyRefreshOptions master = ClusterTopologyRefreshOptions.builder()//
                .enablePeriodicRefresh(true).refreshPeriod(10, TimeUnit.MINUTES)//
                .dynamicRefreshSources(false) //
                .disableAllAdaptiveRefreshTriggers()//
                .enableAdaptiveRefreshTrigger(RefreshTrigger.MOVED_REDIRECT)//
                .adaptiveRefreshTriggersTimeout(15, TimeUnit.MILLISECONDS)//
                .closeStaleConnections(false)//
                .refreshTriggersReconnectAttempts(2)//
                .maxTopologyRefreshSources(100)//
                .build();

        ClusterTopologyRefreshOptions options = ClusterTopologyRefreshOptions.copyOf(master);

        assertThat(options.getRefreshPeriod()).isEqualTo(Duration.ofMinutes(10));
        assertThat(options.isCloseStaleConnections()).isEqualTo(false);
        assertThat(options.isPeriodicRefreshEnabled()).isTrue();
        assertThat(options.useDynamicRefreshSources()).isFalse();
        assertThat(options.getAdaptiveRefreshTimeout()).isEqualTo(Duration.ofMillis(15));
        assertThat(options.getAdaptiveRefreshTriggers()).containsOnly(RefreshTrigger.MOVED_REDIRECT);
        assertThat(options.getRefreshTriggersReconnectAttempts()).isEqualTo(2);
        assertThat(options.getMaxTopologyRefreshSources()).isEqualTo(100);
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
        assertThat(options.getMaxTopologyRefreshSources())
                .isEqualTo(ClusterTopologyRefreshOptions.DEFAULT_MAX_TOPOLOGY_REFRESH_SOURCES);
    }

    @Test
    void testEnabled() {

        ClusterTopologyRefreshOptions options = ClusterTopologyRefreshOptions.enabled();

        assertThat(options.isPeriodicRefreshEnabled()).isTrue();
        assertThat(options.useDynamicRefreshSources()).isTrue();
        assertThat(options.getAdaptiveRefreshTriggers()).contains(RefreshTrigger.ASK_REDIRECT, RefreshTrigger.MOVED_REDIRECT,
                RefreshTrigger.PERSISTENT_RECONNECTS);
    }

    @Test
    void emptyTriggersShouldFail() {

        ClusterTopologyRefreshOptions.Builder builder = ClusterTopologyRefreshOptions.builder();

        assertThatIllegalArgumentException().isThrownBy(builder::enableAdaptiveRefreshTrigger);
    }

}
