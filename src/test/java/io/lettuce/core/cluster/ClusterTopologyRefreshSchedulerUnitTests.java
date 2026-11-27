package io.lettuce.core.cluster;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
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
import io.lettuce.core.protocol.MaintenanceNotification.SlotMigration;
import io.lettuce.core.protocol.SlotMigrationNotifications;
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
                clusterClient::refreshPartitionsAsync, clusterClient::applyTopologyUpdate, clientResources);
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
    void topologyUpdateShouldIgnoreAdaptiveRefreshTimeout() {

        ClusterTopologyRefreshOptions adaptiveTimeout = ClusterTopologyRefreshOptions.builder().enablePeriodicRefresh(false)
                .enableAllAdaptiveRefreshTriggers().adaptiveRefreshTriggersTimeout(1, TimeUnit.HOURS).build();

        when(clusterClient.getClusterClientOptions())
                .thenReturn(ClusterClientOptions.builder().topologyRefreshOptions(adaptiveTimeout).build());

        sut.onMovedRedirection();
        sut.onMovedRedirection();

        assertThat(sut.requestTopologyUpdate(1, migrations())).isTrue();

        verify(eventExecutors, times(2)).submit(any(Runnable.class));
    }

    @Test
    void topologyUpdateShouldNotRequireAdaptiveTriggers() {

        ClusterTopologyRefreshOptions noTriggers = ClusterTopologyRefreshOptions.builder().enablePeriodicRefresh(false).build();

        when(clusterClient.getClusterClientOptions())
                .thenReturn(ClusterClientOptions.builder().topologyRefreshOptions(noTriggers).build());

        assertThat(sut.requestTopologyUpdate(1, migrations())).isTrue();

        verify(eventExecutors).submit(any(Runnable.class));
        verify(eventBus, never()).publish(any());
    }

    @Test
    void topologyUpdateShouldNotSubmitIfExecutorIsShuttingDown() {

        when(eventExecutors.isShuttingDown()).thenReturn(true);

        assertThat(sut.requestTopologyUpdate(1, migrations())).isFalse();

        verify(eventExecutors, never()).submit(any(Runnable.class));
    }

    @Test
    void topologyUpdateShouldRunOnceWhenIdle() {

        runSubmittedTasksImmediately();
        List<SlotMigration> migrations = migrations();
        when(clusterClient.applyTopologyUpdate(anyList())).thenReturn(CompletableFuture.completedFuture(null));

        sut.requestTopologyUpdate(1, migrations);

        verify(clusterClient).applyTopologyUpdate(migrations);
        verify(clusterClient, never()).refreshPartitionsAsync();
        assertThat(sut.isTopologyRefreshInProgress()).isFalse();
    }

    @Test
    void topologyUpdateDuringRunningRefreshShouldRunAfterIt() {

        when(clusterClient.getClusterClientOptions()).thenReturn(clusterClientOptions);
        runSubmittedTasksImmediately();
        List<SlotMigration> migrations = migrations();
        CompletableFuture<Void> refresh = new CompletableFuture<>();
        when(clusterClient.refreshPartitionsAsync()).thenReturn(refresh);
        when(clusterClient.applyTopologyUpdate(anyList())).thenReturn(CompletableFuture.completedFuture(null));

        sut.run();
        sut.requestTopologyUpdate(1, migrations);

        verify(clusterClient, never()).applyTopologyUpdate(anyList());

        refresh.complete(null);

        verify(clusterClient).applyTopologyUpdate(migrations);
        verify(clusterClient).refreshPartitionsAsync();
        assertThat(sut.isTopologyRefreshInProgress()).isFalse();
    }

    @Test
    void topologyUpdatesShouldRunOneAtATimeInSeqIdOrder() {

        when(clusterClient.getClusterClientOptions()).thenReturn(clusterClientOptions);
        runSubmittedTasksImmediately();
        List<SlotMigration> second = migrations();
        List<SlotMigration> third = migrations();
        CompletableFuture<Void> refresh = new CompletableFuture<>();
        CompletableFuture<Void> secondUpdate = new CompletableFuture<>();
        when(clusterClient.refreshPartitionsAsync()).thenReturn(refresh);
        when(clusterClient.applyTopologyUpdate(second)).thenReturn(secondUpdate);
        when(clusterClient.applyTopologyUpdate(third)).thenReturn(CompletableFuture.completedFuture(null));

        sut.run();
        sut.requestTopologyUpdate(3, third);
        sut.requestTopologyUpdate(2, second);

        refresh.complete(null);

        verify(clusterClient).applyTopologyUpdate(second);
        verify(clusterClient, never()).applyTopologyUpdate(third);

        secondUpdate.complete(null);

        verify(clusterClient).applyTopologyUpdate(third);
        assertThat(sut.isTopologyRefreshInProgress()).isFalse();
    }

    @Test
    void topologyUpdateShouldRunAfterFailedUpdate() {

        runSubmittedTasksImmediately();
        List<SlotMigration> first = migrations();
        List<SlotMigration> second = migrations();
        CompletableFuture<Void> failing = new CompletableFuture<>();
        when(clusterClient.applyTopologyUpdate(first)).thenReturn(failing);
        when(clusterClient.applyTopologyUpdate(second)).thenReturn(CompletableFuture.completedFuture(null));

        sut.requestTopologyUpdate(1, first);
        sut.requestTopologyUpdate(2, second);

        failing.completeExceptionally(new IllegalStateException("boom"));

        verify(clusterClient).applyTopologyUpdate(second);
        assertThat(sut.isTopologyRefreshInProgress()).isFalse();
    }

    @Test
    void adaptiveAndPeriodicRefreshShouldNotRunTopologyUpdates() {

        when(clusterClient.getClusterClientOptions()).thenReturn(clusterClientOptions);
        runSubmittedTasksImmediately();
        when(clusterClient.refreshPartitionsAsync()).thenReturn(CompletableFuture.completedFuture(null));

        sut.run();
        sut.onMovedRedirection();

        verify(clusterClient, atLeastOnce()).refreshPartitionsAsync();
        verify(clusterClient, never()).applyTopologyUpdate(anyList());
    }

    private static List<SlotMigration> migrations() {
        return SlotMigrationNotifications.smigrated(1, "10.0.0.1:6379", "10.0.0.2:6379", "0-9").getMigrations();
    }

    private void runSubmittedTasksImmediately() {

        when(eventExecutors.submit(any(Runnable.class))).then(invocation -> {
            ((Runnable) invocation.getArguments()[0]).run();
            return null;
        });
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
    void doRunShouldResetInProgressFlagOnSynchronousException() {

        when(clusterClient.getClusterClientOptions()).thenReturn(clusterClientOptions);
        when(clusterClient.refreshPartitionsAsync()).thenThrow(new IllegalStateException("boom"));

        when(eventExecutors.submit(any(Runnable.class))).then(invocation -> {
            ((Runnable) invocation.getArguments()[0]).run();
            return null;
        });

        sut.run();

        assertThat(sut.isTopologyRefreshInProgress()).isFalse();

        sut.run();

        verify(clusterClient, times(2)).refreshPartitionsAsync();
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
