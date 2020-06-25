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

import static io.lettuce.core.cluster.topology.TopologyComparators.isChanged;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.util.Lists.newArrayList;

import java.util.*;

import org.junit.jupiter.api.Test;

import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.models.partitions.ClusterPartitionParser;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.internal.LettuceLists;

/**
 * Unit tests for {@link TopologyComparators}.
 *
 * @author Mark Paluch
 * @author Alessandro Simi
 */
class TopologyComparatorsUnitTests {

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
    void latenciesForAllNodes() {

        Map<String, Long> map = new HashMap<>();
        map.put(node1.getNodeId(), 1L);
        map.put(node2.getNodeId(), 2L);
        map.put(node3.getNodeId(), 3L);

        runTest(map, newArrayList(node1, node2, node3), newArrayList(node3, node1, node2));
        runTest(map, newArrayList(node1, node2, node3), newArrayList(node1, node2, node3));
        runTest(map, newArrayList(node1, node2, node3), newArrayList(node3, node2, node1));
    }

    @Test
    void latenciesForTwoNodes_N1_N2() {

        Map<String, Long> map = new HashMap<>();
        map.put(node1.getNodeId(), 1L);
        map.put(node2.getNodeId(), 2L);

        runTest(map, newArrayList(node1, node2, node3), newArrayList(node3, node1, node2));
        runTest(map, newArrayList(node1, node2, node3), newArrayList(node1, node2, node3));
        runTest(map, newArrayList(node1, node2, node3), newArrayList(node3, node2, node1));
    }

    @Test
    void latenciesForTwoNodes_N2_N3() {

        Map<String, Long> map = new HashMap<>();
        map.put(node3.getNodeId(), 1L);
        map.put(node2.getNodeId(), 2L);

        runTest(map, newArrayList(node3, node2, node1), newArrayList(node3, node1, node2));
        runTest(map, newArrayList(node3, node2, node1), newArrayList(node1, node2, node3));
        runTest(map, newArrayList(node3, node2, node1), newArrayList(node3, node2, node1));
    }

    @Test
    void latenciesForOneNode() {

        Map<String, Long> map = Collections.singletonMap(node2.getNodeId(), 2L);

        runTest(map, newArrayList(node2, node3, node1), newArrayList(node3, node1, node2));
        runTest(map, newArrayList(node2, node1, node3), newArrayList(node1, node2, node3));
        runTest(map, newArrayList(node2, node3, node1), newArrayList(node3, node2, node1));
    }

    @Test
    void shouldFail() {

        Map<String, Long> map = Collections.singletonMap(node2.getNodeId(), 2L);

        assertThatThrownBy(() -> runTest(map, newArrayList(node2, node1, node3), newArrayList(node3, node1, node2)))
                .isInstanceOf(AssertionError.class);
    }

    @Test
    void testLatencyComparator() {

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
    void testLatencyComparatorWithSomeNodesWithoutStats() {

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
    void testClientComparator() {

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
    void testClientComparatorWithSomeNodesWithoutStats() {

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
    void testLatencyComparatorWithoutClients() {

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
    void testFixedOrdering1() {

        List<RedisClusterNode> list = LettuceLists.newList(node2, node3, node1);
        List<RedisURI> fixedOrder = LettuceLists.newList(node1.getUri(), node2.getUri(), node3.getUri());

        assertThat(TopologyComparators.predefinedSort(list, fixedOrder)).containsSequence(node1, node2, node3);
    }

    @Test
    void testFixedOrdering2() {

        List<RedisClusterNode> list = LettuceLists.newList(node2, node3, node1);
        List<RedisURI> fixedOrder = LettuceLists.newList(node3.getUri(), node2.getUri(), node1.getUri());

        assertThat(TopologyComparators.predefinedSort(list, fixedOrder)).containsSequence(node3, node2, node1);
    }

    @Test
    void testFixedOrderingNoFixedPart() {

        List<RedisClusterNode> list = LettuceLists.newList(node2, node3, node1);
        List<RedisURI> fixedOrder = LettuceLists.newList();

        assertThat(TopologyComparators.predefinedSort(list, fixedOrder)).containsSequence(node1, node2, node3);
    }

    @Test
    void testFixedOrderingPartiallySpecifiedOrder() {

        List<RedisClusterNode> list = LettuceLists.newList(node2, node3, node1);
        List<RedisURI> fixedOrder = LettuceLists.newList(node3.getUri(), node1.getUri());

        assertThat(TopologyComparators.predefinedSort(list, fixedOrder)).containsSequence(node3, node1, node2);
    }

    @Test
    void isChangedSamePartitions() {

        String nodes = "c37ab8396be428403d4e55c0d317348be27ed973 127.0.0.1:7381 master - 111 1401258245007 222 connected 7000 12000 12002-16383\n"
                + "3d005a179da7d8dc1adae6409d47b39c369e992b 127.0.0.1:7380 master - 0 1401258245007 2 disconnected 8000-11999\n";

        Partitions partitions1 = ClusterPartitionParser.parse(nodes);
        Partitions partitions2 = ClusterPartitionParser.parse(nodes);
        assertThat(isChanged(partitions1, partitions2)).isFalse();
    }

    @Test
    void isChangedDifferentOrder() {
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
    void isChangedPortChanged() {
        String nodes1 = "3d005a179da7d8dc1adae6409d47b39c369e992b 127.0.0.1:7382 master - 0 1401258245007 2 disconnected 8000-11999\n"
                + "c37ab8396be428403d4e55c0d317348be27ed973 127.0.0.1:7381 master - 111 1401258245007 222 connected 7000 12000 12002-16383\n";

        String nodes2 = "c37ab8396be428403d4e55c0d317348be27ed973 127.0.0.1:7381 master - 111 1401258245007 222 connected 7000 12000 12002-16383\n"
                + "3d005a179da7d8dc1adae6409d47b39c369e992b 127.0.0.1:7380 master - 0 1401258245007 2 disconnected 8000-11999\n";

        Partitions partitions1 = ClusterPartitionParser.parse(nodes1);
        Partitions partitions2 = ClusterPartitionParser.parse(nodes2);
        assertThat(isChanged(partitions1, partitions2)).isFalse();
    }

    @Test
    void isChangedSlotsChanged() {
        String nodes1 = "3d005a179da7d8dc1adae6409d47b39c369e992b 127.0.0.1:7380 master - 0 1401258245007 2 disconnected 8000-11999\n"
                + "c37ab8396be428403d4e55c0d317348be27ed973 127.0.0.1:7381 master - 111 1401258245007 222 connected 7000 12000 12002-16383\n";

        String nodes2 = "3d005a179da7d8dc1adae6409d47b39c369e992b 127.0.0.1:7380 master - 0 1401258245007 2 disconnected 8000-11999\n"
                + "c37ab8396be428403d4e55c0d317348be27ed973 127.0.0.1:7381 master - 111 1401258245007 222 connected 7000 12000 12001-16383\n";

        Partitions partitions1 = ClusterPartitionParser.parse(nodes1);
        Partitions partitions2 = ClusterPartitionParser.parse(nodes2);
        assertThat(isChanged(partitions1, partitions2)).isTrue();
    }

    @Test
    void isChangedNodeIdChanged() {
        String nodes1 = "3d005a179da7d8dc1adae6409d47b39c369e992b 127.0.0.1:7380 master - 0 1401258245007 2 disconnected 8000-11999\n"
                + "c37ab8396be428403d4e55c0d317348be27ed973 127.0.0.1:7381 master - 111 1401258245007 222 connected 7000 12000 12002-16383\n";

        String nodes2 = "3d005a179da7d8dc1adae6409d47b39c369e992aa 127.0.0.1:7380 master - 0 1401258245007 2 disconnected 8000-11999\n"
                + "c37ab8396be428403d4e55c0d317348be27ed973 127.0.0.1:7381 master - 111 1401258245007 222 connected 7000 12000 12002-16383\n";

        Partitions partitions1 = ClusterPartitionParser.parse(nodes1);
        Partitions partitions2 = ClusterPartitionParser.parse(nodes2);
        assertThat(isChanged(partitions1, partitions2)).isTrue();
    }

    @Test
    void isChangedFlagsChangedReplicaToMaster() {
        String nodes1 = "3d005a179da7d8dc1adae6409d47b39c369e992b 127.0.0.1:7380 slave - 0 1401258245007 2 disconnected 8000-11999\n"
                + "c37ab8396be428403d4e55c0d317348be27ed973 127.0.0.1:7381 master - 111 1401258245007 222 connected 7000 12000 12002-16383\n";

        String nodes2 = "3d005a179da7d8dc1adae6409d47b39c369e992b 127.0.0.1:7380 master - 0 1401258245007 2 disconnected 8000-11999\n"
                + "c37ab8396be428403d4e55c0d317348be27ed973 127.0.0.1:7381 master - 111 1401258245007 222 connected 7000 12000 12002-16383\n";

        Partitions partitions1 = ClusterPartitionParser.parse(nodes1);
        Partitions partitions2 = ClusterPartitionParser.parse(nodes2);
        assertThat(isChanged(partitions1, partitions2)).isTrue();
    }

    @Test
    void shouldConsiderNodesWithoutSlotsUnchanged() {

        String nodes1 = "3d005a179da7d8dc1adae6409d47b39c369e992b 127.0.0.1:7380 slave - 0 1401258245007 2 disconnected\n"
                + "c37ab8396be428403d4e55c0d317348be27ed973 127.0.0.1:7381 master - 111 1401258245007 222 connected 7000 12000 12002-16383\n";

        Partitions partitions1 = ClusterPartitionParser.parse(nodes1);
        Partitions partitions2 = ClusterPartitionParser.parse(nodes1);
        assertThat(isChanged(partitions1, partitions2)).isFalse();
    }

    @Test
    void nodesShouldHaveSameSlots() {
        RedisClusterNode nodeA = createNode(1, 4, 36, 98);
        RedisClusterNode nodeB = createNode(4, 36, 1, 98);
        assertThat(nodeA.getSlots().containsAll(nodeB.getSlots())).isTrue();
        assertThat(nodeA.hasSameSlotsAs(nodeB)).isTrue();
    }

    @Test
    void nodesShouldNotHaveSameSlots() {
        RedisClusterNode nodeA = createNode(1, 4, 36, 99);
        RedisClusterNode nodeB = createNode(4, 36, 1, 100);
        assertThat(nodeA.getSlots().containsAll(nodeB.getSlots())).isFalse();
        assertThat(nodeA.hasSameSlotsAs(nodeB)).isFalse();
    }

    private RedisClusterNode createNode(Integer... slots) {
        RedisClusterNode node = new RedisClusterNode();
        node.setSlots(Arrays.asList(slots));
        return node;
    }

    @Test
    void isChangedFlagsChangedMasterToReplica() {
        String nodes1 = "3d005a179da7d8dc1adae6409d47b39c369e992b 127.0.0.1:7380 master - 0 1401258245007 2 disconnected 8000-11999\n"
                + "c37ab8396be428403d4e55c0d317348be27ed973 127.0.0.1:7381 master - 111 1401258245007 222 connected 7000 12000 12002-16383\n";

        String nodes2 = "3d005a179da7d8dc1adae6409d47b39c369e992b 127.0.0.1:7380 slave - 0 1401258245007 2 disconnected 8000-11999\n"
                + "c37ab8396be428403d4e55c0d317348be27ed973 127.0.0.1:7381 master - 111 1401258245007 222 connected 7000 12000 12002-16383\n";

        Partitions partitions1 = ClusterPartitionParser.parse(nodes1);
        Partitions partitions2 = ClusterPartitionParser.parse(nodes2);
        assertThat(isChanged(partitions1, partitions2)).isTrue();
    }

    void runTest(Map<String, Long> map, List<RedisClusterNodeSnapshot> expectation, List<RedisClusterNodeSnapshot> nodes) {

        for (RedisClusterNodeSnapshot node : nodes) {
            node.setLatencyNs(map.get(node.getNodeId()));
        }
        List<RedisClusterNode> result = TopologyComparators.sortByLatency((Iterable) nodes);

        assertThat(result).containsExactly(expectation.toArray(new RedisClusterNodeSnapshot[expectation.size()]));
    }

}
