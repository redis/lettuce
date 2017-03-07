/*
 * Copyright 2011-2016 the original author or authors.
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
package io.lettuce.core.cluster.topology;

import static io.lettuce.core.cluster.topology.TopologyComparators.isChanged;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Lists.newArrayList;

import java.util.*;

import io.lettuce.core.RedisURI;
import org.junit.Test;

import io.lettuce.core.cluster.models.partitions.ClusterPartitionParser;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.internal.LettuceLists;

/**
 * @author Mark Paluch
 */
public class TopologyComparatorsTest {

    private RedisClusterNodeSnapshot node1 = createNode("1");
    private RedisClusterNodeSnapshot node2 = createNode("2");
    private RedisClusterNodeSnapshot node3 = createNode("3");

    private static RedisClusterNodeSnapshot createNode(String nodeId) {
        RedisClusterNodeSnapshot result = new RedisClusterNodeSnapshot();
        result.setNodeId(nodeId);
        result.setUri(RedisURI.create("localhost", Integer.parseInt(nodeId)));
        return result;
    }

    @Test
    public void latenciesForAllNodes() throws Exception {

        Map<String, Long> map = new HashMap<>();
        map.put(node1.getNodeId(), 1L);
        map.put(node2.getNodeId(), 2L);
        map.put(node3.getNodeId(), 3L);

        runTest(map, newArrayList(node1, node2, node3), newArrayList(node3, node1, node2));
        runTest(map, newArrayList(node1, node2, node3), newArrayList(node1, node2, node3));
        runTest(map, newArrayList(node1, node2, node3), newArrayList(node3, node2, node1));
    }

    @Test
    public void latenciesForTwoNodes_N1_N2() throws Exception {

        Map<String, Long> map = new HashMap<>();
        map.put(node1.getNodeId(), 1L);
        map.put(node2.getNodeId(), 2L);

        runTest(map, newArrayList(node1, node2, node3), newArrayList(node3, node1, node2));
        runTest(map, newArrayList(node1, node2, node3), newArrayList(node1, node2, node3));
        runTest(map, newArrayList(node1, node2, node3), newArrayList(node3, node2, node1));
    }

    @Test
    public void latenciesForTwoNodes_N2_N3() throws Exception {

        Map<String, Long> map = new HashMap<>();
        map.put(node3.getNodeId(), 1L);
        map.put(node2.getNodeId(), 2L);

        runTest(map, newArrayList(node3, node2, node1), newArrayList(node3, node1, node2));
        runTest(map, newArrayList(node3, node2, node1), newArrayList(node1, node2, node3));
        runTest(map, newArrayList(node3, node2, node1), newArrayList(node3, node2, node1));
    }

    @Test
    public void latenciesForOneNode() throws Exception {

        Map<String, Long> map = Collections.singletonMap(node2.getNodeId(), 2L);

        runTest(map, newArrayList(node2, node3, node1), newArrayList(node3, node1, node2));
        runTest(map, newArrayList(node2, node1, node3), newArrayList(node1, node2, node3));
        runTest(map, newArrayList(node2, node3, node1), newArrayList(node3, node2, node1));
    }

    @Test(expected = AssertionError.class)
    public void shouldFail() throws Exception {

        Map<String, Long> map = Collections.singletonMap(node2.getNodeId(), 2L);

        runTest(map, newArrayList(node2, node1, node3), newArrayList(node3, node1, node2));
    }

    @Test
    public void testLatencyComparator() throws Exception {

        RedisClusterNodeSnapshot node1 = new RedisClusterNodeSnapshot();
        node1.setLatencyNs(1L);

        RedisClusterNodeSnapshot node2 = new RedisClusterNodeSnapshot();
        node2.setLatencyNs(2L);

        RedisClusterNodeSnapshot node3 = new RedisClusterNodeSnapshot();
        node3.setLatencyNs(3L);

        List<RedisClusterNodeSnapshot> list = LettuceLists.newList(node2, node3, node1);
        Collections.sort(list, TopologyComparators.LatencyComparator.INSTANCE);

        assertThat(list).containsSequence(node1, node2, node3);
    }

    @Test
    public void testLatencyComparatorWithSomeNodesWithoutStats() throws Exception {

        RedisClusterNodeSnapshot node1 = new RedisClusterNodeSnapshot();
        node1.setLatencyNs(1L);

        RedisClusterNodeSnapshot node2 = new RedisClusterNodeSnapshot();
        node2.setLatencyNs(2L);

        RedisClusterNodeSnapshot node3 = new RedisClusterNodeSnapshot();
        RedisClusterNodeSnapshot node4 = new RedisClusterNodeSnapshot();

        List<RedisClusterNodeSnapshot> list = LettuceLists.newList(node2, node3, node4, node1);
        Collections.sort(list, TopologyComparators.LatencyComparator.INSTANCE);

        assertThat(list).containsSequence(node1, node2, node3, node4);
    }

    @Test
    public void testClientComparator() throws Exception {

        RedisClusterNodeSnapshot node1 = new RedisClusterNodeSnapshot();
        node1.setConnectedClients(1);

        RedisClusterNodeSnapshot node2 = new RedisClusterNodeSnapshot();
        node2.setConnectedClients(2);

        RedisClusterNodeSnapshot node3 = new RedisClusterNodeSnapshot();
        node3.setConnectedClients(3);

        List<RedisClusterNodeSnapshot> list = LettuceLists.newList(node2, node3, node1);
        Collections.sort(list, TopologyComparators.ClientCountComparator.INSTANCE);

        assertThat(list).containsSequence(node1, node2, node3);
    }

    @Test
    public void testClientComparatorWithSomeNodesWithoutStats() throws Exception {

        RedisClusterNodeSnapshot node1 = new RedisClusterNodeSnapshot();
        node1.setConnectedClients(1);

        RedisClusterNodeSnapshot node2 = new RedisClusterNodeSnapshot();
        node2.setConnectedClients(2);

        RedisClusterNodeSnapshot node3 = new RedisClusterNodeSnapshot();
        RedisClusterNodeSnapshot node4 = new RedisClusterNodeSnapshot();

        List<RedisClusterNodeSnapshot> list = LettuceLists.newList(node2, node3, node4, node1);
        Collections.sort(list, TopologyComparators.ClientCountComparator.INSTANCE);

        assertThat(list).containsSequence(node1, node2, node3, node4);
    }

    @Test
    public void testLatencyComparatorWithoutClients() throws Exception {

        RedisClusterNodeSnapshot node1 = new RedisClusterNodeSnapshot();
        node1.setConnectedClients(1);

        RedisClusterNodeSnapshot node2 = new RedisClusterNodeSnapshot();
        node2.setConnectedClients(null);

        RedisClusterNodeSnapshot node3 = new RedisClusterNodeSnapshot();
        node3.setConnectedClients(3);

        List<RedisClusterNodeSnapshot> list = LettuceLists.newList(node2, node3, node1);
        Collections.sort(list, TopologyComparators.LatencyComparator.INSTANCE);

        assertThat(list).containsSequence(node1, node3, node2);
    }

    @Test
    public void testFixedOrdering1() throws Exception {

        List<RedisClusterNode> list = LettuceLists.newList(node2, node3, node1);
        List<RedisURI> fixedOrder = LettuceLists.newList(node1.getUri(), node2.getUri(), node3.getUri());

        assertThat(TopologyComparators.predefinedSort(list, fixedOrder)).containsSequence(node1, node2, node3);
    }

    @Test
    public void testFixedOrdering2() throws Exception {

        List<RedisClusterNode> list = LettuceLists.newList(node2, node3, node1);
        List<RedisURI> fixedOrder = LettuceLists.newList(node3.getUri(), node2.getUri(), node1.getUri());

        assertThat(TopologyComparators.predefinedSort(list, fixedOrder)).containsSequence(node3, node2, node1);
    }

    @Test
    public void testFixedOrderingNoFixedPart() throws Exception {

        List<RedisClusterNode> list = LettuceLists.newList(node2, node3, node1);
        List<RedisURI> fixedOrder = LettuceLists.newList();

        assertThat(TopologyComparators.predefinedSort(list, fixedOrder)).containsSequence(node1, node2, node3);
    }

    @Test
    public void testFixedOrderingPartiallySpecifiedOrder() throws Exception {

        List<RedisClusterNode> list = LettuceLists.newList(node2, node3, node1);
        List<RedisURI> fixedOrder = LettuceLists.newList(node3.getUri(), node1.getUri());

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

    protected void runTest(Map<String, Long> map, List<RedisClusterNodeSnapshot> expectation,
            List<RedisClusterNodeSnapshot> nodes) {

        for (RedisClusterNodeSnapshot node : nodes) {
            node.setLatencyNs(map.get(node.getNodeId()));
        }
        List<RedisClusterNode> result = TopologyComparators.sortByLatency((Iterable) nodes);

        assertThat(result).containsExactly(expectation.toArray(new RedisClusterNodeSnapshot[expectation.size()]));
    }
}
