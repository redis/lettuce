package com.lambdaworks.redis.cluster;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import com.lambdaworks.redis.cluster.ClusterTopologyRefresh.RedisClusterNodeSnapshot;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

/**
 * @author Mark Paluch
 */
public class LatencyComparatorTest {

    private ClusterTopologyRefresh.LatencyComparator sut;

    private RedisClusterNodeSnapshot node1 = createNode("1");
    private RedisClusterNodeSnapshot node2 = createNode("2");
    private RedisClusterNodeSnapshot node3 = createNode("3");

    private static RedisClusterNodeSnapshot createNode(String nodeId) {
        RedisClusterNodeSnapshot result = new RedisClusterNodeSnapshot();
        result.setNodeId(nodeId);
        return result;
    }

    @Test
    public void latenciesForAllNodes() throws Exception {

        Map<String, Long> map = ImmutableMap.of(node1.getNodeId(), 1L, node2.getNodeId(), 2L, node3.getNodeId(), 3L);

        runTest(map, newArrayList(node1, node2, node3), newArrayList(node3, node1, node2));
        runTest(map, newArrayList(node1, node2, node3), newArrayList(node1, node2, node3));
        runTest(map, newArrayList(node1, node2, node3), newArrayList(node3, node2, node1));
    }

    @Test
    public void latenciesForTwoNodes_N1_N2() throws Exception {

        Map<String, Long> map = ImmutableMap.of(node1.getNodeId(), 1L, node2.getNodeId(), 2L);

        runTest(map, newArrayList(node1, node2, node3), newArrayList(node3, node1, node2));
        runTest(map, newArrayList(node1, node2, node3), newArrayList(node1, node2, node3));
        runTest(map, newArrayList(node1, node2, node3), newArrayList(node3, node2, node1));
    }

    @Test
    public void latenciesForTwoNodes_N2_N3() throws Exception {

        Map<String, Long> map = ImmutableMap.of(node3.getNodeId(), 1L, node2.getNodeId(), 2L);

        runTest(map, newArrayList(node3, node2, node1), newArrayList(node3, node1, node2));
        runTest(map, newArrayList(node3, node2, node1), newArrayList(node1, node2, node3));
        runTest(map, newArrayList(node3, node2, node1), newArrayList(node3, node2, node1));
    }

    @Test
    public void latenciesForOneNode() throws Exception {

        Map<String, Long> map = ImmutableMap.of(node2.getNodeId(), 2L);

        runTest(map, newArrayList(node2, node3, node1), newArrayList(node3, node1, node2));
        runTest(map, newArrayList(node2, node1, node3), newArrayList(node1, node2, node3));
        runTest(map, newArrayList(node2, node3, node1), newArrayList(node3, node2, node1));
    }

    @Test(expected = AssertionError.class)
    public void shouldFail() throws Exception {

        Map<String, Long> map = ImmutableMap.of(node2.getNodeId(), 2L);

        runTest(map, newArrayList(node2, node1, node3), newArrayList(node3, node1, node2));
    }

    protected void runTest(Map<String, Long> map, List<RedisClusterNodeSnapshot> expectation, List<RedisClusterNodeSnapshot> nodes) {

        for (RedisClusterNodeSnapshot node : nodes) {
            node.setLatencyNs(map.get(node.getNodeId()));
        }
        List<RedisClusterNode> result = ClusterTopologyRefresh.sortByLatency((Iterable) nodes);

        assertThat(result).containsExactly(expectation.toArray(new RedisClusterNodeSnapshot[expectation.size()]));
    }
}
