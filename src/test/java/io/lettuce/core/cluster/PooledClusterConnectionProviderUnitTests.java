/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.cluster;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
import io.lettuce.core.RedisChannelWriter;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.RedisException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.push.PushListener;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.models.role.RedisNodeDescription;
import io.lettuce.core.protocol.AsyncCommand;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.protocol.ConnectionIntent;
import io.lettuce.core.resource.ClientResources;

/**
 * Unit tests for {@link PooledClusterConnectionProvider}.
 *
 * @author Mark Paluch
 */
@Tag(UNIT_TEST)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PooledClusterConnectionProviderUnitTests {

    private PooledClusterConnectionProvider<String, String> sut;

    @Mock
    SocketAddress socketAddressMock;

    @Mock
    RedisClusterClient clientMock;

    @Mock
    RedisChannelWriter writerMock;

    @Mock(extraInterfaces = StatefulRedisConnection.class)
    RedisChannelHandler<String, String> channelHandlerMock;

    private StatefulRedisConnection<String, String> nodeConnectionMock;

    @Mock
    RedisCommands<String, String> commandsMock;

    @Mock
    RedisAsyncCommands<String, String> asyncCommandsMock;

    @Mock
    ClientResources clientResourcesMock;

    @Mock
    ClusterEventListener clusterEventListener;

    private Partitions partitions = new Partitions();

    @BeforeEach
    void before() {

        nodeConnectionMock = (StatefulRedisConnection) channelHandlerMock;

        sut = new PooledClusterConnectionProvider<>(clientMock, writerMock, StringCodec.UTF8, clusterEventListener);

        List<Integer> slots1 = IntStream.range(0, 8192).boxed().collect(Collectors.toList());
        List<Integer> slots2 = IntStream.range(8192, SlotHash.SLOT_COUNT).boxed().collect(Collectors.toList());

        partitions.add(new RedisClusterNode(RedisURI.create("localhost", 1), "1", true, null, 0, 0, 0, slots1,
                Collections.singleton(RedisClusterNode.NodeFlag.UPSTREAM)));
        partitions.add(new RedisClusterNode(RedisURI.create("localhost", 2), "2", true, "1", 0, 0, 0, slots2,
                Collections.singleton(RedisClusterNode.NodeFlag.REPLICA)));

        sut.setPartitions(partitions);

        when(nodeConnectionMock.async()).thenReturn(asyncCommandsMock);
    }

    @Test
    void shouldObtainConnection() {

        when(clientMock.connectToNodeAsync(eq(StringCodec.UTF8), eq("localhost:1"), any(), any()))
                .thenReturn(ConnectionFuture.from(socketAddressMock, CompletableFuture.completedFuture(nodeConnectionMock)));

        StatefulRedisConnection<String, String> connection = sut.getConnection(ConnectionIntent.READ, 1);

        assertThat(connection).isSameAs(nodeConnectionMock);
        verify(connection).setAutoFlushCommands(true);
        verify(connection).addListener(any(PushListener.class));
        verifyNoMoreInteractions(connection);
    }

    @Test
    void shouldReuseMasterConnectionForReadFromMaster() {

        when(clientMock.connectToNodeAsync(eq(StringCodec.UTF8), eq("localhost:1"), any(), any()))
                .thenReturn(ConnectionFuture.from(socketAddressMock, CompletableFuture.completedFuture(nodeConnectionMock)));

        sut.setReadFrom(ReadFrom.UPSTREAM);

        StatefulRedisConnection<String, String> connection = sut.getConnection(ConnectionIntent.READ, 1);

        assertThat(connection).isSameAs(nodeConnectionMock);
        verify(connection).setAutoFlushCommands(true);
        verify(connection).addListener(any(PushListener.class));
        verifyNoMoreInteractions(connection);
    }

    @Test
    void shouldObtainConnectionReadFromReplica() {

        when(clientMock.connectToNodeAsync(eq(StringCodec.UTF8), eq("localhost:2"), any(), any()))
                .thenReturn(ConnectionFuture.from(socketAddressMock, CompletableFuture.completedFuture(nodeConnectionMock)));

        AsyncCommand<String, String, String> async = new AsyncCommand<>(new Command<>(CommandType.READONLY, null, null));
        async.complete();

        when(asyncCommandsMock.readOnly()).thenReturn(async);

        sut.setReadFrom(ReadFrom.REPLICA);

        StatefulRedisConnection<String, String> connection = sut.getConnection(ConnectionIntent.READ, 1);

        assertThat(connection).isSameAs(nodeConnectionMock);
        verify(connection).async();
        verify(asyncCommandsMock).readOnly();
        verify(connection).setAutoFlushCommands(true);
    }

    @Test
    void shouldAvoidReplicaWithReplOffsetZero() {

        for (RedisClusterNode partition : partitions) {
            partition.setReplOffset(0);
        }

        AsyncCommand<String, String, String> async = new AsyncCommand<>(new Command<>(CommandType.READONLY, null, null));
        async.complete();

        when(asyncCommandsMock.readOnly()).thenReturn(async);

        sut.setReadFrom(ReadFrom.REPLICA);

        assertThatExceptionOfType(PartitionSelectorException.class)
                .isThrownBy(() -> sut.getConnection(ConnectionIntent.READ, 1));
    }

    @Test
    void shouldRandomizeReadNode() {

        StatefulRedisConnection<String, String> nodeConnectionMock2 = mock(StatefulRedisConnection.class);
        when(nodeConnectionMock.isOpen()).thenReturn(true);
        when(nodeConnectionMock2.isOpen()).thenReturn(true);

        when(clientMock.connectToNodeAsync(eq(StringCodec.UTF8), eq("localhost:1"), any(), any()))
                .thenReturn(ConnectionFuture.from(socketAddressMock, CompletableFuture.completedFuture(nodeConnectionMock)));

        when(clientMock.connectToNodeAsync(eq(StringCodec.UTF8), eq("localhost:2"), any(), any()))
                .thenReturn(ConnectionFuture.from(socketAddressMock, CompletableFuture.completedFuture(nodeConnectionMock2)));

        AsyncCommand<String, String, String> async = new AsyncCommand<>(new Command<>(CommandType.READONLY, null, null));
        async.complete();

        when(asyncCommandsMock.readOnly()).thenReturn(async);
        when(nodeConnectionMock2.async()).thenReturn(asyncCommandsMock);

        sut.setReadFrom(ReadFrom.ANY);

        List<StatefulRedisConnection<String, String>> readCandidates = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            readCandidates.add(sut.getConnection(ConnectionIntent.READ, 1));
        }

        assertThat(readCandidates).contains(nodeConnectionMock, nodeConnectionMock2);
    }

    @Test
    void shouldNotRandomizeReadNode() {

        StatefulRedisConnection<String, String> nodeConnectionMock2 = mock(StatefulRedisConnection.class);
        when(nodeConnectionMock.isOpen()).thenReturn(true);
        when(nodeConnectionMock2.isOpen()).thenReturn(true);

        when(clientMock.connectToNodeAsync(eq(StringCodec.UTF8), eq("localhost:1"), any(), any()))
                .thenReturn(ConnectionFuture.from(socketAddressMock, CompletableFuture.completedFuture(nodeConnectionMock)));

        when(clientMock.connectToNodeAsync(eq(StringCodec.UTF8), eq("localhost:2"), any(), any()))
                .thenReturn(ConnectionFuture.from(socketAddressMock, CompletableFuture.completedFuture(nodeConnectionMock2)));

        AsyncCommand<String, String, String> async = new AsyncCommand<>(new Command<>(CommandType.READONLY, null, null));
        async.complete();

        when(asyncCommandsMock.readOnly()).thenReturn(async);
        when(nodeConnectionMock2.async()).thenReturn(asyncCommandsMock);

        sut.setReadFrom(ReadFrom.REPLICA);

        List<StatefulRedisConnection<String, String>> readCandidates = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            readCandidates.add(sut.getConnection(ConnectionIntent.READ, 1));
        }

        assertThat(readCandidates).contains(nodeConnectionMock2).doesNotContain(nodeConnectionMock);
    }

    @Test
    void shouldCloseConnectionOnConnectFailure() {

        when(clientMock.connectToNodeAsync(eq(StringCodec.UTF8), eq("localhost:2"), any(), any()))
                .thenReturn(ConnectionFuture.from(socketAddressMock, CompletableFuture.completedFuture(nodeConnectionMock)));

        AsyncCommand<String, String, String> async = new AsyncCommand<>(new Command<>(CommandType.READONLY, null, null));
        async.completeExceptionally(new RuntimeException());

        when(asyncCommandsMock.readOnly()).thenReturn(async);

        sut.setReadFrom(ReadFrom.REPLICA);

        try {
            sut.getConnection(ConnectionIntent.READ, 1);
            fail("Missing RedisException");
        } catch (RedisException e) {
            assertThat(e).hasRootCauseInstanceOf(RuntimeException.class);
        }

        verify(nodeConnectionMock).close();
        verify(clientMock).connectToNodeAsync(eq(StringCodec.UTF8), eq("localhost:2"), any(), any());
    }

    @Test
    void shouldRetryConnectionAttemptAfterConnectionAttemptWasBroken() {

        when(clientMock.connectToNodeAsync(eq(StringCodec.UTF8), eq("localhost:2"), any(), any()))
                .thenReturn(ConnectionFuture.from(socketAddressMock, CompletableFuture.completedFuture(nodeConnectionMock)));

        AsyncCommand<String, String, String> async = new AsyncCommand<>(new Command<>(CommandType.READONLY, null, null));
        async.completeExceptionally(new RuntimeException());

        when(asyncCommandsMock.readOnly()).thenReturn(async);

        sut.setReadFrom(ReadFrom.REPLICA);

        try {
            sut.getConnection(ConnectionIntent.READ, 1);
            fail("Missing RedisException");
        } catch (RedisException e) {
            assertThat(e).hasRootCauseInstanceOf(RuntimeException.class);
        }
        verify(nodeConnectionMock).close();

        async = new AsyncCommand<>(new Command<>(CommandType.READONLY, null, null));
        async.complete();

        when(asyncCommandsMock.readOnly()).thenReturn(async);

        sut.getConnection(ConnectionIntent.READ, 1);

        verify(clientMock, times(2)).connectToNodeAsync(eq(StringCodec.UTF8), eq("localhost:2"), any(), any());
    }

    @Test
    void shouldSelectSuccessfulConnectionIfOtherNodesFailed() {

        CompletableFuture<StatefulRedisConnection<String, String>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new IllegalStateException());

        when(clientMock.connectToNodeAsync(eq(StringCodec.UTF8), eq("localhost:1"), any(), any()))
                .thenReturn(ConnectionFuture.from(socketAddressMock, failed));

        when(clientMock.connectToNodeAsync(eq(StringCodec.UTF8), eq("localhost:2"), any(), any()))
                .thenReturn(ConnectionFuture.from(socketAddressMock, CompletableFuture.completedFuture(nodeConnectionMock)));

        AsyncCommand<String, String, String> async = new AsyncCommand<>(new Command<>(CommandType.READONLY, null, null));
        async.complete("OK");

        when(asyncCommandsMock.readOnly()).thenReturn(async);

        sut.setReadFrom(ReadFrom.UPSTREAM_PREFERRED);

        assertThat(sut.getConnection(ConnectionIntent.READ, 1)).isNotNull().isSameAs(nodeConnectionMock);

        // cache access
        assertThat(sut.getConnection(ConnectionIntent.READ, 1)).isNotNull().isSameAs(nodeConnectionMock);

        verify(clientMock).connectToNodeAsync(eq(StringCodec.UTF8), eq("localhost:1"), any(), any());
        verify(clientMock).connectToNodeAsync(eq(StringCodec.UTF8), eq("localhost:2"), any(), any());
    }

    @Test
    void shouldFailIfAllReadCandidateNodesFail() {

        CompletableFuture<StatefulRedisConnection<String, String>> failed1 = new CompletableFuture<>();
        failed1.completeExceptionally(new IllegalStateException());

        CompletableFuture<StatefulRedisConnection<String, String>> failed2 = new CompletableFuture<>();
        failed2.completeExceptionally(new IllegalStateException());

        when(clientMock.connectToNodeAsync(eq(StringCodec.UTF8), eq("localhost:1"), any(), any()))
                .thenReturn(ConnectionFuture.from(socketAddressMock, failed2));

        when(clientMock.connectToNodeAsync(eq(StringCodec.UTF8), eq("localhost:2"), any(), any()))
                .thenReturn(ConnectionFuture.from(socketAddressMock, failed2));

        AsyncCommand<String, String, String> async = new AsyncCommand<>(new Command<>(CommandType.READONLY, null, null));
        async.complete("OK");

        sut.setReadFrom(ReadFrom.UPSTREAM_PREFERRED);

        try {
            sut.getConnection(ConnectionIntent.READ, 1);
            fail("Missing RedisException");
        } catch (RedisException e) {
            assertThat(e).isInstanceOf(RedisConnectionException.class)
                    .hasRootCauseExactlyInstanceOf(IllegalStateException.class);
        }

        verify(clientMock).connectToNodeAsync(eq(StringCodec.UTF8), eq("localhost:1"), any(), any());
        verify(clientMock).connectToNodeAsync(eq(StringCodec.UTF8), eq("localhost:2"), any(), any());
    }

    @Test
    void shouldNotifyListerOnUncoveredWriteSlot() {

        partitions.clear();

        sut.getConnectionAsync(ConnectionIntent.WRITE, 2);

        verify(clusterEventListener).onUncoveredSlot(2);
    }

    @Test
    void shouldNotifyListerOnUncoveredReadSlot() {

        partitions.clear();

        sut.getConnectionAsync(ConnectionIntent.WRITE, 2);

        verify(clusterEventListener).onUncoveredSlot(2);
    }

    @Test
    void shouldNotifyListerOnUncoveredReadSlotAfterSelection() {

        sut.setReadFrom(new ReadFrom() {

            @Override
            public List<RedisNodeDescription> select(Nodes nodes) {
                return Collections.emptyList();
            }

        });

        sut.getConnectionAsync(ConnectionIntent.READ, 2);

        verify(clusterEventListener).onUncoveredSlot(2);
    }

    @Test
    void shouldCloseConnections() {

        when(channelHandlerMock.closeAsync()).thenReturn(CompletableFuture.completedFuture(null));

        when(clientMock.connectToNodeAsync(eq(StringCodec.UTF8), eq("localhost:1"), any(), any()))
                .thenReturn(ConnectionFuture.from(socketAddressMock, CompletableFuture.completedFuture(nodeConnectionMock)));

        StatefulRedisConnection<String, String> connection = sut.getConnection(ConnectionIntent.READ, 1);
        assertThat(connection).isNotNull();

        sut.close();

        verify(channelHandlerMock).closeAsync();
    }

    @Test
    void shouldRejectConnectionsToUnknownNodeId() {

        assertThatThrownBy(() -> sut.getConnection(ConnectionIntent.READ, "foobar"))
                .isInstanceOf(UnknownPartitionException.class);

        verify(clusterEventListener).onUnknownNode();
    }

    @Test
    void shouldRejectConnectionsToUnknownNodeHostAndPort() {

        assertThatThrownBy(() -> sut.getConnection(ConnectionIntent.READ, "localhost", 1234))
                .isInstanceOf(UnknownPartitionException.class);

        verify(clusterEventListener).onUnknownNode();
    }

}
