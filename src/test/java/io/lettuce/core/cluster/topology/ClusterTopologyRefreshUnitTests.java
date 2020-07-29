/*
 * Copyright 2011-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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
package io.lettuce.core.cluster.topology;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import io.lettuce.core.ConnectionFuture;
import io.lettuce.core.RedisException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.protocol.RedisCommand;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DnsResolver;
import io.lettuce.core.resource.SocketAddressResolver;
import io.lettuce.test.settings.TestSettings;
import io.netty.util.Timeout;
import io.netty.util.concurrent.EventExecutorGroup;

/**
 * Unit tests for {@link ClusterTopologyRefresh}.
 * 
 * @author Mark Paluch
 * @author Christian Weitendorf
 */
@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClusterTopologyRefreshUnitTests {

    private static final String NODE_1_VIEW = "1 127.0.0.1:7380 master,myself - 0 1401258245007 2 disconnected 8000-11999\n"
            + "2 127.0.0.1:7381 master - 111 1401258245007 222 connected 7000 12000 12002-16383\n";
    private static final String NODE_2_VIEW = "1 127.0.0.1:7380 master - 0 1401258245007 2 disconnected 8000-11999\n"
            + "2 127.0.0.1:7381 master,myself - 111 1401258245007 222 connected 7000 12000 12002-16383\n";

    private DefaultClusterTopologyRefresh sut;

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

    @Mock
    private EventExecutorGroup eventExecutors;

    @BeforeEach
    void before() {

        io.netty.util.Timer timer = mock(io.netty.util.Timer.class);
        when(timer.newTimeout(any(), anyLong(), any())).thenReturn(mock(Timeout.class));
        when(clientResources.timer()).thenReturn(timer);
        when(clientResources.socketAddressResolver()).thenReturn(SocketAddressResolver.create(DnsResolver.unresolved()));
        when(clientResources.eventExecutorGroup()).thenReturn(eventExecutors);
        when(connection1.async()).thenReturn(asyncCommands1);
        when(connection2.async()).thenReturn(asyncCommands2);
        when(connection1.closeAsync()).thenReturn(CompletableFuture.completedFuture(null));
        when(connection2.closeAsync()).thenReturn(CompletableFuture.completedFuture(null));

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

            if (command.getType() == CommandType.INFO) {
                command.getOutput().set(ByteBuffer.wrap(
                        "# Clients\nconnected_clients:2\nclient_longest_output_list:0\nclient_biggest_input_buf:0\nblocked_clients:0"
                                .getBytes()));
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

            if (command.getType() == CommandType.INFO) {
                command.getOutput().set(ByteBuffer.wrap(
                        "# Clients\nconnected_clients:2\nclient_longest_output_list:0\nclient_biggest_input_buf:0\nblocked_clients:0"
                                .getBytes()));
                command.complete();
            }

            command.encodedAtNs = 10;
            command.completedAtNs = 20;

            return command;
        });

        sut = new DefaultClusterTopologyRefresh(nodeConnectionFactory, clientResources);
    }

    @Test
    void getNodeTopologyView() throws Exception {
        Requests requestedTopology = createClusterNodesRequests(1, NODE_1_VIEW);
        Requests requestedClients = createClientListRequests(1,
                "# Clients\r\nconnected_clients:2438\r\nclient_longest_output_list:0\r\nclient_biggest_input_buf:0\r\nblocked_clients:0");
        RedisURI redisURI = RedisURI.create("redis://localhost:1");
        NodeTopologyView nodeTopologyView = NodeTopologyView.from(redisURI, requestedTopology, requestedClients);
        assertThat(nodeTopologyView.getConnectedClients()).isEqualTo(2438);
    }

    @Test
    void getNodeSpecificViewsNode1IsFasterThanNode2() throws Exception {

        Requests requests = createClusterNodesRequests(1, NODE_1_VIEW);
        requests = createClusterNodesRequests(2, NODE_2_VIEW).mergeWith(requests);

        Requests clientRequests = createClientListRequests(1, "c1\nc2\n").mergeWith(createClientListRequests(2, "c1\nc2\n"));

        NodeTopologyViews nodeSpecificViews = sut.getNodeSpecificViews(requests, clientRequests);

        Collection<Partitions> values = nodeSpecificViews.toMap().values();

        assertThat(values).hasSize(2);

        for (Partitions value : values) {
            assertThat(value).extracting("nodeId").containsSequence("1", "2");
        }
    }

    @Test
    void shouldNotRequestTopologyIfExecutorShutsDown() {

        when(eventExecutors.isShuttingDown()).thenReturn(true);

        List<RedisURI> seed = Arrays.asList(RedisURI.create("127.0.0.1", 7380), RedisURI.create("127.0.0.1", 7381));

        sut.loadViews(seed, Duration.ofSeconds(1), true);

        verifyZeroInteractions(nodeConnectionFactory);
    }

    @Test
    void partitionsReturnedAsReported() throws Exception {

        System.setProperty("io.lettuce.core.topology.sort", "none");

        String NODE_1_VIEW = "2 127.0.0.1:7381 master - 111 1401258245007 222 connected 7000 12000 12002-16383\n"
                + "1 127.0.0.1:7380 master,myself - 0 1401258245007 2 disconnected 8000-11999\n";
        String NODE_2_VIEW = "2 127.0.0.1:7381 master,myself - 111 1401258245007 222 connected 7000 12000 12002-16383\n"
                + "1 127.0.0.1:7380 master - 0 1401258245007 2 disconnected 8000-11999\n";

        Requests requests = createClusterNodesRequests(1, NODE_1_VIEW);
        requests = createClusterNodesRequests(2, NODE_2_VIEW).mergeWith(requests);

        Requests clientRequests = createClientListRequests(1, "c1\nc2\n").mergeWith(createClientListRequests(2, "c1\nc2\n"));

        NodeTopologyViews nodeSpecificViews = sut.getNodeSpecificViews(requests, clientRequests);

        Collection<Partitions> values = nodeSpecificViews.toMap().values();

        assertThat(values).hasSize(2);

        for (Partitions value : values) {
            assertThat(value).extracting("nodeId").containsSequence("2", "1");
        }

        System.getProperties().remove("io.lettuce.core.topology.sort");
    }

    @Test
    void getNodeSpecificViewTestingNoAddrFilter() throws Exception {

        String nodes1 = "n1 10.37.110.63:7000 slave n3 0 1452553664848 43 connected\n"
                + "n2 10.37.110.68:7000 slave n6 0 1452553664346 45 connected\n"
                + "badSlave :0 slave,fail,noaddr n5 1449160058028 1449160053146 46 disconnected\n"
                + "n3 10.37.110.69:7000 master - 0 1452553662842 43 connected 3829-6787 7997-9999\n"
                + "n4 10.37.110.62:7000 slave n3 0 1452553663844 43 connected\n"
                + "n5 10.37.110.70:7000 myself,master - 0 0 46 connected 10039-14999\n"
                + "n6 10.37.110.65:7000 master - 0 1452553663844 45 connected 0-3828 6788-7996 10000-10038 15000-16383";

        Requests clusterNodesRequests = createClusterNodesRequests(1, nodes1);
        Requests clientRequests = createClientListRequests(1,
                "# Clients\r\nconnected_clients:2\r\nclient_longest_output_list:0\r\nclient_biggest_input_buf:0\r\nblocked_clients:0");

        NodeTopologyViews nodeSpecificViews = sut.getNodeSpecificViews(clusterNodesRequests, clientRequests);

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
    void getNodeSpecificViewsNode2IsFasterThanNode1() throws Exception {

        Requests clusterNodesRequests = createClusterNodesRequests(5, NODE_1_VIEW);
        clusterNodesRequests = createClusterNodesRequests(1, NODE_2_VIEW).mergeWith(clusterNodesRequests);

        Requests clientRequests = createClientListRequests(5, "c1\nc2\n").mergeWith(createClientListRequests(1, "c1\nc2\n"));

        NodeTopologyViews nodeSpecificViews = sut.getNodeSpecificViews(clusterNodesRequests, clientRequests);
        List<Partitions> values = new ArrayList<>(nodeSpecificViews.toMap().values());

        assertThat(values).hasSize(2);

        for (Partitions value : values) {
            assertThat(value).extracting("nodeId").containsExactly("2", "1");
        }
    }

    @Test
    void shouldAttemptToConnectOnlyOnce() {

        List<RedisURI> seed = Arrays.asList(RedisURI.create("127.0.0.1", 7380), RedisURI.create("127.0.0.1", 7381));

        when(nodeConnectionFactory.connectToNodeAsync(any(RedisCodec.class),
                eq(InetSocketAddress.createUnresolved("127.0.0.1", 7380))))
                .thenReturn(completedFuture((StatefulRedisConnection) connection1));
        when(nodeConnectionFactory.connectToNodeAsync(any(RedisCodec.class),
                eq(InetSocketAddress.createUnresolved("127.0.0.1", 7381))))
                .thenReturn(completedWithException(new RedisException("connection failed")));

        sut.loadViews(seed, Duration.ofSeconds(1), true);

        verify(nodeConnectionFactory).connectToNodeAsync(any(RedisCodec.class),
                eq(InetSocketAddress.createUnresolved("127.0.0.1", 7380)));
        verify(nodeConnectionFactory).connectToNodeAsync(any(RedisCodec.class),
                eq(InetSocketAddress.createUnresolved("127.0.0.1", 7381)));
    }

    @Test
    void shouldNotConnectToSeedNodeTwice() {

        List<RedisURI> seed = Arrays.asList(RedisURI.create("foobar", 7380));

        when(nodeConnectionFactory.connectToNodeAsync(any(RedisCodec.class),
                eq(InetSocketAddress.createUnresolved("foobar", 7380))))
                        .thenReturn(completedFuture((StatefulRedisConnection) connection1));

        when(nodeConnectionFactory.connectToNodeAsync(any(RedisCodec.class),
                eq(InetSocketAddress.createUnresolved("127.0.0.1", 7380))))
                        .thenReturn(completedFuture((StatefulRedisConnection) connection1));

        when(nodeConnectionFactory.connectToNodeAsync(any(RedisCodec.class),
                eq(InetSocketAddress.createUnresolved("127.0.0.1", 7381))))
                        .thenReturn(completedFuture((StatefulRedisConnection) connection2));

        sut.loadViews(seed, Duration.ofSeconds(1), true);

        verify(nodeConnectionFactory).connectToNodeAsync(any(RedisCodec.class),
                eq(InetSocketAddress.createUnresolved("foobar", 7380)));
        verify(nodeConnectionFactory).connectToNodeAsync(any(RedisCodec.class),
                eq(InetSocketAddress.createUnresolved("127.0.0.1", 7380)));
        verify(nodeConnectionFactory).connectToNodeAsync(any(RedisCodec.class),
                eq(InetSocketAddress.createUnresolved("127.0.0.1", 7381)));
    }

    @Test
    void shouldFailIfNoNodeConnects() {

        List<RedisURI> seed = Arrays.asList(RedisURI.create("127.0.0.1", 7380), RedisURI.create("127.0.0.1", 7381));

        when(nodeConnectionFactory.connectToNodeAsync(any(RedisCodec.class),
                eq(InetSocketAddress.createUnresolved("127.0.0.1", 7380))))
                .thenReturn(completedWithException(new RedisException("connection failed")));
        when(nodeConnectionFactory.connectToNodeAsync(any(RedisCodec.class),
                eq(InetSocketAddress.createUnresolved("127.0.0.1", 7381))))
                .thenReturn(completedWithException(new RedisException("connection failed")));

        try {
            sut.loadViews(seed, Duration.ofSeconds(1), true).toCompletableFuture().join();
            fail("Missing RedisConnectionException");
        } catch (Exception e) {

            assertThat(e.getCause()).hasMessageStartingWith("Cannot retrieve cluster partitions from ");
            assertThat(e.getCause().getSuppressed()).hasSize(2);
        }

        verify(nodeConnectionFactory).connectToNodeAsync(any(RedisCodec.class),
                eq(InetSocketAddress.createUnresolved("127.0.0.1", 7380)));
        verify(nodeConnectionFactory).connectToNodeAsync(any(RedisCodec.class),
                eq(InetSocketAddress.createUnresolved("127.0.0.1", 7381)));
    }

    @Test
    void shouldShouldDiscoverNodes() {

        List<RedisURI> seed = Collections.singletonList(RedisURI.create("127.0.0.1", 7380));

        when(nodeConnectionFactory.connectToNodeAsync(any(RedisCodec.class),
                eq(InetSocketAddress.createUnresolved("127.0.0.1", 7380))))
                .thenReturn(completedFuture((StatefulRedisConnection) connection1));
        when(nodeConnectionFactory.connectToNodeAsync(any(RedisCodec.class),
                eq(InetSocketAddress.createUnresolved("127.0.0.1", 7381))))
                .thenReturn(completedFuture((StatefulRedisConnection) connection2));

        sut.loadViews(seed, Duration.ofSeconds(1), true);

        verify(nodeConnectionFactory).connectToNodeAsync(any(RedisCodec.class),
                eq(InetSocketAddress.createUnresolved("127.0.0.1", 7380)));
        verify(nodeConnectionFactory).connectToNodeAsync(any(RedisCodec.class),
                eq(InetSocketAddress.createUnresolved("127.0.0.1", 7381)));
    }

    @Test
    void shouldShouldNotDiscoverNodes() {

        List<RedisURI> seed = Collections.singletonList(RedisURI.create("127.0.0.1", 7380));

        when(nodeConnectionFactory.connectToNodeAsync(any(RedisCodec.class),
                eq(InetSocketAddress.createUnresolved("127.0.0.1", 7380))))
                .thenReturn(completedFuture((StatefulRedisConnection) connection1));

        sut.loadViews(seed, Duration.ofSeconds(1), false);

        verify(nodeConnectionFactory).connectToNodeAsync(any(RedisCodec.class),
                eq(InetSocketAddress.createUnresolved("127.0.0.1", 7380)));
        verifyNoMoreInteractions(nodeConnectionFactory);
    }

    @Test
    void shouldNotFailOnDuplicateSeedNodes() {

        List<RedisURI> seed = Arrays.asList(RedisURI.create("127.0.0.1", 7380), RedisURI.create("127.0.0.1", 7381),
                RedisURI.create("127.0.0.1", 7381));

        when(nodeConnectionFactory.connectToNodeAsync(any(RedisCodec.class),
                eq(InetSocketAddress.createUnresolved("127.0.0.1", 7380))))
                .thenReturn(completedFuture((StatefulRedisConnection) connection1));

        when(nodeConnectionFactory.connectToNodeAsync(any(RedisCodec.class),
                eq(InetSocketAddress.createUnresolved("127.0.0.1", 7381))))
                .thenReturn(completedFuture((StatefulRedisConnection) connection2));

        sut.loadViews(seed, Duration.ofSeconds(1), true);

        verify(nodeConnectionFactory).connectToNodeAsync(any(RedisCodec.class),
                eq(InetSocketAddress.createUnresolved("127.0.0.1", 7380)));
        verify(nodeConnectionFactory).connectToNodeAsync(any(RedisCodec.class),
                eq(InetSocketAddress.createUnresolved("127.0.0.1", 7381)));
    }

    @Test
    void shouldCloseConnections() {

        List<RedisURI> seed = Arrays.asList(RedisURI.create("127.0.0.1", 7380), RedisURI.create("127.0.0.1", 7381));

        when(nodeConnectionFactory.connectToNodeAsync(any(RedisCodec.class),
                eq(InetSocketAddress.createUnresolved("127.0.0.1", 7380))))
                .thenReturn(completedFuture((StatefulRedisConnection) connection1));
        when(nodeConnectionFactory.connectToNodeAsync(any(RedisCodec.class),
                eq(InetSocketAddress.createUnresolved("127.0.0.1", 7381))))
                .thenReturn(completedFuture((StatefulRedisConnection) connection2));

        sut.loadViews(seed, Duration.ofSeconds(1), true);

        verify(connection1).closeAsync();
        verify(connection2).closeAsync();
    }

    @Test
    void undiscoveredAdditionalNodesShouldBeLastUsingClientCount() {

        List<RedisURI> seed = Collections.singletonList(RedisURI.create("127.0.0.1", 7380));

        when(nodeConnectionFactory.connectToNodeAsync(any(RedisCodec.class),
                eq(InetSocketAddress.createUnresolved("127.0.0.1", 7380))))
                .thenReturn(completedFuture((StatefulRedisConnection) connection1));

        Map<RedisURI, Partitions> partitionsMap = sut.loadViews(seed, Duration.ofSeconds(1), false).toCompletableFuture()
                .join();

        Partitions partitions = partitionsMap.values().iterator().next();

        List<RedisClusterNode> nodes = TopologyComparators.sortByClientCount(partitions);

        assertThat(nodes).hasSize(2).extracting(RedisClusterNode::getUri).containsSequence(seed.get(0),
                RedisURI.create("127.0.0.1", 7381));
    }

    @Test
    void discoveredAdditionalNodesShouldBeOrderedUsingClientCount() {

        List<RedisURI> seed = Collections.singletonList(RedisURI.create("127.0.0.1", 7380));

        when(nodeConnectionFactory.connectToNodeAsync(any(RedisCodec.class),
                eq(InetSocketAddress.createUnresolved("127.0.0.1", 7380))))
                .thenReturn(completedFuture((StatefulRedisConnection) connection1));
        when(nodeConnectionFactory.connectToNodeAsync(any(RedisCodec.class),
                eq(InetSocketAddress.createUnresolved("127.0.0.1", 7381))))
                .thenReturn(completedFuture((StatefulRedisConnection) connection2));

        Map<RedisURI, Partitions> partitionsMap = sut.loadViews(seed, Duration.ofSeconds(1), true).toCompletableFuture().join();

        Partitions partitions = partitionsMap.values().iterator().next();

        List<RedisClusterNode> nodes = TopologyComparators.sortByClientCount(partitions);

        assertThat(nodes).hasSize(2).extracting(RedisClusterNode::getUri).containsSequence(RedisURI.create("127.0.0.1", 7381),
                seed.get(0));
    }

    @Test
    void undiscoveredAdditionalNodesShouldBeLastUsingLatency() {

        List<RedisURI> seed = Collections.singletonList(RedisURI.create("127.0.0.1", 7380));

        when(nodeConnectionFactory.connectToNodeAsync(any(RedisCodec.class),
                eq(InetSocketAddress.createUnresolved("127.0.0.1", 7380))))
                .thenReturn(completedFuture((StatefulRedisConnection) connection1));

        Map<RedisURI, Partitions> partitionsMap = sut.loadViews(seed, Duration.ofSeconds(1), false).toCompletableFuture()
                .join();

        Partitions partitions = partitionsMap.values().iterator().next();

        List<RedisClusterNode> nodes = TopologyComparators.sortByLatency(partitions);

        assertThat(nodes).hasSize(2).extracting(RedisClusterNode::getUri).containsSequence(seed.get(0),
                RedisURI.create("127.0.0.1", 7381));
    }

    @Test
    void discoveredAdditionalNodesShouldBeOrderedUsingLatency() {

        List<RedisURI> seed = Collections.singletonList(RedisURI.create("127.0.0.1", 7380));

        when(nodeConnectionFactory.connectToNodeAsync(any(RedisCodec.class),
                eq(InetSocketAddress.createUnresolved("127.0.0.1", 7380))))
                .thenReturn(completedFuture((StatefulRedisConnection) connection1));
        when(nodeConnectionFactory.connectToNodeAsync(any(RedisCodec.class),
                eq(InetSocketAddress.createUnresolved("127.0.0.1", 7381))))
                .thenReturn(completedFuture((StatefulRedisConnection) connection2));

        Map<RedisURI, Partitions> partitionsMap = sut.loadViews(seed, Duration.ofSeconds(1), true).toCompletableFuture().join();

        Partitions partitions = partitionsMap.values().iterator().next();

        List<RedisClusterNode> nodes = TopologyComparators.sortByLatency(partitions);

        assertThat(nodes).hasSize(2).extracting(RedisClusterNode::getUri).containsSequence(RedisURI.create("127.0.0.1", 7381),
                seed.get(0));
    }

    @Test
    void shouldPropagateCommandFailures() {

        List<RedisURI> seed = Arrays.asList(RedisURI.create("127.0.0.1", 7380), RedisURI.create("127.0.0.1", 7381));

        when(nodeConnectionFactory.connectToNodeAsync(any(RedisCodec.class),
                eq(InetSocketAddress.createUnresolved("127.0.0.1", 7380))))
                .thenReturn(completedFuture((StatefulRedisConnection) connection1));
        when(nodeConnectionFactory.connectToNodeAsync(any(RedisCodec.class),
                eq(InetSocketAddress.createUnresolved("127.0.0.1", 7381))))
                .thenReturn(completedFuture((StatefulRedisConnection) connection2));

        reset(connection1, connection2);

        when(connection1.async()).thenReturn(asyncCommands1);
        when(connection2.async()).thenReturn(asyncCommands2);
        when(connection1.closeAsync()).thenReturn(CompletableFuture.completedFuture(null));
        when(connection2.closeAsync()).thenReturn(CompletableFuture.completedFuture(null));

        when(connection1.dispatch(any(RedisCommand.class))).thenAnswer(invocation -> {

            TimedAsyncCommand command = invocation.getArgument(0);
            command.completeExceptionally(new RedisException("AUTH"));
            return command;
        });

        RedisException nestedException = new RedisException("NESTED");

        when(connection2.dispatch(any(RedisCommand.class))).thenAnswer(invocation -> {

            TimedAsyncCommand command = invocation.getArgument(0);
            command.completeExceptionally(nestedException);
            return command;
        });

        CompletionStage<Map<RedisURI, Partitions>> actual = sut.loadViews(seed, Duration.ofSeconds(1), true);
        assertThat(actual).isCompletedExceptionally();

        try {
            actual.toCompletableFuture().join();
            fail("Missing CompletionException");
        } catch (CompletionException e) {

            assertThat(e.getCause()).hasSuppressedException(nestedException);
        }
    }

    Requests createClusterNodesRequests(int duration, String nodes) {

        RedisURI redisURI = RedisURI.create("redis://localhost:" + duration);
        Connections connections = new Connections(clientResources, new HashMap<>());
        connections.addConnection(redisURI, connection);

        Requests requests = connections.requestTopology(100, TimeUnit.SECONDS);
        TimedAsyncCommand<String, String, String> command = requests.getRequest(redisURI);

        command.getOutput().set(ByteBuffer.wrap(nodes.getBytes()));
        command.complete();
        command.encodedAtNs = 0;
        command.completedAtNs = duration;

        return requests;
    }

    Requests createClientListRequests(int duration, String response) {

        RedisURI redisURI = RedisURI.create("redis://localhost:" + duration);
        Connections connections = new Connections(clientResources, new HashMap<>());
        connections.addConnection(redisURI, connection);

        Requests requests = connections.requestTopology(100, TimeUnit.SECONDS);
        TimedAsyncCommand<String, String, String> command = requests.getRequest(redisURI);

        command.getOutput().set(ByteBuffer.wrap(response.getBytes()));
        command.complete();

        return requests;
    }

    private static <T> ConnectionFuture<T> completedFuture(T value) {

        return ConnectionFuture.from(InetSocketAddress.createUnresolved(TestSettings.host(), TestSettings.port()),
                CompletableFuture.completedFuture(value));
    }

    private static <T> ConnectionFuture<T> completedWithException(Exception e) {

        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(e);

        return ConnectionFuture.from(InetSocketAddress.createUnresolved(TestSettings.host(), TestSettings.port()), future);
    }
}
