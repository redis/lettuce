package com.lambdaworks.redis.masterslave;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisConnectionException;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.codec.StringCodec;
import com.lambdaworks.redis.pubsub.RedisPubSubAdapter;
import com.lambdaworks.redis.pubsub.StatefulRedisPubSubConnection;
import com.lambdaworks.redis.pubsub.api.async.RedisPubSubAsyncCommands;
import com.lambdaworks.redis.resource.ClientResources;

import io.netty.util.concurrent.EventExecutorGroup;

/**
 * @author Mark Paluch
 */
@SuppressWarnings("unchecked")
@RunWith(MockitoJUnitRunner.class)
public class SentinelTopologyRefreshTest {

    private static final RedisURI host1 = RedisURI.create("localhost", 1234);
    private static final RedisURI host2 = RedisURI.create("localhost", 3456);

    @Mock
    private RedisClient redisClient;

    @Mock
    private StatefulRedisPubSubConnection<String, String> connection;

    @Mock
    private RedisPubSubAsyncCommands<String, String> pubSubAsyncCommands;

    @Mock
    private ClientResources clientResources;

    @Mock
    private EventExecutorGroup eventExecutors;

    @Mock
    private Runnable refreshRunnable;

    @Captor
    private ArgumentCaptor<Runnable> captor;

    private SentinelTopologyRefresh sut;

    @Before
    public void before() throws Exception {

        when(redisClient.connectPubSub(any(StringCodec.class), eq(host1))).thenReturn(connection);
        when(clientResources.eventExecutorGroup()).thenReturn(eventExecutors);
        when(redisClient.getResources()).thenReturn(clientResources);
        when(connection.async()).thenReturn(pubSubAsyncCommands);

        sut = new SentinelTopologyRefresh(redisClient, "mymaster", Collections.singletonList(host1));
    }

    @Test
    public void bind() throws Exception {

        sut.bind(refreshRunnable);

        verify(redisClient).connectPubSub(any(), any());
        verify(pubSubAsyncCommands).psubscribe("*");
    }

    @Test
    public void bindWithSecondSentinelFails() throws Exception {

        sut = new SentinelTopologyRefresh(redisClient, "mymaster", Arrays.asList(host1, host2));

        when(redisClient.connectPubSub(any(StringCodec.class), eq(host2))).thenThrow(new RedisConnectionException("err"));

        sut.bind(refreshRunnable);

        Map<RedisURI, StatefulRedisPubSubConnection<String, String>> connections = (Map) ReflectionTestUtils.getField(sut,
                "pubSubConnections");

        assertThat(connections).containsKey(host1).hasSize(1);
    }

    @Test
    public void bindWithSentinelRecovery() throws Exception {

        StatefulRedisPubSubConnection<String, String> connection2 = mock(StatefulRedisPubSubConnection.class);
        RedisPubSubAsyncCommands<String, String> async2 = mock(RedisPubSubAsyncCommands.class);
        when(connection2.async()).thenReturn(async2);

        sut = new SentinelTopologyRefresh(redisClient, "mymaster", Arrays.asList(host1, host2));

        when(redisClient.connectPubSub(any(StringCodec.class), eq(host2))).thenThrow(new RedisConnectionException("err"))
                .thenReturn(connection2);

        sut.bind(refreshRunnable);

        verify(redisClient).connectPubSub(any(), eq(host1));
        verify(redisClient).connectPubSub(any(), eq(host2));

        Map<RedisURI, StatefulRedisPubSubConnection<String, String>> connections = (Map) ReflectionTestUtils.getField(sut,
                "pubSubConnections");

        RedisPubSubAdapter<String, String> adapter = getAdapter();

        adapter.message("*", "+sentinel",
                "sentinel c14cc895bb0479c91312cee0e0440b7d99ad367b 127.0.0.1 26380 @ mymaster 127.0.0.1 6483");

        verify(redisClient, times(2)).connectPubSub(any(), eq(host2));
        assertThat(connections).containsKey(host1).containsKey(host2).hasSize(2);
        verify(refreshRunnable, never()).run();
    }

    @Test
    public void bindDuringClose() throws Exception {

        sut = new SentinelTopologyRefresh(redisClient, "mymaster", Arrays.asList(host1, host2));

        StatefulRedisPubSubConnection<String, String> connection2 = mock(StatefulRedisPubSubConnection.class);
        RedisPubSubAsyncCommands<String, String> async2 = mock(RedisPubSubAsyncCommands.class);
        when(connection2.async()).thenReturn(async2);

        when(redisClient.connectPubSub(any(StringCodec.class), eq(host2))).thenAnswer(invocation -> {

            sut.close();
            return connection2;
        });

        sut.bind(refreshRunnable);

        verify(redisClient).connectPubSub(any(), eq(host2));
        verify(connection).close();
        verify(connection2).close();

        Map<RedisURI, StatefulRedisPubSubConnection<String, String>> connections = (Map) ReflectionTestUtils.getField(sut,
                "pubSubConnections");

        assertThat(connections).isEmpty();
    }

    @Test
    public void close() throws Exception {

        sut.bind(refreshRunnable);
        sut.close();

        verify(connection).removeListener(any());
        verify(connection).close();
    }

    @Test
    public void bindAfterClose() throws Exception {

        sut.close();
        sut.bind(refreshRunnable);

        verify(redisClient, times(2)).getResources();
        verifyNoMoreInteractions(redisClient);
    }

    @Test
    public void shouldNotProcessOtherEvents() throws Exception {

        RedisPubSubAdapter<String, String> adapter = getAdapter();

        adapter.message("*", "*", "irreleval");

        verify(redisClient, times(2)).getResources();
        verify(redisClient).connectPubSub(any(), any());
        verifyNoMoreInteractions(redisClient);
    }

    @Test
    public void shouldProcessElectedLeader() throws Exception {

        RedisPubSubAdapter<String, String> adapter = getAdapter();

        adapter.message("*", "+elected-leader", "master mymaster 127.0.0.1");

        verify(eventExecutors, times(1)).schedule(captor.capture(), anyLong(), any());
        captor.getValue().run();
        verify(refreshRunnable, times(1)).run();
    }

    @Test
    public void shouldProcessSwitchMaster() throws Exception {

        RedisPubSubAdapter<String, String> adapter = getAdapter();

        adapter.message("*", "+switch-master", "mymaster 127.0.0.1");

        verify(eventExecutors, times(1)).schedule(captor.capture(), anyLong(), any());
        captor.getValue().run();
        verify(refreshRunnable, times(1)).run();
    }

    @Test
    public void shouldProcessFixSlaveConfig() throws Exception {

        RedisPubSubAdapter<String, String> adapter = getAdapter();

        adapter.message("*", "fix-slave-config", "@ mymaster 127.0.0.1");

        verify(eventExecutors, times(1)).schedule(captor.capture(), anyLong(), any());
        captor.getValue().run();
        verify(refreshRunnable, times(1)).run();
    }

    @Test
    public void shouldProcessFailoverEnd() throws Exception {

        RedisPubSubAdapter<String, String> adapter = getAdapter();

        adapter.message("*", "failover-end", "");

        verify(eventExecutors, times(1)).schedule(captor.capture(), anyLong(), any());
        captor.getValue().run();
        verify(refreshRunnable, times(1)).run();
    }

    @Test
    public void shouldProcessFailoverTimeout() throws Exception {

        RedisPubSubAdapter<String, String> adapter = getAdapter();

        adapter.message("*", "failover-end-for-timeout", "");

        verify(eventExecutors, times(1)).schedule(captor.capture(), anyLong(), any());
        captor.getValue().run();
        verify(refreshRunnable, times(1)).run();
    }

    @Test
    public void shouldExecuteOnceWithinATimeout() throws Exception {

        RedisPubSubAdapter<String, String> adapter = getAdapter();

        adapter.message("*", "failover-end-for-timeout", "");
        adapter.message("*", "failover-end-for-timeout", "");

        verify(eventExecutors, times(1)).schedule(captor.capture(), anyLong(), any());
        captor.getValue().run();
        verify(refreshRunnable, times(1)).run();
    }

    @Test
    public void shouldNotProcessIfExecutorIsShuttingDown() throws Exception {

        RedisPubSubAdapter<String, String> adapter = getAdapter();
        when(eventExecutors.isShuttingDown()).thenReturn(true);

        adapter.message("*", "failover-end-for-timeout", "");

        verify(redisClient).connectPubSub(any(), any());
        verify(eventExecutors, never()).schedule(any(Runnable.class), anyLong(), any());
    }

    private RedisPubSubAdapter<String, String> getAdapter() {

        sut.bind(refreshRunnable);
        return (RedisPubSubAdapter<String, String>) ReflectionTestUtils.getField(sut, "adapter");
    }
}