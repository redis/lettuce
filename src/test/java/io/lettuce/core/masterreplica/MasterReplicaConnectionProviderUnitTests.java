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
import io.lettuce.core.ReadFrom;
import io.lettuce.core.RedisChannelHandler;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.models.role.RedisInstance;
import io.lettuce.core.protocol.ConnectionIntent;

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

    @Mock(extraInterfaces = StatefulRedisConnection.class)
    RedisChannelHandler<String, String> replicaChannelHandlerMock;

    @Mock
    RedisCommands<String, String> commandsMock;

    @BeforeEach
    void before() {

        nodeConnectionMock = (StatefulRedisConnection) channelHandlerMock;
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
    void shouldUseDirectConnectionForSingleReadSelection() {

        when(clientMock.connectAsync(eq(StringCodec.UTF8), any()))
                .thenReturn(ConnectionFuture.completed(null, nodeConnectionMock));

        sut.setReadFrom(ReadFrom.ANY);

        StatefulRedisConnection<String, String> connection = sut.getConnectionAsync(ConnectionIntent.READ).join();

        assertThat(connection).isSameAs(nodeConnectionMock);
        verify(nodeConnectionMock, never()).isOpen();
    }

    @Test
    void shouldSkipFailedReplicaWithOrderSensitiveReadFrom() {

        StatefulRedisConnection<String, String> upstream = upstreamAndReplica();
        when(clientMock.connectAsync(eq(StringCodec.UTF8), argThat(uri -> uri.getPort() == 2))).thenReturn(failedConnection());

        sut.setReadFrom(ReadFrom.REPLICA_PREFERRED);

        StatefulRedisConnection<String, String> connection = sut.getConnectionAsync(ConnectionIntent.READ).join();

        assertThat(connection).isSameAs(upstream);
    }

    @Test
    void shouldSkipFailedReplicaWithRandomReadFrom() {

        StatefulRedisConnection<String, String> upstream = upstreamAndReplica();
        when(clientMock.connectAsync(eq(StringCodec.UTF8), argThat(uri -> uri.getPort() == 2))).thenReturn(failedConnection());

        sut.setReadFrom(ReadFrom.ANY);

        StatefulRedisConnection<String, String> connection = sut.getConnectionAsync(ConnectionIntent.READ).join();

        assertThat(connection).isSameAs(upstream);
    }

    @Test
    void shouldPreferReplicaWhenAllCandidatesConnect() {

        upstreamAndReplica();
        StatefulRedisConnection<String, String> replica = (StatefulRedisConnection) replicaChannelHandlerMock;
        when(replica.isOpen()).thenReturn(true);
        when(clientMock.connectAsync(eq(StringCodec.UTF8), argThat(uri -> uri.getPort() == 2)))
                .thenReturn(ConnectionFuture.completed(null, replica));

        sut.setReadFrom(ReadFrom.REPLICA_PREFERRED);

        StatefulRedisConnection<String, String> connection = sut.getConnectionAsync(ConnectionIntent.READ).join();

        assertThat(connection).isSameAs(replica);
    }

    @Test
    void shouldFailWhenAllReadCandidatesFail() {

        upstreamAndReplica();
        when(clientMock.connectAsync(eq(StringCodec.UTF8), any())).thenReturn(failedConnection());

        sut.setReadFrom(ReadFrom.REPLICA_PREFERRED);

        assertThatThrownBy(() -> sut.getConnectionAsync(ConnectionIntent.READ).join())
                .hasRootCauseInstanceOf(RedisConnectionException.class);

        sut.setReadFrom(ReadFrom.ANY);

        assertThatThrownBy(() -> sut.getConnectionAsync(ConnectionIntent.READ).join())
                .hasRootCauseInstanceOf(RedisConnectionException.class);
    }

    private StatefulRedisConnection<String, String> upstreamAndReplica() {

        sut.setKnownNodes(Arrays.asList(
                new RedisMasterReplicaNode("localhost", 1, RedisURI.create("localhost", 1), RedisInstance.Role.UPSTREAM),
                new RedisMasterReplicaNode("localhost", 2, RedisURI.create("localhost", 2), RedisInstance.Role.REPLICA)));

        when(nodeConnectionMock.isOpen()).thenReturn(true);
        when(clientMock.connectAsync(eq(StringCodec.UTF8), argThat(uri -> uri.getPort() == 1)))
                .thenReturn(ConnectionFuture.completed(null, nodeConnectionMock));

        return nodeConnectionMock;
    }

    private static ConnectionFuture<StatefulRedisConnection<String, String>> failedConnection() {

        CompletableFuture<StatefulRedisConnection<String, String>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RedisConnectionException("Unable to connect"));
        return ConnectionFuture.from(null, failed);
    }

}
