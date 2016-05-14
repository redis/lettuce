package com.lambdaworks.redis.cluster;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.lambdaworks.redis.resource.ClientResources;

import io.netty.util.concurrent.EventExecutorGroup;

/**
 * @author Mark Paluch
 */
@RunWith(MockitoJUnitRunner.class)
public class ClusterTopologyRefreshSchedulerTest {

    private ClusterTopologyRefreshScheduler sut;

    private ClusterTopologyRefreshOptions immediateRefresh = new ClusterTopologyRefreshOptions.Builder().enablePeriodicRefresh(1, TimeUnit.MILLISECONDS)
            .enableAllAdaptiveRefreshTriggers().build();

    private ClusterClientOptions clusterClientOptions = new ClusterClientOptions.Builder()
            .topologyRefreshOptions(immediateRefresh).build();

    @Mock
    private ClientResources clientResources;

    @Mock
    private RedisClusterClient clusterClient;

    @Mock
    private EventExecutorGroup eventExecutors;

    @Before
    public void before() throws Exception {

        when(clientResources.eventExecutorGroup()).thenReturn(eventExecutors);

        sut = new ClusterTopologyRefreshScheduler(clusterClient, clientResources);
    }

    @Test
    public void runShouldSubmitRefreshShouldTrigger() throws Exception {

        when(clusterClient.getClusterClientOptions()).thenReturn(clusterClientOptions);

        sut.run();
        verify(eventExecutors).submit(any(Runnable.class));
    }

    @Test
    public void runnableShouldCallPartitionRefresh() throws Exception {

        when(clusterClient.getClusterClientOptions()).thenReturn(clusterClientOptions);

        when(eventExecutors.submit(any(Runnable.class))).then(invocation -> {
            ((Runnable) invocation.getArguments()[0]).run();
            return null;
        });

        sut.run();

        verify(clusterClient).reloadPartitions();
    }

    @Test
    public void shouldNotSubmitIfOptionsNotSet() throws Exception {

        sut.run();
        verify(eventExecutors, never()).submit(any(Runnable.class));
    }

    @Test
    public void shouldNotSubmitIfExecutorIsShuttingDown() throws Exception {

        when(clusterClient.getClusterClientOptions()).thenReturn(clusterClientOptions);
        when(eventExecutors.isShuttingDown()).thenReturn(true);

        sut.run();
        verify(eventExecutors, never()).submit(any(Runnable.class));
    }

    @Test
    public void shouldNotSubmitIfExecutorIsShutdown() throws Exception {

        when(clusterClient.getClusterClientOptions()).thenReturn(clusterClientOptions);
        when(eventExecutors.isShutdown()).thenReturn(true);

        sut.run();
        verify(eventExecutors, never()).submit(any(Runnable.class));
    }

    @Test
    public void shouldNotSubmitIfExecutorIsTerminated() throws Exception {

        when(clusterClient.getClusterClientOptions()).thenReturn(clusterClientOptions);
        when(eventExecutors.isTerminated()).thenReturn(true);

        sut.run();
        verify(eventExecutors, never()).submit(any(Runnable.class));
    }

    @Test
    public void shouldTriggerRefreshOnAskRedirection() throws Exception {

        ClusterTopologyRefreshOptions clusterTopologyRefreshOptions = new ClusterTopologyRefreshOptions.Builder()
                .enableAllAdaptiveRefreshTriggers().build();

        ClusterClientOptions clusterClientOptions = new ClusterClientOptions.Builder()
                .topologyRefreshOptions(clusterTopologyRefreshOptions).build();

        when(clusterClient.getClusterClientOptions()).thenReturn(clusterClientOptions);

        sut.onAskRedirection();
        verify(eventExecutors).submit(any(Runnable.class));
    }

    @Test
    public void shouldNotTriggerAdaptiveRefreshUsingDefaults() throws Exception {

        ClusterTopologyRefreshOptions clusterTopologyRefreshOptions = ClusterTopologyRefreshOptions.create();

        ClusterClientOptions clusterClientOptions = new ClusterClientOptions.Builder()
                .topologyRefreshOptions(clusterTopologyRefreshOptions).build();

        when(clusterClient.getClusterClientOptions()).thenReturn(clusterClientOptions);

        sut.onAskRedirection();
        verify(eventExecutors, never()).submit(any(Runnable.class));
    }

    @Test
    public void shouldTriggerRefreshOnMovedRedirection() throws Exception {

        ClusterClientOptions clusterClientOptions = new ClusterClientOptions.Builder().topologyRefreshOptions(immediateRefresh)
                .build();

        when(clusterClient.getClusterClientOptions()).thenReturn(clusterClientOptions);

        sut.onMovedRedirection();
        verify(eventExecutors).submit(any(Runnable.class));
    }

    @Test
    public void shouldTriggerRefreshOnReconnect() throws Exception {

        ClusterClientOptions clusterClientOptions = new ClusterClientOptions.Builder().topologyRefreshOptions(immediateRefresh)
                .build();

        when(clusterClient.getClusterClientOptions()).thenReturn(clusterClientOptions);

        sut.onReconnection(10);
        verify(eventExecutors).submit(any(Runnable.class));
    }

    @Test
    public void shouldNotTriggerRefreshOnFirstReconnect() throws Exception {

        ClusterClientOptions clusterClientOptions = new ClusterClientOptions.Builder().topologyRefreshOptions(immediateRefresh)
                .build();

        when(clusterClient.getClusterClientOptions()).thenReturn(clusterClientOptions);

        sut.onReconnection(1);
        verify(eventExecutors, never()).submit(any(Runnable.class));
    }

    @Test
    public void shouldRateLimitAdaptiveRequests() throws Exception {

        ClusterTopologyRefreshOptions adaptiveTimeout = new ClusterTopologyRefreshOptions.Builder().enablePeriodicRefresh(false)
                .enableAllAdaptiveRefreshTriggers().adaptiveRefreshTriggersTimeout(50, TimeUnit.MILLISECONDS).build();

        ClusterClientOptions clusterClientOptions = new ClusterClientOptions.Builder().topologyRefreshOptions(adaptiveTimeout)
                .build();

        when(clusterClient.getClusterClientOptions()).thenReturn(clusterClientOptions);

        for (int i = 0; i < 10; i++) {
            sut.onAskRedirection();
        }

        Thread.sleep(100);
        sut.onAskRedirection();

        verify(eventExecutors, times(2)).submit(any(Runnable.class));
    }
}
