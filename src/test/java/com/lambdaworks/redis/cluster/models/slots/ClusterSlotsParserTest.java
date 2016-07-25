package com.lambdaworks.redis.cluster.models.slots;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;
import com.lambdaworks.redis.internal.LettuceLists;

@SuppressWarnings("unchecked")
public class ClusterSlotsParserTest {

    @Test
    public void testEmpty() throws Exception {
        List<ClusterSlotRange> result = ClusterSlotsParser.parse(new ArrayList<>());
        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    public void testOneString() throws Exception {
        List<ClusterSlotRange> result = ClusterSlotsParser.parse(LettuceLists.newList(""));
        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    public void testOneStringInList() throws Exception {
        List<?> list = Arrays.asList(LettuceLists.newList("0"));
        List<ClusterSlotRange> result = ClusterSlotsParser.parse(list);
        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    public void testParse() throws Exception {
        List<?> list = Arrays.asList(LettuceLists.newList("0", "1", LettuceLists.newList("1", "2")));
        List<ClusterSlotRange> result = ClusterSlotsParser.parse(list);
        assertThat(result).hasSize(1);

        assertThat(result.get(0).getMasterNode()).isNotNull();
    }

    @Test
    public void testParseWithSlave() throws Exception {
        List<?> list = Arrays.asList(LettuceLists.newList("100", "200", LettuceLists.newList("1", "2", "nodeId1"),
                LettuceLists.newList("1", 2, "nodeId2")));
        List<ClusterSlotRange> result = ClusterSlotsParser.parse(list);
        assertThat(result).hasSize(1);
        ClusterSlotRange clusterSlotRange = result.get(0);

        RedisClusterNode masterNode = clusterSlotRange.getMasterNode();
        assertThat(masterNode).isNotNull();
        assertThat(masterNode.getNodeId()).isEqualTo("nodeId1");
        assertThat(masterNode.getUri().getHost()).isEqualTo("1");
        assertThat(masterNode.getUri().getPort()).isEqualTo(2);
        assertThat(masterNode.getFlags()).contains(RedisClusterNode.NodeFlag.MASTER);
        assertThat(masterNode.getSlots()).contains(100, 101, 199, 200);
        assertThat(masterNode.getSlots()).doesNotContain(99, 201);
        assertThat(masterNode.getSlots()).hasSize(101);

        assertThat(clusterSlotRange.getSlaveNodes()).hasSize(1);

        RedisClusterNode slaveNode = clusterSlotRange.getSlaveNodes().get(0);

        assertThat(slaveNode.getNodeId()).isEqualTo("nodeId2");
        assertThat(slaveNode.getSlaveOf()).isEqualTo("nodeId1");
        assertThat(slaveNode.getFlags()).contains(RedisClusterNode.NodeFlag.SLAVE);
    }

    @Test
    public void testSameNode() throws Exception {
        List<?> list = Arrays.asList(
                LettuceLists.newList("100", "200", LettuceLists.newList("1", "2", "nodeId1"),
                        LettuceLists.newList("1", 2, "nodeId2")),
                LettuceLists.newList("200", "300", LettuceLists.newList("1", "2", "nodeId1"),
                        LettuceLists.newList("1", 2, "nodeId2")));

        List<ClusterSlotRange> result = ClusterSlotsParser.parse(list);
        assertThat(result).hasSize(2);

        assertThat(result.get(0).getMasterNode()).isSameAs(result.get(1).getMasterNode());

        RedisClusterNode masterNode = result.get(0).getMasterNode();
        assertThat(masterNode).isNotNull();
        assertThat(masterNode.getNodeId()).isEqualTo("nodeId1");
        assertThat(masterNode.getUri().getHost()).isEqualTo("1");
        assertThat(masterNode.getUri().getPort()).isEqualTo(2);
        assertThat(masterNode.getFlags()).contains(RedisClusterNode.NodeFlag.MASTER);
        assertThat(masterNode.getSlots()).contains(100, 101, 199, 200, 203);
        assertThat(masterNode.getSlots()).doesNotContain(99, 301);
        assertThat(masterNode.getSlots()).hasSize(201);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseInvalidMaster() throws Exception {
        List<?> list = Arrays.asList(LettuceLists.newList("0", "1", LettuceLists.newList("1")));
        ClusterSlotsParser.parse(list);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseInvalidMaster2() throws Exception {
        List<?> list = Arrays.asList(LettuceLists.newList("0", "1", ""));
        ClusterSlotsParser.parse(list);
    }

    @Test
    public void testModel() throws Exception {

        ClusterSlotRange range = new ClusterSlotRange();
        range.setFrom(1);
        range.setTo(2);

        assertThat(range.toString()).contains(ClusterSlotRange.class.getSimpleName());
    }
}
