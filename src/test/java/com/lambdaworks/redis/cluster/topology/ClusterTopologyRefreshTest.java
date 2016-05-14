package com.lambdaworks.redis.cluster.topology;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.cluster.RedisClusterClient;
import com.lambdaworks.redis.cluster.models.partitions.Partitions;

/**
 * @author Mark Paluch
 */
@RunWith(MockitoJUnitRunner.class)
public class ClusterTopologyRefreshTest {

    public final static long COMMAND_TIMEOUT_NS = TimeUnit.MILLISECONDS.toNanos(10);

    private ClusterTopologyRefresh sut;

    @Mock
    private RedisClusterClient client;

    @Mock
    private StatefulRedisConnection<String, String> connection;

    @Before
    public void before() throws Exception {
        sut = new ClusterTopologyRefresh(null, null);
    }

    @Test
    public void getNodeSpecificViewsNode1IsFasterThanNode2() throws Exception {

        String nodes1 = "1 127.0.0.1:7380 master,myself - 0 1401258245007 2 disconnected 8000-11999\n"
                + "2 127.0.0.1:7381 master - 111 1401258245007 222 connected 7000 12000 12002-16383\n";
        Requests requests = createClusterNodesCommand(1, nodes1);

        String nodes2 = "1 127.0.0.1:7380 master - 0 1401258245007 2 disconnected 8000-11999\n"
                + "2 127.0.0.1:7381 master,myself - 111 1401258245007 222 connected 7000 12000 12002-16383\n";
        requests = createClusterNodesCommand(2, nodes2).mergeWith(requests);

        Requests clientRequests = createClientCommand(1, "c1\nc2\n").mergeWith(createClientCommand(2, "c1\nc2\n"));

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

        Requests clusterNodesRequests = createClusterNodesCommand(1, nodes1);
        Requests clientRequests = createClientCommand(1, "c1\nc2\n");

        NodeTopologyViews nodeSpecificViews = sut.getNodeSpecificViews(clusterNodesRequests, clientRequests,
                COMMAND_TIMEOUT_NS);

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

        String nodes1 = "1 127.0.0.1:7380 master,myself - 0 1401258245007 2 disconnected 8000-11999\n"
                + "2 127.0.0.1:7381 master - 111 1401258245007 222 connected 7000 12000 12002-16383\n";
        Requests clusterNodesRequests = createClusterNodesCommand(5, nodes1);

        String nodes2 = "1 127.0.0.1:7380 master - 0 1401258245007 2 disconnected 8000-11999\n"
                + "2 127.0.0.1:7381 master,myself - 111 1401258245007 222 connected 7000 12000 12002-16383\n";
        clusterNodesRequests = createClusterNodesCommand(1, nodes2).mergeWith(clusterNodesRequests);

        Requests clientRequests = createClientCommand(5, "c1\nc2\n").mergeWith(createClientCommand(1, "c1\nc2\n"));

        NodeTopologyViews nodeSpecificViews = sut.getNodeSpecificViews(clusterNodesRequests, clientRequests,
                COMMAND_TIMEOUT_NS);
        List<Partitions> values = new ArrayList<>(nodeSpecificViews.toMap().values());

        assertThat(values).hasSize(2);

        for (Partitions value : values) {
            assertThat(value).extracting("nodeId").containsExactly("2", "1");
        }
    }

    protected Requests createClusterNodesCommand(int duration, String nodes) {

        RedisURI redisURI = RedisURI.create("redis://localhost:" + duration);
        Connections connections = new Connections();
        connections.addConnection(redisURI, connection);

        Requests requests = connections.requestTopology();
        TimedAsyncCommand<String, String, String> command = requests.rawViews.get(redisURI);

        command.getOutput().set(ByteBuffer.wrap(nodes.getBytes()));
        command.complete();
        command.encodedAtNs = 0;
        command.completedAtNs = duration;

        return requests;

    }

    protected Requests createClientCommand(int duration, String response) {

        RedisURI redisURI = RedisURI.create("redis://localhost:" + duration);
        Connections connections = new Connections();
        connections.addConnection(redisURI, connection);

        Requests requests = connections.requestTopology();
        TimedAsyncCommand<String, String, String> command = requests.rawViews.get(redisURI);

        command.getOutput().set(ByteBuffer.wrap(response.getBytes()));
        command.complete();

        return requests;
    }
}
