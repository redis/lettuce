package com.lambdaworks.redis.masterslave;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.codec.Utf8StringCodec;
import com.lambdaworks.redis.pubsub.RedisPubSubAdapter;
import com.lambdaworks.redis.pubsub.StatefulRedisPubSubConnection;
import com.lambdaworks.redis.pubsub.api.async.RedisPubSubAsyncCommands;
import com.lambdaworks.redis.resource.ClientResources;

import io.netty.util.concurrent.EventExecutorGroup;

/**
 * @author Mark Paluch
 */
@RunWith(MockitoJUnitRunner.class)
public class SentinelTopologyRefreshTest {

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

    private SentinelTopologyRefresh sut;

    @Before
    public void before() throws Exception {

        sut = new SentinelTopologyRefresh(redisClient, "mymaster", Arrays.asList(RedisURI.create("localhost", 1234)));

        when(redisClient.connectPubSub(any(Utf8StringCodec.class), any())).thenReturn(connection);
        when(clientResources.eventExecutorGroup()).thenReturn(eventExecutors);
        when(redisClient.getResources()).thenReturn(clientResources);
        when(connection.async()).thenReturn(pubSubAsyncCommands);
    }

    @Test
    public void bind() throws Exception {

        sut.bind(refreshRunnable);

        verify(redisClient).connectPubSub(any(), any());
        verify(pubSubAsyncCommands).psubscribe("*");
    }

    @Test
    public void close() throws Exception {

        sut.bind(refreshRunnable);
        sut.close();

        verify(connection).removeListener(any());
        verify(connection).close();
    }

    @Test
    public void shouldNotProcessOtherEvents() throws Exception {

        RedisPubSubAdapter<String, String> adapter = getAdapter();

        adapter.message("*", "*", "irreleval");

        verify(eventExecutors).isShuttingDown();
        verifyNoMoreInteractions(eventExecutors);
    }

    @Test
    public void shouldProcessElectedLeader() throws Exception {

        RedisPubSubAdapter<String, String> adapter = getAdapter();

        adapter.message("*", "+elected-leader", "master mymaster 127.0.0.1");

        verify(eventExecutors).schedule(any(Runnable.class), anyLong(), any());
    }

    @Test
    public void shouldProcessSwitchMaster() throws Exception {

        RedisPubSubAdapter<String, String> adapter = getAdapter();

        adapter.message("*", "+switch-master", "mymaster 127.0.0.1");

        verify(eventExecutors).schedule(any(Runnable.class), anyLong(), any());
    }

    @Test
    public void shouldProcessFixSlaveConfig() throws Exception {

        RedisPubSubAdapter<String, String> adapter = getAdapter();

        adapter.message("*", "fix-slave-config", "@ mymaster 127.0.0.1");

        verify(eventExecutors).schedule(any(Runnable.class), anyLong(), any());
    }

    @Test
    public void shouldProcessFailoverEnd() throws Exception {

        RedisPubSubAdapter<String, String> adapter = getAdapter();

        adapter.message("*", "failover-end", "");

        verify(eventExecutors).schedule(any(Runnable.class), anyLong(), any());
    }

    @Test
    public void shouldProcessFailoverTimeout() throws Exception {

        RedisPubSubAdapter<String, String> adapter = getAdapter();

        adapter.message("*", "failover-end-for-timeout", "");

        verify(eventExecutors).schedule(any(Runnable.class), anyLong(), any());
    }

    @Test
    public void shouldExecuteOnceWithinATimeout() throws Exception {

        RedisPubSubAdapter<String, String> adapter = getAdapter();

        adapter.message("*", "failover-end-for-timeout", "");
        adapter.message("*", "failover-end-for-timeout", "");

        verify(eventExecutors, times(1)).schedule(any(Runnable.class), anyLong(), any());
    }

    @Test
    public void shouldNotProcessIfExecutorIsShuttingDown() throws Exception {

        RedisPubSubAdapter<String, String> adapter = getAdapter();
        when(eventExecutors.isShuttingDown()).thenReturn(true);

        adapter.message("*", "failover-end-for-timeout", "");

        verify(eventExecutors, never()).schedule(any(Runnable.class), anyLong(), any());
    }

    private RedisPubSubAdapter<String, String> getAdapter() {

        sut.bind(refreshRunnable);
        return (RedisPubSubAdapter<String, String>) ReflectionTestUtils.getField(sut,
                "adapter");
    }
}