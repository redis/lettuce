package com.lambdaworks.redis.cluster;

import static com.lambdaworks.redis.cluster.TopologyComparators.isChanged;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.Test;

import com.google.common.collect.Lists;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.cluster.models.partitions.ClusterPartitionParser;
import com.lambdaworks.redis.cluster.models.partitions.Partitions;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;

/**
 * @author Mark Paluch
 */
public class TopologyComparatorsTest {

    private RedisClusterNode node1 = createNode("1");
    private RedisClusterNode node2 = createNode("2");
    private RedisClusterNode node3 = createNode("3");

    private static RedisClusterNode createNode(String nodeId) {
        RedisClusterNode result = new RedisClusterNode();
        result.setNodeId(nodeId);
        result.setUri(RedisURI.Builder.redis("localhost", Integer.parseInt(nodeId)).build());
        return result;
    }

    @Test
    public void testFixedOrdering1() throws Exception {

        List<RedisClusterNode> list = Lists.newArrayList(node2, node3, node1);
        List<RedisURI> fixedOrder = Lists.newArrayList(node1.getUri(), node2.getUri(), node3.getUri());

        assertThat(TopologyComparators.predefinedSort(list, fixedOrder)).containsSequence(node1, node2, node3);
    }

    @Test
    public void testFixedOrdering2() throws Exception {

        List<RedisClusterNode> list = Lists.newArrayList(node2, node3, node1);
        List<RedisURI> fixedOrder = Lists.newArrayList(node3.getUri(), node2.getUri(), node1.getUri());

        assertThat(TopologyComparators.predefinedSort(list, fixedOrder)).containsSequence(node3, node2, node1);
    }

    @Test
    public void testFixedOrderingNoFixedPart() throws Exception {

        List<RedisClusterNode> list = Lists.newArrayList(node2, node3, node1);
        List<RedisURI> fixedOrder = Lists.newArrayList();

        assertThat(TopologyComparators.predefinedSort(list, fixedOrder)).containsSequence(node1, node2, node3);
    }

    @Test
    public void testFixedOrderingPartiallySpecifiedOrder() throws Exception {

        List<RedisClusterNode> list = Lists.newArrayList(node2, node3, node1);
        List<RedisURI> fixedOrder = Lists.newArrayList(node3.getUri(), node1.getUri());

        assertThat(TopologyComparators.predefinedSort(list, fixedOrder)).containsSequence(node3, node1, node2);
    }

    @Test
    public void isChangedSamePartitions() throws Exception {

        String nodes = "c37ab8396be428403d4e55c0d317348be27ed973 127.0.0.1:7381 master - 111 1401258245007 222 connected 7000 12000 12002-16383\n"
                + "3d005a179da7d8dc1adae6409d47b39c369e992b 127.0.0.1:7380 master - 0 1401258245007 2 disconnected 8000-11999\n";

        Partitions partitions1 = ClusterPartitionParser.parse(nodes);
        Partitions partitions2 = ClusterPartitionParser.parse(nodes);
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
    public void isChangedFlagsChangedSlaveToMaster() throws Exception {
        String nodes1 = "3d005a179da7d8dc1adae6409d47b39c369e992b 127.0.0.1:7380 slave - 0 1401258245007 2 disconnected 8000-11999\n"
                + "c37ab8396be428403d4e55c0d317348be27ed973 127.0.0.1:7381 master - 111 1401258245007 222 connected 7000 12000 12002-16383\n";

        String nodes2 = "3d005a179da7d8dc1adae6409d47b39c369e992b 127.0.0.1:7380 master - 0 1401258245007 2 disconnected 8000-11999\n"
                + "c37ab8396be428403d4e55c0d317348be27ed973 127.0.0.1:7381 master - 111 1401258245007 222 connected 7000 12000 12002-16383\n";

        Partitions partitions1 = ClusterPartitionParser.parse(nodes1);
        Partitions partitions2 = ClusterPartitionParser.parse(nodes2);
        assertThat(isChanged(partitions1, partitions2)).isTrue();
    }

    @Test
    public void isChangedFlagsChangedMasterToSlave() throws Exception {
        String nodes1 = "3d005a179da7d8dc1adae6409d47b39c369e992b 127.0.0.1:7380 master - 0 1401258245007 2 disconnected 8000-11999\n"
                + "c37ab8396be428403d4e55c0d317348be27ed973 127.0.0.1:7381 master - 111 1401258245007 222 connected 7000 12000 12002-16383\n";

        String nodes2 = "3d005a179da7d8dc1adae6409d47b39c369e992b 127.0.0.1:7380 slave - 0 1401258245007 2 disconnected 8000-11999\n"
                + "c37ab8396be428403d4e55c0d317348be27ed973 127.0.0.1:7381 master - 111 1401258245007 222 connected 7000 12000 12002-16383\n";

        Partitions partitions1 = ClusterPartitionParser.parse(nodes1);
        Partitions partitions2 = ClusterPartitionParser.parse(nodes2);
        assertThat(isChanged(partitions1, partitions2)).isTrue();
    }
}