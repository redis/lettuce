/*
 * Copyright 2011-2017 the original author or authors.
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
package io.lettuce.core.masterslave;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import io.lettuce.core.ConnectionFuture;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.internal.Futures;
import io.lettuce.core.protocol.AsyncCommand;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.pubsub.RedisPubSubAdapter;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands;
import io.lettuce.core.resource.ClientResources;
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
    public void before() {

        when(redisClient.connectPubSubAsync(any(StringCodec.class), eq(host1))).thenReturn(
                ConnectionFuture.completed(null, connection));
        when(clientResources.eventExecutorGroup()).thenReturn(eventExecutors);
        when(redisClient.getResources()).thenReturn(clientResources);
        when(connection.async()).thenReturn(pubSubAsyncCommands);

        AsyncCommand<String, String, Void> command = new AsyncCommand<>(new Command<>(CommandType.PSUBSCRIBE, null));
        command.complete();

        when(connection.async().psubscribe(anyString())).thenReturn(command);

        sut = new SentinelTopologyRefresh(redisClient, "mymaster", Collections.singletonList(host1));
    }

    @After
    public void tearDown() {

        verify(redisClient, never()).connect(any(), any());
        verify(redisClient, never()).connectPubSub(any(), any());
    }

    @Test
    public void bind() {

        sut.bind(refreshRunnable);

        verify(redisClient).connectPubSubAsync(any(), any());
        verify(pubSubAsyncCommands).psubscribe("*");
    }

    @Test
    public void bindWithSecondSentinelFails() {

        sut = new SentinelTopologyRefresh(redisClient, "mymaster", Arrays.asList(host1, host2));

        when(redisClient.connectPubSubAsync(any(StringCodec.class), eq(host2))).thenReturn(
                ConnectionFuture.from(null, Futures.failed(new RedisConnectionException("err"))));

        sut.bind(refreshRunnable);

        Map<RedisURI, StatefulRedisPubSubConnection<String, String>> connections = (Map) ReflectionTestUtils.getField(sut,
                "pubSubConnections");

        assertThat(connections).containsKey(host1).hasSize(1);
    }

    @Test
    public void bindWithSentinelRecovery() {

        StatefulRedisPubSubConnection<String, String> connection2 = mock(StatefulRedisPubSubConnection.class);
        RedisPubSubAsyncCommands<String, String> async2 = mock(RedisPubSubAsyncCommands.class);
        when(connection2.async()).thenReturn(async2);

        AsyncCommand<String, String, Void> command = new AsyncCommand<>(new Command<>(CommandType.PSUBSCRIBE, null));
        command.complete();

        when(async2.psubscribe(anyString())).thenReturn(command);

        sut = new SentinelTopologyRefresh(redisClient, "mymaster", Arrays.asList(host1, host2));

        when(redisClient.connectPubSubAsync(any(StringCodec.class), eq(host2))).thenReturn(
                ConnectionFuture.from(null, Futures.failed(new RedisConnectionException("err")))).thenReturn(
                ConnectionFuture.completed(null, connection2));

        sut.bind(refreshRunnable);

        verify(redisClient).connectPubSubAsync(any(), eq(host1));
        verify(redisClient).connectPubSubAsync(any(), eq(host2));

        Map<RedisURI, StatefulRedisPubSubConnection<String, String>> connections = (Map) ReflectionTestUtils.getField(sut,
                "pubSubConnections");

        RedisPubSubAdapter<String, String> adapter = getAdapter();

        adapter.message("*", "+sentinel",
                "sentinel c14cc895bb0479c91312cee0e0440b7d99ad367b 127.0.0.1 26380 @ mymaster 127.0.0.1 6483");

        verify(eventExecutors, times(1)).schedule(captor.capture(), anyLong(), any());
        captor.getValue().run();

        verify(redisClient, times(2)).connectPubSubAsync(any(), eq(host2));
        assertThat(connections).containsKey(host1).containsKey(host2).hasSize(2);
        verify(refreshRunnable, never()).run();
    }

    @Test
    public void bindDuringClose() {

        sut = new SentinelTopologyRefresh(redisClient, "mymaster", Arrays.asList(host1, host2));

        StatefulRedisPubSubConnection<String, String> connection2 = mock(StatefulRedisPubSubConnection.class);
        when(connection.closeAsync()).thenReturn(CompletableFuture.completedFuture(null));
        when(connection2.closeAsync()).thenReturn(CompletableFuture.completedFuture(null));

        when(redisClient.connectPubSubAsync(any(StringCodec.class), eq(host2))).thenAnswer(invocation -> {

            sut.closeAsync();
            return ConnectionFuture.completed(null, connection2);
        });

        sut.bind(refreshRunnable);

        verify(redisClient).connectPubSubAsync(any(), eq(host2));
        verify(connection).closeAsync();
        verify(connection2).closeAsync();

        Map<RedisURI, StatefulRedisPubSubConnection<String, String>> connections = (Map) ReflectionTestUtils.getField(sut,
                "pubSubConnections");

        assertThat(connections).isEmpty();
    }

    @Test
    public void close() {

        when(connection.closeAsync()).thenReturn(CompletableFuture.completedFuture(null));

        sut.bind(refreshRunnable);
        sut.close();

        verify(connection).removeListener(any());
        verify(connection).closeAsync();
    }

    @Test
    public void bindAfterClose() {

        sut.close();
        sut.bind(refreshRunnable);

        verify(redisClient, times(2)).getResources();
        verifyNoMoreInteractions(redisClient);
    }

    @Test
    public void shouldNotProcessOtherEvents() {

        RedisPubSubAdapter<String, String> adapter = getAdapter();
        sut.bind(refreshRunnable);

        adapter.message("*", "*", "irreleval");

        verify(redisClient, times(3)).getResources();
        verify(redisClient).connectPubSubAsync(any(), any());
        verifyNoMoreInteractions(redisClient);
    }

    @Test
    public void shouldProcessElectedLeader() {

        RedisPubSubAdapter<String, String> adapter = getAdapter();
        sut.bind(refreshRunnable);

        adapter.message("*", "+elected-leader", "master mymaster 127.0.0.1");

        verify(eventExecutors, times(1)).schedule(captor.capture(), anyLong(), any());
        captor.getValue().run();
        verify(refreshRunnable, times(1)).run();
    }

    @Test
    public void shouldProcessSwitchMaster() {

        RedisPubSubAdapter<String, String> adapter = getAdapter();
        sut.bind(refreshRunnable);

        adapter.message("*", "+switch-master", "mymaster 127.0.0.1");

        verify(eventExecutors, times(1)).schedule(captor.capture(), anyLong(), any());
        captor.getValue().run();
        verify(refreshRunnable, times(1)).run();
    }

    @Test
    public void shouldProcessFixSlaveConfig() {

        RedisPubSubAdapter<String, String> adapter = getAdapter();
        sut.bind(refreshRunnable);

        adapter.message("*", "fix-slave-config", "@ mymaster 127.0.0.1");

        verify(eventExecutors, times(1)).schedule(captor.capture(), anyLong(), any());
        captor.getValue().run();
        verify(refreshRunnable, times(1)).run();
    }

    @Test
    public void shouldProcessConvertToSlave() {

        RedisPubSubAdapter<String, String> adapter = getAdapter();
        sut.bind(refreshRunnable);

        adapter.message("*", "+convert-to-slave", "@ mymaster 127.0.0.1");

        verify(eventExecutors, times(1)).schedule(captor.capture(), anyLong(), any());
        captor.getValue().run();
        verify(refreshRunnable, times(1)).run();
    }

    @Test
    public void shouldProcessRoleChange() {

        RedisPubSubAdapter<String, String> adapter = getAdapter();
        sut.bind(refreshRunnable);

        adapter.message("*", "+role-change", "@ mymaster 127.0.0.1");

        verify(eventExecutors, times(1)).schedule(captor.capture(), anyLong(), any());
        captor.getValue().run();
        verify(refreshRunnable, times(1)).run();
    }

    @Test
    public void shouldProcessFailoverEnd() {

        RedisPubSubAdapter<String, String> adapter = getAdapter();
        sut.bind(refreshRunnable);

        adapter.message("*", "failover-end", "");

        verify(eventExecutors, times(1)).schedule(captor.capture(), anyLong(), any());
        captor.getValue().run();
        verify(refreshRunnable, times(1)).run();
    }

    @Test
    public void shouldProcessFailoverTimeout() {

        RedisPubSubAdapter<String, String> adapter = getAdapter();
        sut.bind(refreshRunnable);

        adapter.message("*", "failover-end-for-timeout", "");

        verify(eventExecutors, times(1)).schedule(captor.capture(), anyLong(), any());
        captor.getValue().run();
        verify(refreshRunnable, times(1)).run();
    }

    @Test
    public void shouldExecuteOnceWithinATimeout() {

        RedisPubSubAdapter<String, String> adapter = getAdapter();
        sut.bind(refreshRunnable);

        adapter.message("*", "failover-end-for-timeout", "");
        adapter.message("*", "failover-end-for-timeout", "");

        verify(eventExecutors, times(1)).schedule(captor.capture(), anyLong(), any());
        captor.getValue().run();
        verify(refreshRunnable, times(1)).run();
    }

    @Test
    public void shouldNotProcessIfExecutorIsShuttingDown() {

        RedisPubSubAdapter<String, String> adapter = getAdapter();
        sut.bind(refreshRunnable);
        when(eventExecutors.isShuttingDown()).thenReturn(true);

        adapter.message("*", "failover-end-for-timeout", "");

        verify(redisClient).connectPubSubAsync(any(), any());
        verify(eventExecutors, never()).schedule(any(Runnable.class), anyLong(), any());
    }

    private RedisPubSubAdapter<String, String> getAdapter() {
        return (RedisPubSubAdapter<String, String>) ReflectionTestUtils.getField(sut, "adapter");
    }
}
