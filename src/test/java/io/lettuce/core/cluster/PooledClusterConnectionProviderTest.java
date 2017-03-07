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
package io.lettuce.core.cluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.net.SocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import io.lettuce.Futures;
import io.lettuce.core.ReadFrom;
import io.lettuce.core.RedisChannelWriter;
import io.lettuce.core.RedisException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.cluster.ClusterConnectionProvider.Intent;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.codec.Utf8StringCodec;
import io.lettuce.core.protocol.AsyncCommand;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.resource.ClientResources;

/**
 * @author Mark Paluch
 */
@RunWith(MockitoJUnitRunner.class)
public class PooledClusterConnectionProviderTest {

    public static final Utf8StringCodec CODEC = new Utf8StringCodec();

    private PooledClusterConnectionProvider<String, String> sut;

    @Mock
    SocketAddress socketAddressMock;

    @Mock
    RedisClusterClient clientMock;

    @Mock
    RedisChannelWriter writerMock;

    @Mock
    StatefulRedisConnection<String, String> nodeConnectionMock;

    @Mock
    RedisCommands<String, String> commandsMock;

    @Mock
    RedisAsyncCommands<String, String> asyncCommandsMock;

    @Mock
    ClientResources clientResourcesMock;

    Partitions partitions = new Partitions();

    @Before
    public void before() {

        sut = new PooledClusterConnectionProvider<>(clientMock, writerMock, CODEC);

        List<Integer> slots1 = IntStream.range(0, 8192).boxed().collect(Collectors.toList());
        List<Integer> slots2 = IntStream.range(8192, SlotHash.SLOT_COUNT).boxed().collect(Collectors.toList());

        partitions.add(new RedisClusterNode(RedisURI.create("localhost", 1), "1", true, null, 0, 0, 0, slots1, Collections
                .singleton(RedisClusterNode.NodeFlag.MASTER)));
        partitions.add(new RedisClusterNode(RedisURI.create("localhost", 2), "2", true, "1", 0, 0, 0, slots2, Collections
                .singleton(RedisClusterNode.NodeFlag.SLAVE)));

        sut.setPartitions(partitions);

        when(nodeConnectionMock.async()).thenReturn(asyncCommandsMock);
    }

    @Test
    public void shouldObtainConnection() {

        when(clientMock.connectToNodeAsync(eq(CODEC), eq("localhost:1"), any(), any())).thenReturn(
                Futures.createConnectionFuture(socketAddressMock, CompletableFuture.completedFuture(nodeConnectionMock)));

        StatefulRedisConnection<String, String> connection = sut.getConnection(Intent.READ, 1);

        assertThat(connection).isSameAs(nodeConnectionMock);
        verify(connection).setAutoFlushCommands(true);
        verifyNoMoreInteractions(connection);
    }

    @Test
    public void shouldObtainConnectionReadFromSlave() {

        when(clientMock.connectToNodeAsync(eq(CODEC), eq("localhost:2"), any(), any())).thenReturn(
                Futures.createConnectionFuture(socketAddressMock, CompletableFuture.completedFuture(nodeConnectionMock)));

        AsyncCommand<String, String, String> async = new AsyncCommand<>(new Command<String, String, String>(
                CommandType.READONLY, null, null));
        async.complete();

        when(asyncCommandsMock.readOnly()).thenReturn(async);

        sut.setReadFrom(ReadFrom.SLAVE);

        StatefulRedisConnection<String, String> connection = sut.getConnection(Intent.READ, 1);

        assertThat(connection).isSameAs(nodeConnectionMock);
        verify(connection).async();
        verify(asyncCommandsMock).readOnly();
        verify(connection).setAutoFlushCommands(true);
    }

    @Test
    public void shouldCloseConnectionOnConnectFailure() {

        when(clientMock.connectToNodeAsync(eq(CODEC), eq("localhost:2"), any(), any())).thenReturn(
                Futures.createConnectionFuture(socketAddressMock, CompletableFuture.completedFuture(nodeConnectionMock)));

        AsyncCommand<String, String, String> async = new AsyncCommand<>(new Command<String, String, String>(
                CommandType.READONLY, null, null));
        async.completeExceptionally(new RuntimeException());

        when(asyncCommandsMock.readOnly()).thenReturn(async);

        sut.setReadFrom(ReadFrom.SLAVE);

        try {
            sut.getConnection(Intent.READ, 1);
            fail("Missing RedisException");
        } catch (RedisException e) {
            assertThat(e).hasRootCauseInstanceOf(RuntimeException.class);
        }

        verify(nodeConnectionMock).close();
        verify(clientMock).connectToNodeAsync(eq(CODEC), eq("localhost:2"), any(), any());
    }

    @Test
    public void shouldRetryConnectionAttemptAfterConnectionAttemptWasBroken() {

        when(clientMock.connectToNodeAsync(eq(CODEC), eq("localhost:2"), any(), any())).thenReturn(
                Futures.createConnectionFuture(socketAddressMock, CompletableFuture.completedFuture(nodeConnectionMock)));

        AsyncCommand<String, String, String> async = new AsyncCommand<>(new Command<String, String, String>(
                CommandType.READONLY, null, null));
        async.completeExceptionally(new RuntimeException());

        when(asyncCommandsMock.readOnly()).thenReturn(async);

        sut.setReadFrom(ReadFrom.SLAVE);

        try {
            sut.getConnection(Intent.READ, 1);
            fail("Missing RedisException");
        } catch (RedisException e) {
            assertThat(e).hasRootCauseInstanceOf(RuntimeException.class);
        }
        verify(nodeConnectionMock).close();

        async = new AsyncCommand<>(new Command<String, String, String>(CommandType.READONLY, null, null));
        async.complete();

        when(asyncCommandsMock.readOnly()).thenReturn(async);

        sut.getConnection(Intent.READ, 1);

        verify(clientMock, times(2)).connectToNodeAsync(eq(CODEC), eq("localhost:2"), any(), any());
    }

    @Test
    public void shouldCloseConnections() {

        when(clientMock.connectToNodeAsync(eq(CODEC), eq("localhost:1"), any(), any())).thenReturn(
                Futures.createConnectionFuture(socketAddressMock, CompletableFuture.completedFuture(nodeConnectionMock)));

        StatefulRedisConnection<String, String> connection = sut.getConnection(Intent.READ, 1);
        assertThat(connection).isNotNull();

        sut.close();

        verify(nodeConnectionMock).close();
    }

}
