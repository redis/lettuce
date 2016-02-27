package com.lambdaworks.redis.cluster;

import static com.lambdaworks.redis.cluster.ClusterTopologyRefresh.isChanged;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.cluster.ClusterTopologyRefresh.RedisClusterNodeSnapshot;
import com.lambdaworks.redis.cluster.models.partitions.ClusterPartitionParser;
import com.lambdaworks.redis.cluster.models.partitions.Partitions;
import com.lambdaworks.redis.protocol.AsyncCommand;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
@RunWith(MockitoJUnitRunner.class)
public class ClusterTopologyRefreshTest {

    public static final String NODES = "c37ab8396be428403d4e55c0d317348be27ed973 127.0.0.1:7381 master - 111 1401258245007 222 connected 7000 12000 12002-16383\n"
            + "3d005a179da7d8dc1adae6409d47b39c369e992b 127.0.0.1:7380 master - 0 1401258245007 2 disconnected 8000-11999\n";

    private ClusterTopologyRefresh sut;
    @Mock
    private RedisClusterClient client;

    @Before
    public void before() throws Exception {
        sut = new ClusterTopologyRefresh(client);
        when(client.getFirstUri()).thenReturn(RedisURI.create("redis://localhost:999"));
    }

    @Test
    public void isChangedSamePartitions() throws Exception {
        Partitions partitions1 = ClusterPartitionParser.parse(NODES);
        Partitions partitions2 = ClusterPartitionParser.parse(NODES);
        assertThat(isChanged(partitions1, partitions2)).isFalse();
    }

    @Test
    public void isChangedDifferentOrder() throws Exception {
        String nodes1 = "3d005a179da7d8dc1adae6409d47b39c369e992b 127.0.0.1:7380 master,myself - 0 1401258245007 2 disconnected 8000-11999\n"
                + "c37ab8396be428403d4e55c0d317348be27ed973 127.0.0.1:7381 master - 111 1401258245007 222 connected 7000 12000 12002-16383\n";

        String nodes2 = "c37ab8396be428403d4e55c0d317348be27ed973 127.0.0.1:7381 master,myself - 111 1401258245007 222 connected 7000 12000 12002-16383\n"
                + "3d005a179da7d8dc1adae6409d47b39c369e992b 127.0.0.1:7380 master - 0 1401258245007 2 disconnected 8000-11999\n";

        assertThat(nodes1).isNotEqualTo(nodes2);
        Partitions partitions1 = ClusterPartitionParser.parse(nodes1);
        Partitions partitions2 = ClusterPartitionParser.parse(nodes2);
        assertThat(isChanged(partitions1, partitions2)).isFalse();
    }

    @Test
    public void isChangedPortChanged() throws Exception {
        String nodes1 = "3d005a179da7d8dc1adae6409d47b39c369e992b 127.0.0.1:7382 master - 0 1401258245007 2 disconnected 8000-11999\n"
                + "c37ab8396be428403d4e55c0d317348be27ed973 127.0.0.1:7381 master - 111 1401258245007 222 connected 7000 12000 12002-16383\n";

        String nodes2 = "c37ab8396be428403d4e55c0d317348be27ed973 127.0.0.1:7381 master - 111 1401258245007 222 connected 7000 12000 12002-16383\n"
                + "3d005a179da7d8dc1adae6409d47b39c369e992b 127.0.0.1:7380 master - 0 1401258245007 2 disconnected 8000-11999\n";

        Partitions partitions1 = ClusterPartitionParser.parse(nodes1);
        Partitions partitions2 = ClusterPartitionParser.parse(nodes2);
        assertThat(isChanged(partitions1, partitions2)).isFalse();
    }

    @Test
    public void isChangedSlotsChanged() throws Exception {
        String nodes1 = "3d005a179da7d8dc1adae6409d47b39c369e992b 127.0.0.1:7380 master - 0 1401258245007 2 disconnected 8000-11999\n"
                + "c37ab8396be428403d4e55c0d317348be27ed973 127.0.0.1:7381 master - 111 1401258245007 222 connected 7000 12000 12002-16383\n";

        String nodes2 = "3d005a179da7d8dc1adae6409d47b39c369e992b 127.0.0.1:7380 master - 0 1401258245007 2 disconnected 8000-11999\n"
                + "c37ab8396be428403d4e55c0d317348be27ed973 127.0.0.1:7381 master - 111 1401258245007 222 connected 7000 12000 12001-16383\n";

        Partitions partitions1 = ClusterPartitionParser.parse(nodes1);
        Partitions partitions2 = ClusterPartitionParser.parse(nodes2);
        assertThat(isChanged(partitions1, partitions2)).isTrue();
    }

    @Test
    public void isChangedNodeIdChanged() throws Exception {
        String nodes1 = "3d005a179da7d8dc1adae6409d47b39c369e992b 127.0.0.1:7380 master - 0 1401258245007 2 disconnected 8000-11999\n"
                + "c37ab8396be428403d4e55c0d317348be27ed973 127.0.0.1:7381 master - 111 1401258245007 222 connected 7000 12000 12002-16383\n";

        String nodes2 = "3d005a179da7d8dc1adae6409d47b39c369e992aa 127.0.0.1:7380 master - 0 1401258245007 2 disconnected 8000-11999\n"
                + "c37ab8396be428403d4e55c0d317348be27ed973 127.0.0.1:7381 master - 111 1401258245007 222 connected 7000 12000 12002-16383\n";

        Partitions partitions1 = ClusterPartitionParser.parse(nodes1);
        Partitions partitions2 = ClusterPartitionParser.parse(nodes2);
        assertThat(isChanged(partitions1, partitions2)).isTrue();
    }

    @Test
    public void isChangedFlagsChanged() throws Exception {
        String nodes1 = "3d005a179da7d8dc1adae6409d47b39c369e992b 127.0.0.1:7380 slave - 0 1401258245007 2 disconnected 8000-11999\n"
                + "c37ab8396be428403d4e55c0d317348be27ed973 127.0.0.1:7381 master - 111 1401258245007 222 connected 7000 12000 12002-16383\n";

        String nodes2 = "3d005a179da7d8dc1adae6409d47b39c369e992b 127.0.0.1:7380 master - 0 1401258245007 2 disconnected 8000-11999\n"
                + "c37ab8396be428403d4e55c0d317348be27ed973 127.0.0.1:7381 master - 111 1401258245007 222 connected 7000 12000 12002-16383\n";

        Partitions partitions1 = ClusterPartitionParser.parse(nodes1);
        Partitions partitions2 = ClusterPartitionParser.parse(nodes2);
        assertThat(isChanged(partitions1, partitions2)).isTrue();
    }

    @Test
    public void getNodeSpecificViews_1_2() throws Exception {

        Map<RedisURI, ClusterTopologyRefresh.TimedAsyncCommand<String, String, String>> commands = Maps.newHashMap();

        String nodes1 = "1 127.0.0.1:7380 master,myself - 0 1401258245007 2 disconnected 8000-11999\n"
                + "2 127.0.0.1:7381 master - 111 1401258245007 222 connected 7000 12000 12002-16383\n";
        createClusterNodesCommand(commands, 1, nodes1);

        String nodes2 = "1 127.0.0.1:7380 master - 0 1401258245007 2 disconnected 8000-11999\n"
                + "2 127.0.0.1:7381 master,myself - 111 1401258245007 222 connected 7000 12000 12002-16383\n";
        createClusterNodesCommand(commands, 2, nodes2);

        List<Partitions> values = Lists.newArrayList(sut.getNodeSpecificViews(commands, Maps.newHashMap()).values());

        assertThat(values).hasSize(2);

        for (Partitions value : values) {
            assertThat(value).extracting("nodeId").containsExactly("1", "2");
        }
    }

    @Test
    public void getNodeSpecificViewTestingNoAddrFilter() throws Exception {

        Map<RedisURI, ClusterTopologyRefresh.TimedAsyncCommand<String, String, String>> clusterCommands = Maps.newHashMap();
        Map<RedisURI, AsyncCommand<String, String, String>> clientCommands = Maps.newHashMap();

        String nodes1 = "n1 10.37.110.63:7000 slave n3 0 1452553664848 43 connected\n"
                + "n2 10.37.110.68:7000 slave n6 0 1452553664346 45 connected\n"
                + "badSlave :0 slave,fail,noaddr n5 1449160058028 1449160053146 46 disconnected\n"
                + "n3 10.37.110.69:7000 master - 0 1452553662842 43 connected 3829-6787 7997-9999\n"
                + "n4 10.37.110.62:7000 slave n3 0 1452553663844 43 connected\n"
                + "n5 10.37.110.70:7000 myself,master - 0 0 46 connected 10039-14999\n"
                + "n6 10.37.110.65:7000 master - 0 1452553663844 45 connected 0-3828 6788-7996 10000-10038 15000-16383";

        createClusterNodesCommand(clusterCommands, 1, nodes1);
        createClientCommand(clientCommands, 1, "c1\nc2\n");

        List<Partitions> values = Lists.newArrayList(sut.getNodeSpecificViews(clusterCommands, clientCommands).values());

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
    public void getNodeSpecificViews_2_1() throws Exception {

        Map<RedisURI, ClusterTopologyRefresh.TimedAsyncCommand<String, String, String>> commands = Maps.newHashMap();

        String nodes1 = "1 127.0.0.1:7380 master,myself - 0 1401258245007 2 disconnected 8000-11999\n"
                + "2 127.0.0.1:7381 master - 111 1401258245007 222 connected 7000 12000 12002-16383\n";
        createClusterNodesCommand(commands, 5, nodes1);

        String nodes2 = "1 127.0.0.1:7380 master - 0 1401258245007 2 disconnected 8000-11999\n"
                + "2 127.0.0.1:7381 master,myself - 111 1401258245007 222 connected 7000 12000 12002-16383\n";
        createClusterNodesCommand(commands, 1, nodes2);

        List<Partitions> values = Lists.newArrayList(sut.getNodeSpecificViews(commands, Maps.newHashMap()).values());

        assertThat(values).hasSize(2);

        for (Partitions value : values) {
            assertThat(value).extracting("nodeId").containsExactly("2", "1");
        }
    }

    @Test
    public void testLatencyComparator() throws Exception {

        RedisClusterNodeSnapshot node1 = new RedisClusterNodeSnapshot();
        node1.setConnectedClients(1);

        RedisClusterNodeSnapshot node2 = new RedisClusterNodeSnapshot();
        node2.setConnectedClients(2);

        RedisClusterNodeSnapshot node3 = new RedisClusterNodeSnapshot();
        node3.setConnectedClients(3);

        List<RedisClusterNodeSnapshot> list = Lists.newArrayList(node2, node3, node1);
        Collections.sort(list, ClusterTopologyRefresh.LatencyComparator.INSTANCE);

        assertThat(list).containsSequence(node1, node2, node3);
    }

    @Test
    public void testLatencyComparatorWithoutClients() throws Exception {

        RedisClusterNodeSnapshot node1 = new RedisClusterNodeSnapshot();
        node1.setConnectedClients(1);

        RedisClusterNodeSnapshot node2 = new RedisClusterNodeSnapshot();
        node2.setConnectedClients(null);

        RedisClusterNodeSnapshot node3 = new RedisClusterNodeSnapshot();
        node3.setConnectedClients(3);

        List<RedisClusterNodeSnapshot> list = Lists.newArrayList(node2, node3, node1);
        Collections.sort(list, ClusterTopologyRefresh.LatencyComparator.INSTANCE);

        assertThat(list).containsSequence(node1, node3, node2);

    }

    protected void createClusterNodesCommand(
            Map<RedisURI, ClusterTopologyRefresh.TimedAsyncCommand<String, String, String>> commands, int duration, String nodes) {

        ClusterTopologyRefresh.TimedAsyncCommand<String, String, String> command1 = sut.createClusterNodesCommand();
        command1.getOutput().set(ByteBuffer.wrap(nodes.getBytes()));
        command1.complete();
        command1.encodedAtNs = 0;
        command1.completedAtNs = duration;

        commands.put(RedisURI.create("redis://localhost:" + duration), command1);
    }

    protected void createClientCommand(Map<RedisURI, AsyncCommand<String, String, String>> commands, int duration,
            String response) {

        AsyncCommand<String, String, String> command1 = sut.createClusterNodesCommand();
        command1.getOutput().set(ByteBuffer.wrap(response.getBytes()));
        command1.complete();

        commands.put(RedisURI.create("redis://localhost:" + duration), command1);
    }
}
