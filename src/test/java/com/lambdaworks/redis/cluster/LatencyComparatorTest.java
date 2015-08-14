package com.lambdaworks.redis.cluster;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class LatencyComparatorTest {

    private ClusterTopologyRefresh.LatencyComparator sut;

    private RedisClusterNode node1 = createNode("1");
    private RedisClusterNode node2 = createNode("2");
    private RedisClusterNode node3 = createNode("3");

    private static RedisClusterNode createNode(String nodeId) {
        RedisClusterNode result = new RedisClusterNode();
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

    protected void runTest(Map<String, Long> map, List<RedisClusterNode> expectation, List<RedisClusterNode> nodes) {
        sut = new ClusterTopologyRefresh.LatencyComparator(map);
        Collections.sort(nodes, sut);

        assertThat(nodes).containsExactly(expectation.toArray(new RedisClusterNode[expectation.size()]));
    }
}