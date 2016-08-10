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
import org.mockito.runners.MockitoJUnitRunner;

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
    private RedisClusterClient clientMock;

    @Mock
    private RedisChannelWriter writerMock;

    @Mock
    StatefulRedisConnection<String, String> nodeConnectionMock;

    @Mock
    RedisCommands<String, String> commandsMock;

    private Partitions partitions = new Partitions();

    @Before
    public void before() throws Exception {

        sut = new PooledClusterConnectionProvider<>(clientMock, writerMock, CODEC);

        List<Integer> slots1 = IntStream.range(0, 8192).boxed().collect(Collectors.toList());
        List<Integer> slots2 = IntStream.range(8192, SlotHash.SLOT_COUNT).boxed().collect(Collectors.toList());

        partitions.add(new RedisClusterNode(RedisURI.create("localhost", 1), "1", true, null, 0, 0, 0, slots1,
                Collections.singleton(RedisClusterNode.NodeFlag.MASTER)));
        partitions.add(new RedisClusterNode(RedisURI.create("localhost", 2), "2", true, "1", 0, 0, 0, slots2,
                Collections.singleton(RedisClusterNode.NodeFlag.SLAVE)));

        sut.setPartitions(partitions);

        when(nodeConnectionMock.sync()).thenReturn(commandsMock);
    }

    @Test
    public void shouldObtainConnection() throws Exception {

        when(clientMock.connectToNode(eq(CODEC), eq("localhost:1"), any(), any())).thenReturn(nodeConnectionMock);

        StatefulRedisConnection<String, String> connection = sut.getConnection(Intent.READ, 1);

        assertThat(connection).isSameAs(nodeConnectionMock);
        verify(connection).setAutoFlushCommands(true);
        verifyNoMoreInteractions(connection);
    }

    @Test
    public void shouldObtainConnectionReadFromSlave() throws Exception {

        when(clientMock.connectToNode(eq(CODEC), eq("localhost:2"), any(), any())).thenReturn(nodeConnectionMock);

        sut.setReadFrom(ReadFrom.SLAVE);

        StatefulRedisConnection<String, String> connection = sut.getConnection(Intent.READ, 1);

        assertThat(connection).isSameAs(nodeConnectionMock);
        verify(connection).sync();
        verify(commandsMock).readOnly();
        verify(connection).setAutoFlushCommands(true);
    }

    @Test
    public void shouldCloseConnectionOnConnectFailure() throws Exception {

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
    public void shouldRetryConnectionAttemptAfterConnectionAttemptWasBroken() throws Exception {

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
}