package io.lettuce.core.masterreplica;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

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
import io.lettuce.core.RedisException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.models.role.RedisInstance;
import io.lettuce.core.models.role.RedisNodeDescription;
import io.lettuce.core.protocol.ConnectionIntent;

/**
 * @author Mark Paluch
 * @author Sanghun Lee
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
    void shouldReportSelectedNodeToListener() {

        when(clientMock.connectAsync(eq(StringCodec.UTF8), any()))
                .thenReturn(ConnectionFuture.completed(null, nodeConnectionMock));

        sut.setReadFrom(ReadFrom.ANY);

        AtomicReference<RedisNodeDescription> selectedNode = new AtomicReference<>();

        StatefulRedisConnection<String, String> connection = sut.getConnectionAsync(ConnectionIntent.READ, selectedNode::set)
                .join();

        assertThat(connection).isSameAs(nodeConnectionMock);
        assertThat(selectedNode.get()).isNotNull();
        assertThat(selectedNode.get().getUri()).isEqualTo(RedisURI.create("localhost", 1));
    }

    @Test
    void shouldReportSelectedNodeForMultiNodeSelection() {

        sut.setKnownNodes(Arrays.asList(
                new RedisMasterReplicaNode("localhost", 1, RedisURI.create("localhost", 1), RedisInstance.Role.UPSTREAM),
                new RedisMasterReplicaNode("localhost", 2, RedisURI.create("localhost", 2), RedisInstance.Role.REPLICA)));

        when(clientMock.connectAsync(eq(StringCodec.UTF8), any()))
                .thenReturn(ConnectionFuture.completed(null, nodeConnectionMock));
        when(nodeConnectionMock.isOpen()).thenReturn(true);

        sut.setReadFrom(ReadFrom.ANY);

        AtomicReference<RedisNodeDescription> selectedNode = new AtomicReference<>();

        StatefulRedisConnection<String, String> connection = sut.getConnectionAsync(ConnectionIntent.READ, selectedNode::set)
                .join();

        assertThat(connection).isSameAs(nodeConnectionMock);
        assertThat(selectedNode.get()).isNotNull();
        assertThat(selectedNode.get().getUri().getPort()).isIn(1, 2);
    }

    @Test
    void shouldConnectPinnedConnectionToKnownNode() {

        when(clientMock.connectAsync(eq(StringCodec.UTF8), any()))
                .thenReturn(ConnectionFuture.completed(null, nodeConnectionMock));

        StatefulRedisConnection<String, String> connection = sut.getPinnedConnectionAsync(RedisURI.create("localhost", 1))
                .join();

        assertThat(connection).isSameAs(nodeConnectionMock);
    }

    @Test
    void shouldFailPinnedConnectionWhenNodeIsNoLongerKnown() {

        CompletableFuture<StatefulRedisConnection<String, String>> future = sut
                .getPinnedConnectionAsync(RedisURI.create("localhost", 42));

        assertThat(future).isCompletedExceptionally();
        assertThatThrownBy(future::join).hasCauseInstanceOf(RedisException.class).hasMessageContaining("no longer available");
    }

}
