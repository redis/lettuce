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
package com.lambdaworks.redis.cluster.topology;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.lambdaworks.redis.RedisException;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.api.async.RedisAsyncCommands;
import com.lambdaworks.redis.cluster.RedisClusterClient;
import com.lambdaworks.redis.cluster.models.partitions.Partitions;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;
import com.lambdaworks.redis.codec.RedisCodec;
import com.lambdaworks.redis.protocol.CommandType;
import com.lambdaworks.redis.protocol.RedisCommand;
import com.lambdaworks.redis.resource.ClientResources;
import com.lambdaworks.redis.resource.DnsResolvers;

import io.netty.util.concurrent.ImmediateEventExecutor;

/**
 * @author Mark Paluch
 */
@RunWith(MockitoJUnitRunner.class)
public class ClusterTopologyRefreshTest {

    public final static long COMMAND_TIMEOUT_NS = TimeUnit.MILLISECONDS.toNanos(10);

    public static final String NODE_1_VIEW = "1 127.0.0.1:7380 master,myself - 0 1401258245007 2 disconnected 8000-11999\n"
            + "2 127.0.0.1:7381 master - 111 1401258245007 222 connected 7000 12000 12002-16383\n";
    public static final String NODE_2_VIEW = "1 127.0.0.1:7380 master - 0 1401258245007 2 disconnected 8000-11999\n"
            + "2 127.0.0.1:7381 master,myself - 111 1401258245007 222 connected 7000 12000 12002-16383\n";

    private ClusterTopologyRefresh sut;

    @Mock
    private RedisClusterClient client;

    @Mock
    private StatefulRedisConnection<String, String> connection;

    @Mock
    private ClientResources clientResources;

    @Mock
    private NodeConnectionFactory nodeConnectionFactory;

    @Mock
    private StatefulRedisConnection<String, String> connection1;

    @Mock
    private RedisAsyncCommands<String, String> asyncCommands1;

    @Mock
    private StatefulRedisConnection<String, String> connection2;

    @Mock
    private RedisAsyncCommands<String, String> asyncCommands2;

    @Before
    public void before() throws Exception {

        when(clientResources.dnsResolver()).thenReturn(DnsResolvers.JVM_DEFAULT);
        when(clientResources.eventExecutorGroup()).thenReturn(ImmediateEventExecutor.INSTANCE);
        when(connection1.async()).thenReturn(asyncCommands1);
        when(connection2.async()).thenReturn(asyncCommands2);

        when(connection1.dispatch(any(RedisCommand.class))).thenAnswer(invocation -> {

            TimedAsyncCommand command = (TimedAsyncCommand) invocation.getArguments()[0];
            if (command.getType() == CommandType.CLUSTER) {
                command.getOutput().set(ByteBuffer.wrap(NODE_1_VIEW.getBytes()));
                command.complete();
            }

            if (command.getType() == CommandType.CLIENT) {
                command.getOutput().set(ByteBuffer.wrap("c1\nc2\n".getBytes()));
                command.complete();
            }

            command.encodedAtNs = 10;
            command.completedAtNs = 50;

            return command;
        });

        when(connection2.dispatch(any(RedisCommand.class))).thenAnswer(invocation -> {

            TimedAsyncCommand command = (TimedAsyncCommand) invocation.getArguments()[0];
            if (command.getType() == CommandType.CLUSTER) {
                command.getOutput().set(ByteBuffer.wrap(NODE_2_VIEW.getBytes()));
                command.complete();
            }

            if (command.getType() == CommandType.CLIENT) {
                command.getOutput().set(ByteBuffer.wrap("".getBytes()));
                command.complete();
            }

            command.encodedAtNs = 10;
            command.completedAtNs = 20;

            return command;
        });

        sut = new ClusterTopologyRefresh(nodeConnectionFactory, clientResources);
    }

    @Test
    public void getNodeSpecificViewsNode1IsFasterThanNode2() throws Exception {

        Requests requests = createClusterNodesRequests(1, NODE_1_VIEW);
        requests = createClusterNodesRequests(2, NODE_2_VIEW).mergeWith(requests);

        Requests clientRequests = createClientListRequests(1, "c1\nc2\n").mergeWith(createClientListRequests(2, "c1\nc2\n"));

        NodeTopologyViews nodeSpecificViews = sut.getNodeSpecificViews(requests, clientRequests, COMMAND_TIMEOUT_NS);

        Collection<Partitions> values = nodeSpecificViews.toMap().values();

        assertThat(values).hasSize(2);

        for (Partitions value : values) {
            assertThat(value).extracting("nodeId").containsExactly("1", "2");
        }
    }

    @Test
    public void getNodeSpecificViewTestingNoAddrFilter() throws Exception {

        String nodes1 = "n1 10.37.110.63:7000 slave n3 0 1452553664848 43 connected\n"
                + "n2 10.37.110.68:7000 slave n6 0 1452553664346 45 connected\n"
                + "badSlave :0 slave,fail,noaddr n5 1449160058028 1449160053146 46 disconnected\n"
                + "n3 10.37.110.69:7000 master - 0 1452553662842 43 connected 3829-6787 7997-9999\n"
                + "n4 10.37.110.62:7000 slave n3 0 1452553663844 43 connected\n"
                + "n5 10.37.110.70:7000 myself,master - 0 0 46 connected 10039-14999\n"
                + "n6 10.37.110.65:7000 master - 0 1452553663844 45 connected 0-3828 6788-7996 10000-10038 15000-16383";

        Requests clusterNodesRequests = createClusterNodesRequests(1, nodes1);
        Requests clientRequests = createClientListRequests(1, "c1\nc2\n");

        NodeTopologyViews nodeSpecificViews = sut
                .getNodeSpecificViews(clusterNodesRequests, clientRequests, COMMAND_TIMEOUT_NS);

        List<Partitions> values = new ArrayList<>(nodeSpecificViews.toMap().values());

        assertThat(values).hasSize(1);

        for (Partitions value : values) {
            assertThat(value).extracting("nodeId").containsOnly("n1", "n2", "n3", "n4", "n5", "n6");
        }

        RedisClusterNodeSnapshot firstPartition = (RedisClusterNodeSnapshot) values.get(0).getPartition(0);
        RedisClusterNodeSnapshot selfPartition = (RedisClusterNodeSnapshot) values.get(0).getPartition(4);
        assertThat(firstPartition.getConnectedClients()).isEqualTo(2);
        assertThat(selfPartition.getConnectedClients()).isNull();

    }

    @Test
    public void getNodeSpecificViewsNode2IsFasterThanNode1() throws Exception {

        Requests clusterNodesRequests = createClusterNodesRequests(5, NODE_1_VIEW);
        clusterNodesRequests = createClusterNodesRequests(1, NODE_2_VIEW).mergeWith(clusterNodesRequests);

        Requests clientRequests = createClientListRequests(5, "c1\nc2\n").mergeWith(createClientListRequests(1, "c1\nc2\n"));

        NodeTopologyViews nodeSpecificViews = sut
                .getNodeSpecificViews(clusterNodesRequests, clientRequests, COMMAND_TIMEOUT_NS);
        List<Partitions> values = new ArrayList<>(nodeSpecificViews.toMap().values());

        assertThat(values).hasSize(2);

        for (Partitions value : values) {
            assertThat(value).extracting("nodeId").containsExactly("2", "1");
        }
    }

    @Test
    public void shouldAttemptToConnectOnlyOnce() throws Exception {

        List<RedisURI> seed = Arrays.asList(RedisURI.create("127.0.0.1", 7380), RedisURI.create("127.0.0.1", 7381));

        when(nodeConnectionFactory.connectToNodeAsync(any(RedisCodec.class), eq(new InetSocketAddress("127.0.0.1", 7380))))
                .thenReturn(completedFuture((StatefulRedisConnection) connection1));
        when(nodeConnectionFactory.connectToNodeAsync(any(RedisCodec.class), eq(new InetSocketAddress("127.0.0.1", 7381))))
                .thenReturn(completedWithException(new RedisException("connection failed")));

        sut.loadViews(seed, true);

        verify(nodeConnectionFactory).connectToNodeAsync(any(RedisCodec.class), eq(new InetSocketAddress("127.0.0.1", 7380)));
        verify(nodeConnectionFactory).connectToNodeAsync(any(RedisCodec.class), eq(new InetSocketAddress("127.0.0.1", 7381)));
    }

    @Test
    public void shouldFailIfNoNodeConnects() throws Exception {

        List<RedisURI> seed = Arrays.asList(RedisURI.create("127.0.0.1", 7380), RedisURI.create("127.0.0.1", 7381));

        when(nodeConnectionFactory.connectToNodeAsync(any(RedisCodec.class), eq(new InetSocketAddress("127.0.0.1", 7380))))
                .thenReturn(completedWithException(new RedisException("connection failed")));
        when(nodeConnectionFactory.connectToNodeAsync(any(RedisCodec.class), eq(new InetSocketAddress("127.0.0.1", 7381))))
                .thenReturn(completedWithException(new RedisException("connection failed")));

        try {
            sut.loadViews(seed, true);
            fail("Missing RedisConnectionException");
        } catch (Exception e) {
            assertThat(e).hasNoCause().hasMessage("Unable to establish a connection to Redis Cluster");
            assertThat(e.getSuppressed()).hasSize(2);
        }

        verify(nodeConnectionFactory).connectToNodeAsync(any(RedisCodec.class), eq(new InetSocketAddress("127.0.0.1", 7380)));
        verify(nodeConnectionFactory).connectToNodeAsync(any(RedisCodec.class), eq(new InetSocketAddress("127.0.0.1", 7381)));
    }

    @Test
    public void shouldShouldDiscoverNodes() throws Exception {

        List<RedisURI> seed = Arrays.asList(RedisURI.create("127.0.0.1", 7380));

        when(nodeConnectionFactory.connectToNodeAsync(any(RedisCodec.class), eq(new InetSocketAddress("127.0.0.1", 7380))))
                .thenReturn(completedFuture((StatefulRedisConnection) connection1));
        when(nodeConnectionFactory.connectToNodeAsync(any(RedisCodec.class), eq(new InetSocketAddress("127.0.0.1", 7381))))
                .thenReturn(completedFuture((StatefulRedisConnection) connection2));

        sut.loadViews(seed, true);

        verify(nodeConnectionFactory).connectToNodeAsync(any(RedisCodec.class), eq(new InetSocketAddress("127.0.0.1", 7380)));
        verify(nodeConnectionFactory).connectToNodeAsync(any(RedisCodec.class), eq(new InetSocketAddress("127.0.0.1", 7381)));
    }

    @Test
    public void shouldShouldNotDiscoverNodes() throws Exception {

        List<RedisURI> seed = Arrays.asList(RedisURI.create("127.0.0.1", 7380));

        when(nodeConnectionFactory.connectToNodeAsync(any(RedisCodec.class), eq(new InetSocketAddress("127.0.0.1", 7380))))
                .thenReturn(completedFuture((StatefulRedisConnection) connection1));

        sut.loadViews(seed, false);

        verify(nodeConnectionFactory).connectToNodeAsync(any(RedisCodec.class), eq(new InetSocketAddress("127.0.0.1", 7380)));
        verifyNoMoreInteractions(nodeConnectionFactory);
    }

    @Test
    public void shouldNotFailOnDuplicateSeedNodes() throws Exception {

        List<RedisURI> seed = Arrays.asList(RedisURI.create("127.0.0.1", 7380), RedisURI.create("127.0.0.1", 7381),
                RedisURI.create("127.0.0.1", 7381));

        when(nodeConnectionFactory.connectToNodeAsync(any(RedisCodec.class), eq(new InetSocketAddress("127.0.0.1", 7380))))
                .thenReturn(completedFuture((StatefulRedisConnection) connection1));

        when(nodeConnectionFactory.connectToNodeAsync(any(RedisCodec.class), eq(new InetSocketAddress("127.0.0.1", 7381))))
                .thenReturn(completedFuture((StatefulRedisConnection) connection2));

        sut.loadViews(seed, true);

        verify(nodeConnectionFactory).connectToNodeAsync(any(RedisCodec.class), eq(new InetSocketAddress("127.0.0.1", 7380)));
        verify(nodeConnectionFactory).connectToNodeAsync(any(RedisCodec.class), eq(new InetSocketAddress("127.0.0.1", 7381)));
    }

    @Test
    public void undiscoveredAdditionalNodesShouldBeLastUsingClientCount() throws Exception {

        List<RedisURI> seed = Arrays.asList(RedisURI.create("127.0.0.1", 7380));

        when(nodeConnectionFactory.connectToNodeAsync(any(RedisCodec.class), eq(new InetSocketAddress("127.0.0.1", 7380))))
                .thenReturn(completedFuture((StatefulRedisConnection) connection1));

        Map<RedisURI, Partitions> partitionsMap = sut.loadViews(seed, false);

        Partitions partitions = partitionsMap.values().iterator().next();

        List<RedisClusterNode> nodes = TopologyComparators.sortByClientCount(partitions);

        assertThat(nodes).hasSize(2).extracting(RedisClusterNode::getUri)
                .containsSequence(seed.get(0), RedisURI.create("127.0.0.1", 7381));
    }

    @Test
    public void discoveredAdditionalNodesShouldBeOrderedUsingClientCount() throws Exception {

        List<RedisURI> seed = Arrays.asList(RedisURI.create("127.0.0.1", 7380));

        when(nodeConnectionFactory.connectToNodeAsync(any(RedisCodec.class), eq(new InetSocketAddress("127.0.0.1", 7380))))
                .thenReturn(completedFuture((StatefulRedisConnection) connection1));
        when(nodeConnectionFactory.connectToNodeAsync(any(RedisCodec.class), eq(new InetSocketAddress("127.0.0.1", 7381))))
                .thenReturn(completedFuture((StatefulRedisConnection) connection2));

        Map<RedisURI, Partitions> partitionsMap = sut.loadViews(seed, true);

        Partitions partitions = partitionsMap.values().iterator().next();

        List<RedisClusterNode> nodes = TopologyComparators.sortByClientCount(partitions);

        assertThat(nodes).hasSize(2).extracting(RedisClusterNode::getUri)
                .containsSequence(RedisURI.create("127.0.0.1", 7381), seed.get(0));
    }

    @Test
    public void undiscoveredAdditionalNodesShouldBeLastUsingLatency() throws Exception {

        List<RedisURI> seed = Arrays.asList(RedisURI.create("127.0.0.1", 7380));

        when(nodeConnectionFactory.connectToNodeAsync(any(RedisCodec.class), eq(new InetSocketAddress("127.0.0.1", 7380))))
                .thenReturn(completedFuture((StatefulRedisConnection) connection1));

        Map<RedisURI, Partitions> partitionsMap = sut.loadViews(seed, false);

        Partitions partitions = partitionsMap.values().iterator().next();

        List<RedisClusterNode> nodes = TopologyComparators.sortByLatency(partitions);

        assertThat(nodes).hasSize(2).extracting(RedisClusterNode::getUri)
                .containsSequence(seed.get(0), RedisURI.create("127.0.0.1", 7381));
    }

    @Test
    public void discoveredAdditionalNodesShouldBeOrderedUsingLatency() throws Exception {

        List<RedisURI> seed = Arrays.asList(RedisURI.create("127.0.0.1", 7380));

        when(nodeConnectionFactory.connectToNodeAsync(any(RedisCodec.class), eq(new InetSocketAddress("127.0.0.1", 7380))))
                .thenReturn(completedFuture((StatefulRedisConnection) connection1));
        when(nodeConnectionFactory.connectToNodeAsync(any(RedisCodec.class), eq(new InetSocketAddress("127.0.0.1", 7381))))
                .thenReturn(completedFuture((StatefulRedisConnection) connection2));

        Map<RedisURI, Partitions> partitionsMap = sut.loadViews(seed, true);

        Partitions partitions = partitionsMap.values().iterator().next();

        List<RedisClusterNode> nodes = TopologyComparators.sortByLatency(partitions);

        assertThat(nodes).hasSize(2).extracting(RedisClusterNode::getUri)
                .containsSequence(RedisURI.create("127.0.0.1", 7381), seed.get(0));
    }

    protected Requests createClusterNodesRequests(int duration, String nodes) {

        RedisURI redisURI = RedisURI.create("redis://localhost:" + duration);
        Connections connections = new Connections();
        connections.addConnection(redisURI, connection);

        Requests requests = connections.requestTopology();
        TimedAsyncCommand<String, String, String> command = requests.getRequest(redisURI);

        command.getOutput().set(ByteBuffer.wrap(nodes.getBytes()));
        command.complete();
        command.encodedAtNs = 0;
        command.completedAtNs = duration;

        return requests;
    }

    protected Requests createClientListRequests(int duration, String response) {

        RedisURI redisURI = RedisURI.create("redis://localhost:" + duration);
        Connections connections = new Connections();
        connections.addConnection(redisURI, connection);

        Requests requests = connections.requestTopology();
        TimedAsyncCommand<String, String, String> command = requests.getRequest(redisURI);

        command.getOutput().set(ByteBuffer.wrap(response.getBytes()));
        command.complete();

        return requests;
    }

    private static <T> CompletableFuture<T> completedWithException(Exception e) {

        CompletableFuture<T> future = new CompletableFuture<T>();
        future.completeExceptionally(e);
        return future;
    }
}
