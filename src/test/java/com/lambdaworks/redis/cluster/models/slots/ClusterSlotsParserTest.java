package com.lambdaworks.redis.cluster.models.slots;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.net.HostAndPort;

@SuppressWarnings("unchecked")
public class ClusterSlotsParserTest {

    @Test
    public void testEmpty() throws Exception {
        List<ClusterSlotRange> result = ClusterSlotsParser.parse(Lists.newArrayList());
        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    public void testOneString() throws Exception {
        List<ClusterSlotRange> result = ClusterSlotsParser.parse(Lists.newArrayList(""));
        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    public void testOneStringInList() throws Exception {
        List<?> list = ImmutableList.of(Lists.newArrayList("0"));
        List<ClusterSlotRange> result = ClusterSlotsParser.parse(list);
        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    public void testParse() throws Exception {
        List<?> list = ImmutableList.of(Lists.newArrayList("0", "1", Lists.newArrayList("1", "2")));
        List<ClusterSlotRange> result = ClusterSlotsParser.parse(list);
        assertThat(result).hasSize(1);

        assertThat(result.get(0).getMaster()).isNotNull();
        assertThat(result.get(0).getMasterNode()).isNotNull();
    }

    @Test
    public void testParseWithSlave() throws Exception {
        List<?> list = ImmutableList.of(Lists.newArrayList("0", "1", Lists.newArrayList("1", "2", "nodeId1"), Lists.newArrayList("1", 2, "nodeId2")));
        List<ClusterSlotRange> result = ClusterSlotsParser.parse(list);
        assertThat(result).hasSize(1);
        ClusterSlotRange clusterSlotRange = result.get(0);

        assertThat(clusterSlotRange.getMaster()).isNotNull();
        assertThat(clusterSlotRange.getMaster().getHostText()).isEqualTo("1");
        assertThat(clusterSlotRange.getMaster().getPort()).isEqualTo(2);

        RedisClusterNode masterNode = clusterSlotRange.getMasterNode();
        assertThat(masterNode).isNotNull();
        assertThat(masterNode.getNodeId()).isEqualTo("nodeId1");
        assertThat(masterNode.getUri().getHost()).isEqualTo("1");
        assertThat(masterNode.getUri().getPort()).isEqualTo(2);
        assertThat(masterNode.getFlags()).contains(RedisClusterNode.NodeFlag.MASTER);

        assertThat(clusterSlotRange.getSlaves()).hasSize(1);
        assertThat(clusterSlotRange.getSlaveNodes()).hasSize(1);

        HostAndPort slave = clusterSlotRange.getSlaves().get(0);
        assertThat(slave.getHostText()).isEqualTo("1");
        assertThat(slave.getPort()).isEqualTo(2);

        RedisClusterNode slaveNode = clusterSlotRange.getSlaveNodes().get(0);

        assertThat(slaveNode.getNodeId()).isEqualTo("nodeId2");
        assertThat(slaveNode.getSlaveOf()).isEqualTo("nodeId1");
        assertThat(slaveNode.getFlags()).contains(RedisClusterNode.NodeFlag.SLAVE);
    }

    @Test
    public void testHostAndPortConstructor() throws Exception {

        ClusterSlotRange clusterSlotRange = new ClusterSlotRange(100,200,HostAndPort.fromParts("1", 2), ImmutableList.of(
                HostAndPort.fromParts("1", 2)));

        RedisClusterNode masterNode = clusterSlotRange.getMasterNode();
        assertThat(masterNode).isNotNull();
        assertThat(masterNode.getNodeId()).isNull();
        assertThat(masterNode.getUri().getHost()).isEqualTo("1");
        assertThat(masterNode.getUri().getPort()).isEqualTo(2);
        assertThat(masterNode.getFlags()).contains(RedisClusterNode.NodeFlag.MASTER);

        assertThat(clusterSlotRange.getSlaves()).hasSize(1);
        assertThat(clusterSlotRange.getSlaveNodes()).hasSize(1);

        HostAndPort slave = clusterSlotRange.getSlaves().get(0);
        assertThat(slave.getHostText()).isEqualTo("1");
        assertThat(slave.getPort()).isEqualTo(2);

        RedisClusterNode slaveNode = clusterSlotRange.getSlaveNodes().get(0);

        assertThat(slaveNode.getNodeId()).isNull();
        assertThat(slaveNode.getSlaveOf()).isNull();
        assertThat(slaveNode.getFlags()).contains(RedisClusterNode.NodeFlag.SLAVE);

    }

    @Test
    public void testParseWithSlaveAndNodeIds() throws Exception {
        List<?> list = ImmutableList.of(Lists.newArrayList("0", "1", Lists.newArrayList("1", "2"), Lists.newArrayList("1", 2)));
        List<ClusterSlotRange> result = ClusterSlotsParser.parse(list);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMaster()).isNotNull();
        assertThat(result.get(0).getSlaves()).hasSize(1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseInvalidMaster() throws Exception {
        List<?> list = ImmutableList.of(Lists.newArrayList("0", "1", Lists.newArrayList("1")));
        ClusterSlotsParser.parse(list);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseInvalidMaster2() throws Exception {
        List<?> list = ImmutableList.of(Lists.newArrayList("0", "1", ""));
        ClusterSlotsParser.parse(list);
    }

    @Test
    public void testModel() throws Exception {

        ClusterSlotRange range = new ClusterSlotRange();
        range.setFrom(1);
        range.setTo(2);
        range.setSlaves(Lists.<HostAndPort> newArrayList());
        range.setMaster(HostAndPort.fromHost("localhost"));

        assertThat(range.toString()).contains(ClusterSlotRange.class.getSimpleName());
    }
}
