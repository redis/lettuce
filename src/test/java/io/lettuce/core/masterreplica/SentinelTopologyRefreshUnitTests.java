package io.lettuce.core.masterreplica;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.anyString;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import io.lettuce.core.ConnectionFuture;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.internal.Futures;
import io.lettuce.core.masterreplica.SentinelTopologyRefresh.PubSubMessageHandler;
import io.lettuce.core.protocol.AsyncCommand;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.test.ReflectionTestUtils;
import io.netty.util.concurrent.EventExecutorGroup;

/**
 * Unit tests for {@link SentinelTopologyRefresh}.
 *
 * @author Mark Paluch
 */
@Tag(UNIT_TEST)
@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SentinelTopologyRefreshUnitTests {

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

    @BeforeEach
    void before() {

        when(redisClient.connectPubSubAsync(any(StringCodec.class), eq(host1)))
                .thenReturn(ConnectionFuture.completed(null, connection));
        when(clientResources.eventExecutorGroup()).thenReturn(eventExecutors);
        when(redisClient.getResources()).thenReturn(clientResources);
        when(connection.async()).thenReturn(pubSubAsyncCommands);

        AsyncCommand<String, String, Void> command = new AsyncCommand<>(new Command<>(CommandType.PSUBSCRIBE, null));
        command.complete();

        when(connection.async().psubscribe(anyString())).thenReturn(command);

        sut = new SentinelTopologyRefresh(redisClient, "mymaster", Collections.singletonList(host1));
    }

    @AfterEach
    void tearDown() {

        verify(redisClient, never()).connect(any(), any());
        verify(redisClient, never()).connectPubSub(any(), any());
    }

    @Test
    void bind() {

        sut.bind(refreshRunnable);

        verify(redisClient).connectPubSubAsync(any(), any());
        verify(pubSubAsyncCommands).psubscribe("*");
    }

    @Test
    void bindWithSecondSentinelFails() {

        sut = new SentinelTopologyRefresh(redisClient, "mymaster", Arrays.asList(host1, host2));

        when(redisClient.connectPubSubAsync(any(StringCodec.class), eq(host2)))
                .thenReturn(ConnectionFuture.from(null, Futures.failed(new RedisConnectionException("err"))));

        sut.bind(refreshRunnable);

        Map<RedisURI, StatefulRedisPubSubConnection<String, String>> connections = (Map) ReflectionTestUtils.getField(sut,
                "pubSubConnections");

        assertThat(connections).containsKey(host1).hasSize(1);
    }

    @Test
    void bindWithSentinelRecovery() {

        StatefulRedisPubSubConnection<String, String> connection2 = mock(StatefulRedisPubSubConnection.class);
        RedisPubSubAsyncCommands<String, String> async2 = mock(RedisPubSubAsyncCommands.class);
        when(connection2.async()).thenReturn(async2);

        AsyncCommand<String, String, Void> command = new AsyncCommand<>(new Command<>(CommandType.PSUBSCRIBE, null));
        command.complete();

        when(async2.psubscribe(anyString())).thenReturn(command);

        sut = new SentinelTopologyRefresh(redisClient, "mymaster", Arrays.asList(host1, host2));

        when(redisClient.connectPubSubAsync(any(StringCodec.class), eq(host2)))
                .thenReturn(ConnectionFuture.from(null, Futures.failed(new RedisConnectionException("err"))))
                .thenReturn(ConnectionFuture.completed(null, connection2));

        sut.bind(refreshRunnable);

        verify(redisClient).connectPubSubAsync(any(), eq(host1));
        verify(redisClient).connectPubSubAsync(any(), eq(host2));

        Map<RedisURI, StatefulRedisPubSubConnection<String, String>> connections = (Map) ReflectionTestUtils.getField(sut,
                "pubSubConnections");

        PubSubMessageHandler adapter = getMessageHandler();

        adapter.handle("*", "+sentinel",
                "sentinel c14cc895bb0479c91312cee0e0440b7d99ad367b 127.0.0.1 26380 @ mymaster 127.0.0.1 6483");

        verify(eventExecutors, times(1)).schedule(captor.capture(), anyLong(), any());
        captor.getValue().run();

        verify(redisClient, times(2)).connectPubSubAsync(any(), eq(host2));
        assertThat(connections).containsKey(host1).containsKey(host2).hasSize(2);
        verify(refreshRunnable, never()).run();
    }

    @Test
    void bindDuringClose() {

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
    void close() {

        when(connection.closeAsync()).thenReturn(CompletableFuture.completedFuture(null));

        sut.bind(refreshRunnable);
        sut.close();

        verify(connection).closeAsync();
    }

    @Test
    void bindAfterClose() {

        sut.close();
        sut.bind(refreshRunnable);

        verify(redisClient, times(2)).getResources();
        verifyNoMoreInteractions(redisClient);
    }

    @Test
    void shouldNotProcessOtherEvents() {

        PubSubMessageHandler adapter = getMessageHandler();
        sut.bind(refreshRunnable);

        adapter.handle("*", "*", "irrelevant");

        verify(redisClient, times(3)).getResources();
        verify(redisClient).connectPubSubAsync(any(), any());
        verifyNoMoreInteractions(redisClient);
    }

    @Test
    void shouldProcessSlaveDown() {

        PubSubMessageHandler adapter = getMessageHandler();
        sut.bind(refreshRunnable);

        adapter.handle("*", "+sdown", "replica 127.0.0.1:6483 127.0.0.1 6483-2020 @ mymaster 127.0.0.1 6482");

        verify(eventExecutors, times(1)).schedule(captor.capture(), anyLong(), any());
        captor.getValue().run();
        verify(refreshRunnable, times(1)).run();
    }

    @Test
    void shouldProcessSlaveAdded() {

        PubSubMessageHandler adapter = getMessageHandler();
        sut.bind(refreshRunnable);

        adapter.handle("*", "+slave", "replica 127.0.0.1:8483 127.0.0.1 8483-2020 @ mymaster 127.0.0.1 6482");

        verify(eventExecutors, times(1)).schedule(captor.capture(), anyLong(), any());
        captor.getValue().run();
        verify(refreshRunnable, times(1)).run();
    }

    @Test
    void shouldProcessSlaveBackUp() {

        PubSubMessageHandler adapter = getMessageHandler();
        sut.bind(refreshRunnable);

        adapter.handle("*", "-sdown", "replica 127.0.0.1:6483 127.0.0.1 6483-2020 @ mymaster 127.0.0.1 6482");

        verify(eventExecutors, times(1)).schedule(captor.capture(), anyLong(), any());
        captor.getValue().run();
        verify(refreshRunnable, times(1)).run();
    }

    @Test
    void shouldProcessElectedLeader() {

        PubSubMessageHandler adapter = getMessageHandler();
        sut.bind(refreshRunnable);

        adapter.handle("*", "+elected-leader", "master mymaster 127.0.0.1");

        verify(eventExecutors, times(1)).schedule(captor.capture(), anyLong(), any());
        captor.getValue().run();
        verify(refreshRunnable, times(1)).run();
    }

    @Test
    void shouldProcessSwitchMaster() {

        PubSubMessageHandler adapter = getMessageHandler();
        sut.bind(refreshRunnable);

        adapter.handle("*", "+switch-master", "mymaster 127.0.0.1");

        verify(eventExecutors, times(1)).schedule(captor.capture(), anyLong(), any());
        captor.getValue().run();
        verify(refreshRunnable, times(1)).run();
    }

    @Test
    void shouldProcessFixSlaveConfig() {

        PubSubMessageHandler adapter = getMessageHandler();
        sut.bind(refreshRunnable);

        adapter.handle("*", "fix-slave-config", "@ mymaster 127.0.0.1");

        verify(eventExecutors, times(1)).schedule(captor.capture(), anyLong(), any());
        captor.getValue().run();
        verify(refreshRunnable, times(1)).run();
    }

    @Test
    void shouldProcessConvertToSlave() {

        PubSubMessageHandler adapter = getMessageHandler();
        sut.bind(refreshRunnable);

        adapter.handle("*", "+convert-to-slave", "@ mymaster 127.0.0.1");

        verify(eventExecutors, times(1)).schedule(captor.capture(), anyLong(), any());
        captor.getValue().run();
        verify(refreshRunnable, times(1)).run();
    }

    @Test
    void shouldProcessRoleChange() {

        PubSubMessageHandler adapter = getMessageHandler();
        sut.bind(refreshRunnable);

        adapter.handle("*", "+role-change", "@ mymaster 127.0.0.1");

        verify(eventExecutors, times(1)).schedule(captor.capture(), anyLong(), any());
        captor.getValue().run();
        verify(refreshRunnable, times(1)).run();
    }

    @Test
    void shouldProcessFailoverEnd() {

        PubSubMessageHandler adapter = getMessageHandler();
        sut.bind(refreshRunnable);

        adapter.handle("*", "failover-end", "");

        verify(eventExecutors, times(1)).schedule(captor.capture(), anyLong(), any());
        captor.getValue().run();
        verify(refreshRunnable, times(1)).run();
    }

    @Test
    void shouldProcessFailoverTimeout() {

        PubSubMessageHandler adapter = getMessageHandler();
        sut.bind(refreshRunnable);

        adapter.handle("*", "failover-end-for-timeout", "");

        verify(eventExecutors, times(1)).schedule(captor.capture(), anyLong(), any());
        captor.getValue().run();
        verify(refreshRunnable, times(1)).run();
    }

    @Test
    void shouldExecuteOnceWithinATimeout() {

        PubSubMessageHandler adapter = getMessageHandler();
        sut.bind(refreshRunnable);

        adapter.handle("*", "failover-end-for-timeout", "");
        adapter.handle("*", "failover-end-for-timeout", "");

        verify(eventExecutors, times(1)).schedule(captor.capture(), anyLong(), any());
        captor.getValue().run();
        verify(refreshRunnable, times(1)).run();
    }

    @Test
    void shouldNotProcessIfExecutorIsShuttingDown() {

        PubSubMessageHandler adapter = getMessageHandler();
        sut.bind(refreshRunnable);
        when(eventExecutors.isShuttingDown()).thenReturn(true);

        adapter.handle("*", "failover-end-for-timeout", "");

        verify(redisClient).connectPubSubAsync(any(), any());
        verify(eventExecutors, never()).schedule(any(Runnable.class), anyLong(), any());
    }

    private PubSubMessageHandler getMessageHandler() {
        return ReflectionTestUtils.getField(sut, "messageHandler");
    }

}
