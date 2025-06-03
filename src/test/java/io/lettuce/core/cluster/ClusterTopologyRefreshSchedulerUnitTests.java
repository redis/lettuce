package io.lettuce.core.cluster;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.event.EventBus;
import io.lettuce.core.event.cluster.AdaptiveRefreshTriggeredEvent;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.test.Delay;
import io.netty.util.concurrent.EventExecutorGroup;

/**
 * Unit test for {@link ClusterTopologyRefreshScheduler}.
 *
 * @author Mark Paluch
 */
@Tag(UNIT_TEST)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClusterTopologyRefreshSchedulerUnitTests {

    private ClusterTopologyRefreshScheduler sut;

    private ClusterTopologyRefreshOptions immediateRefresh = ClusterTopologyRefreshOptions.builder()
            .enablePeriodicRefresh(1, TimeUnit.MILLISECONDS).enableAllAdaptiveRefreshTriggers().build();

    private ClusterClientOptions clusterClientOptions = ClusterClientOptions.builder().topologyRefreshOptions(immediateRefresh)
            .build();

    @Mock
    private ClientResources clientResources;

    @Mock
    private RedisClusterClient clusterClient;

    @Mock
    private EventExecutorGroup eventExecutors;

    @Mock
    private EventBus eventBus;

    @BeforeEach
    void before() {

        when(clientResources.eventBus()).thenReturn(eventBus);
        when(clientResources.eventExecutorGroup()).thenReturn(eventExecutors);

        sut = new ClusterTopologyRefreshScheduler(clusterClient::getClusterClientOptions, clusterClient::getPartitions,
                clusterClient::refreshPartitionsAsync, clientResources);
    }

    @Test
    void runShouldSubmitRefreshShouldTrigger() {

        when(clusterClient.getClusterClientOptions()).thenReturn(clusterClientOptions);

        sut.run();
        verify(eventExecutors).submit(any(Runnable.class));
    }

    @Test
    void runnableShouldCallPartitionRefresh() {

        when(clusterClient.getClusterClientOptions()).thenReturn(clusterClientOptions);

        when(eventExecutors.submit(any(Runnable.class))).then(invocation -> {
            ((Runnable) invocation.getArguments()[0]).run();
            return null;
        });

        sut.run();

        verify(clusterClient).refreshPartitionsAsync();
    }

    @Test
    void shouldNotSubmitIfExecutorIsShuttingDown() {

        when(eventExecutors.isShuttingDown()).thenReturn(true);

        sut.run();
        verify(eventExecutors, never()).submit(any(Runnable.class));
    }

    @Test
    void shouldNotSubmitIfExecutorIsShutdown() {

        when(eventExecutors.isShutdown()).thenReturn(true);

        sut.run();
        verify(eventExecutors, never()).submit(any(Runnable.class));
    }

    @Test
    void shouldNotSubmitIfExecutorIsTerminated() {

        when(eventExecutors.isTerminated()).thenReturn(true);

        sut.run();
        verify(eventExecutors, never()).submit(any(Runnable.class));
    }

    @Test
    void shouldTriggerRefreshOnAskRedirection() {

        ClusterTopologyRefreshOptions clusterTopologyRefreshOptions = ClusterTopologyRefreshOptions.builder()
                .enableAllAdaptiveRefreshTriggers().build();

        ClusterClientOptions clusterClientOptions = ClusterClientOptions.builder()
                .topologyRefreshOptions(clusterTopologyRefreshOptions).build();

        when(clusterClient.getClusterClientOptions()).thenReturn(clusterClientOptions);

        sut.onAskRedirection();
        verify(eventExecutors).submit(any(Runnable.class));
    }

    @Test
    void shouldTriggerAdaptiveRefreshUsingDefaults() {

        ClusterTopologyRefreshOptions clusterTopologyRefreshOptions = ClusterTopologyRefreshOptions.create();

        ClusterClientOptions clusterClientOptions = ClusterClientOptions.builder()
                .topologyRefreshOptions(clusterTopologyRefreshOptions).build();

        when(clusterClient.getClusterClientOptions()).thenReturn(clusterClientOptions);

        sut.onAskRedirection();
        verify(eventExecutors).submit(any(Runnable.class));
    }

    @Test
    void shouldNotTriggerAdaptiveRefreshWhenDisabled() {

        ClusterTopologyRefreshOptions clusterTopologyRefreshOptions = ClusterTopologyRefreshOptions.builder()
                .disableAllAdaptiveRefreshTriggers().build();

        ClusterClientOptions clusterClientOptions = ClusterClientOptions.builder()
                .topologyRefreshOptions(clusterTopologyRefreshOptions).build();

        when(clusterClient.getClusterClientOptions()).thenReturn(clusterClientOptions);

        sut.onAskRedirection();
        verify(eventExecutors, never()).submit(any(Runnable.class));
    }

    @Test
    void shouldTriggerRefreshOnMovedRedirection() {

        ClusterClientOptions clusterClientOptions = ClusterClientOptions.builder().topologyRefreshOptions(immediateRefresh)
                .build();

        when(clusterClient.getClusterClientOptions()).thenReturn(clusterClientOptions);

        sut.onMovedRedirection();
        verify(eventExecutors).submit(any(Runnable.class));
    }

    @Test
    void shouldTriggerRefreshOnReconnect() {

        ClusterClientOptions clusterClientOptions = ClusterClientOptions.builder().topologyRefreshOptions(immediateRefresh)
                .build();

        when(clusterClient.getClusterClientOptions()).thenReturn(clusterClientOptions);

        sut.onReconnectAttempt(10);
        verify(eventExecutors).submit(any(Runnable.class));
    }

    @Test
    void shouldTriggerRefreshOnUncoveredSlot() {

        ClusterClientOptions clusterClientOptions = ClusterClientOptions.builder().topologyRefreshOptions(immediateRefresh)
                .build();

        when(clusterClient.getClusterClientOptions()).thenReturn(clusterClientOptions);

        sut.onUncoveredSlot(1234);
        verify(eventExecutors).submit(any(Runnable.class));
    }

    @Test
    void shouldTriggerRefreshOnUnknownNode() {

        ClusterClientOptions clusterClientOptions = ClusterClientOptions.builder().topologyRefreshOptions(immediateRefresh)
                .build();

        when(clusterClient.getClusterClientOptions()).thenReturn(clusterClientOptions);

        sut.onUnknownNode();
        verify(eventExecutors).submit(any(Runnable.class));
    }

    @Test
    void shouldNotTriggerRefreshOnFirstReconnect() {

        ClusterClientOptions clusterClientOptions = ClusterClientOptions.builder().topologyRefreshOptions(immediateRefresh)
                .build();

        when(clusterClient.getClusterClientOptions()).thenReturn(clusterClientOptions);

        sut.onReconnectAttempt(1);
        verify(eventExecutors, never()).submit(any(Runnable.class));
    }

    @Test
    void shouldRateLimitAdaptiveRequests() {

        ClusterTopologyRefreshOptions adaptiveTimeout = ClusterTopologyRefreshOptions.builder().enablePeriodicRefresh(false)
                .enableAllAdaptiveRefreshTriggers().adaptiveRefreshTriggersTimeout(50, TimeUnit.MILLISECONDS).build();

        ClusterClientOptions clusterClientOptions = ClusterClientOptions.builder().topologyRefreshOptions(adaptiveTimeout)
                .build();

        when(clusterClient.getClusterClientOptions()).thenReturn(clusterClientOptions);

        for (int i = 0; i < 10; i++) {
            sut.onAskRedirection();
        }

        Delay.delay(Duration.ofMillis(100));
        sut.onAskRedirection();

        verify(eventExecutors, times(2)).submit(any(Runnable.class));
    }

    @Test
    void shouldEmitAdaptiveRefreshEventOnSchedule() {

        ClusterClientOptions clusterClientOptions = ClusterClientOptions.builder().topologyRefreshOptions(immediateRefresh)
                .build();

        when(clusterClient.getClusterClientOptions()).thenReturn(clusterClientOptions);

        sut.onMovedRedirection();
        verify(eventExecutors).submit(any(Runnable.class));
        verify(eventBus).publish(any(AdaptiveRefreshTriggeredEvent.class));
    }

    @Test
    void shouldScheduleRefreshViaAdaptiveRefreshTriggeredEvent() {

        ClusterClientOptions clusterClientOptions = ClusterClientOptions.builder().topologyRefreshOptions(immediateRefresh)
                .build();
        when(clusterClient.getClusterClientOptions()).thenReturn(clusterClientOptions);

        sut.onMovedRedirection();

        ArgumentCaptor<AdaptiveRefreshTriggeredEvent> captor = ArgumentCaptor.forClass(AdaptiveRefreshTriggeredEvent.class);
        verify(eventBus).publish(captor.capture());

        AdaptiveRefreshTriggeredEvent capture = captor.getValue();

        capture.scheduleRefresh();
        verify(eventExecutors, times(2)).submit(any(Runnable.class));
    }

    @Test
    void shouldRetrievePartitionsViaAdaptiveRefreshTriggeredEvent() {

        ClusterClientOptions clusterClientOptions = ClusterClientOptions.builder().topologyRefreshOptions(immediateRefresh)
                .build();
        when(clusterClient.getClusterClientOptions()).thenReturn(clusterClientOptions);

        sut.onMovedRedirection();

        ArgumentCaptor<AdaptiveRefreshTriggeredEvent> captor = ArgumentCaptor.forClass(AdaptiveRefreshTriggeredEvent.class);
        verify(eventBus).publish(captor.capture());

        AdaptiveRefreshTriggeredEvent capture = captor.getValue();

        Partitions partitions = new Partitions();
        when(clusterClient.getPartitions()).thenReturn(partitions);

        assertThat(capture.getPartitions()).isSameAs(partitions);
    }

}
