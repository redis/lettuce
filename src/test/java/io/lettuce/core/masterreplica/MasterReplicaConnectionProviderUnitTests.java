package io.lettuce.core.masterreplica;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import io.lettuce.core.ConnectionFuture;
import io.lettuce.core.RedisChannelHandler;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.models.role.RedisInstance;
import io.lettuce.core.protocol.ConnectionIntent;
import io.lettuce.core.resource.ClientResources;
import io.netty.util.concurrent.ImmediateEventExecutor;

/**
 * @author Mark Paluch
 */
@Tag(UNIT_TEST)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MasterReplicaConnectionProviderUnitTests {

    private MasterReplicaConnectionProvider<String, String> sut;

    @Mock
    RedisClient clientMock;

    @Mock(extraInterfaces = StatefulRedisConnection.class)
    RedisChannelHandler<String, String> channelHandlerMock;

    private StatefulRedisConnection<String, String> nodeConnectionMock;

    @Mock
    RedisCommands<String, String> commandsMock;

    @Mock
    ClientResources clientResourcesMock;

    @BeforeEach
    void before() {

        nodeConnectionMock = (StatefulRedisConnection) channelHandlerMock;
        when(clientMock.getResources()).thenReturn(clientResourcesMock);
        when(clientResourcesMock.eventExecutorGroup()).thenReturn(ImmediateEventExecutor.INSTANCE);
        sut = new MasterReplicaConnectionProvider<>(clientMock, StringCodec.UTF8, RedisURI.create("localhost", 1),
                Collections.emptyMap());
        sut.setKnownNodes(Arrays.asList(
                new RedisMasterReplicaNode("localhost", 1, RedisURI.create("localhost", 1), RedisInstance.Role.UPSTREAM)));
    }

    @Test
    void shouldCloseConnections() {

        when(channelHandlerMock.closeAsync()).thenReturn(CompletableFuture.completedFuture(null));

        when(clientMock.connectAsync(eq(StringCodec.UTF8), any()))
                .thenReturn(ConnectionFuture.completed(null, nodeConnectionMock));

        StatefulRedisConnection<String, String> connection = sut.getConnection(ConnectionIntent.READ);
        assertThat(connection).isNotNull();

        sut.close();

        verify(channelHandlerMock).closeAsync();
    }

    @Test
    void shouldNotPropagateCancellationToUnderlyingConnectionAttempt() {

        CompletableFuture<StatefulRedisConnection<String, String>> actualConnection = new CompletableFuture<>();

        when(clientMock.connectAsync(eq(StringCodec.UTF8), any()))
                .thenReturn(ConnectionFuture.from(mock(java.net.SocketAddress.class), actualConnection));

        CompletableFuture<StatefulRedisConnection<String, String>> future = sut.getConnectionAsync(ConnectionIntent.READ);

        assertThat(future.cancel(false)).isTrue();
        assertThat(actualConnection.isCancelled()).isFalse();
    }

    @Test
    void shouldAllowOtherWaitersToSucceedWhenOneIsCancelled() {

        CompletableFuture<StatefulRedisConnection<String, String>> actualConnection = new CompletableFuture<>();

        when(clientMock.connectAsync(eq(StringCodec.UTF8), any()))
                .thenReturn(ConnectionFuture.from(mock(java.net.SocketAddress.class), actualConnection));

        CompletableFuture<StatefulRedisConnection<String, String>> first = sut.getConnectionAsync(ConnectionIntent.READ);
        CompletableFuture<StatefulRedisConnection<String, String>> second = sut.getConnectionAsync(ConnectionIntent.READ);

        assertThat(first.cancel(false)).isTrue();
        assertThat(actualConnection.isCancelled()).isFalse();

        actualConnection.complete(nodeConnectionMock);

        assertThat(second.join()).isSameAs(nodeConnectionMock);
        verify(clientMock, times(1)).connectAsync(eq(StringCodec.UTF8), any());
    }

}
