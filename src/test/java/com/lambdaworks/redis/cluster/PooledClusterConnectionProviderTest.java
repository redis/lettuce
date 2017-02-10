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
package com.lambdaworks.redis.cluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.lambdaworks.redis.ReadFrom;
import com.lambdaworks.redis.RedisChannelWriter;
import com.lambdaworks.redis.RedisException;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.cluster.ClusterConnectionProvider.Intent;
import com.lambdaworks.redis.cluster.models.partitions.Partitions;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;
import com.lambdaworks.redis.codec.Utf8StringCodec;

/**
 * @author Mark Paluch
 */
@RunWith(MockitoJUnitRunner.class)
public class PooledClusterConnectionProviderTest {

    public static final Utf8StringCodec CODEC = new Utf8StringCodec();

    private PooledClusterConnectionProvider<String, String> sut;

    @Mock
    RedisClusterClient clientMock;

    @Mock
    RedisChannelWriter writerMock;

    @Mock
    StatefulRedisConnection<String, String> nodeConnectionMock;

    @Mock
    RedisCommands<String, String> commandsMock;

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

        when(nodeConnectionMock.sync()).thenReturn(commandsMock);
    }

    @Test
    public void shouldObtainConnection() {

        when(clientMock.connectToNode(eq(CODEC), eq("localhost:1"), any(), any())).thenReturn(nodeConnectionMock);

        StatefulRedisConnection<String, String> connection = sut.getConnection(Intent.READ, 1);

        assertThat(connection).isSameAs(nodeConnectionMock);
        verify(connection).setAutoFlushCommands(true);
        verifyNoMoreInteractions(connection);
    }

    @Test
    public void shouldObtainConnectionReadFromSlave() {

        when(clientMock.connectToNode(eq(CODEC), eq("localhost:2"), any(), any())).thenReturn(nodeConnectionMock);

        sut.setReadFrom(ReadFrom.SLAVE);

        StatefulRedisConnection<String, String> connection = sut.getConnection(Intent.READ, 1);

        assertThat(connection).isSameAs(nodeConnectionMock);
        verify(connection).sync();
        verify(commandsMock).readOnly();
        verify(connection).setAutoFlushCommands(true);
    }

    @Test
    public void shouldCloseConnectionOnConnectFailure() {

        when(clientMock.connectToNode(eq(CODEC), eq("localhost:2"), any(), any())).thenReturn(nodeConnectionMock);
        doThrow(new RuntimeException()).when(commandsMock).readOnly();

        sut.setReadFrom(ReadFrom.SLAVE);

        try {
            sut.getConnection(Intent.READ, 1);
            fail("Missing RedisException");
        } catch (RedisException e) {
            assertThat(e).hasRootCauseInstanceOf(RuntimeException.class);
        }

        verify(nodeConnectionMock).close();
        verify(clientMock).connectToNode(eq(CODEC), eq("localhost:2"), any(), any());
    }

    @Test
    public void shouldRetryConnectionAttemptAfterConnectionAttemptWasBroken() {

        when(clientMock.connectToNode(eq(CODEC), eq("localhost:2"), any(), any())).thenReturn(nodeConnectionMock);
        doThrow(new RuntimeException()).when(commandsMock).readOnly();

        sut.setReadFrom(ReadFrom.SLAVE);

        try {
            sut.getConnection(Intent.READ, 1);
            fail("Missing RedisException");
        } catch (RedisException e) {
            assertThat(e).hasRootCauseInstanceOf(RuntimeException.class);
        }
        verify(nodeConnectionMock).close();

        doReturn("OK").when(commandsMock).readOnly();

        sut.getConnection(Intent.READ, 1);

        verify(clientMock, times(2)).connectToNode(eq(CODEC), eq("localhost:2"), any(), any());
    }

    @Test
    public void shouldCloseConnections() {

        when(clientMock.connectToNode(eq(CODEC), eq("localhost:1"), any(), any())).thenReturn(nodeConnectionMock);

        StatefulRedisConnection<String, String> connection = sut.getConnection(Intent.READ, 1);
        assertThat(connection).isNotNull();

        sut.close();

        verify(nodeConnectionMock).close();
    }

}
