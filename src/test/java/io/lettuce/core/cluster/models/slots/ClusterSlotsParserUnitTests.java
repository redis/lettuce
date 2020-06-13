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
package io.lettuce.core.cluster.models.slots;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.internal.LettuceLists;

@SuppressWarnings("unchecked")
class ClusterSlotsParserUnitTests {

    @Test
    void testEmpty() {
        List<ClusterSlotRange> result = ClusterSlotsParser.parse(new ArrayList<>());
        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    void testOneString() {
        List<ClusterSlotRange> result = ClusterSlotsParser.parse(LettuceLists.newList(""));
        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    void testOneStringInList() {
        List<?> list = Arrays.asList(LettuceLists.newList("0"));
        List<ClusterSlotRange> result = ClusterSlotsParser.parse(list);
        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    void testParse() {
        List<?> list = Arrays.asList(LettuceLists.newList("0", "1", LettuceLists.newList("1", "2")));
        List<ClusterSlotRange> result = ClusterSlotsParser.parse(list);
        assertThat(result).hasSize(1);

        assertThat(result.get(0).getUpstream()).isNotNull();
    }

    @Test
    void testParseWithReplica() {
        List<?> list = Arrays.asList(LettuceLists.newList("100", "200", LettuceLists.newList("1", "2", "nodeId1"),
                LettuceLists.newList("1", 2, "nodeId2")));
        List<ClusterSlotRange> result = ClusterSlotsParser.parse(list);
        assertThat(result).hasSize(1);
        ClusterSlotRange clusterSlotRange = result.get(0);

        RedisClusterNode upstreamNode = clusterSlotRange.getUpstream();
        assertThat(upstreamNode).isNotNull();
        assertThat(upstreamNode.getNodeId()).isEqualTo("nodeId1");
        assertThat(upstreamNode.getUri().getHost()).isEqualTo("1");
        assertThat(upstreamNode.getUri().getPort()).isEqualTo(2);
        assertThat(upstreamNode.is(RedisClusterNode.NodeFlag.MASTER)).isTrue();
        assertThat(upstreamNode.getSlots()).contains(100, 101, 199, 200);
        assertThat(upstreamNode.getSlots()).doesNotContain(99, 201);
        assertThat(upstreamNode.getSlots()).hasSize(101);

        assertThat(clusterSlotRange.getSlaveNodes()).hasSize(1);

        RedisClusterNode replica = clusterSlotRange.getReplicaNodes().get(0);

        assertThat(replica.getNodeId()).isEqualTo("nodeId2");
        assertThat(replica.getSlaveOf()).isEqualTo("nodeId1");
        assertThat(replica.is(RedisClusterNode.NodeFlag.SLAVE)).isTrue();
    }

    @Test
    void testSameNode() {
        List<?> list = Arrays.asList(
                LettuceLists.newList("100", "200", LettuceLists.newList("1", "2", "nodeId1"),
                        LettuceLists.newList("1", 2, "nodeId2")),
                LettuceLists.newList("200", "300", LettuceLists.newList("1", "2", "nodeId1"),
                        LettuceLists.newList("1", 2, "nodeId2")));

        List<ClusterSlotRange> result = ClusterSlotsParser.parse(list);
        assertThat(result).hasSize(2);

        assertThat(result.get(0).getUpstream()).isSameAs(result.get(1).getUpstream());

        RedisClusterNode upstreamNode = result.get(0).getUpstream();
        assertThat(upstreamNode).isNotNull();
        assertThat(upstreamNode.getNodeId()).isEqualTo("nodeId1");
        assertThat(upstreamNode.getUri().getHost()).isEqualTo("1");
        assertThat(upstreamNode.getUri().getPort()).isEqualTo(2);
        assertThat(upstreamNode.is(RedisClusterNode.NodeFlag.MASTER)).isTrue();
        assertThat(upstreamNode.getSlots()).contains(100, 101, 199, 200, 203);
        assertThat(upstreamNode.getSlots()).doesNotContain(99, 301);
        assertThat(upstreamNode.getSlots()).hasSize(201);
    }

    @Test
    void testParseInvalidUpstream() {

        List<?> list = Arrays.asList(LettuceLists.newList("0", "1", LettuceLists.newList("1")));
        assertThatThrownBy(() -> ClusterSlotsParser.parse(list)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testParseInvalidUpstream2() {
        List<?> list = Arrays.asList(LettuceLists.newList("0", "1", ""));
        assertThatThrownBy(() -> ClusterSlotsParser.parse(list)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testModel() {

        ClusterSlotRange range = new ClusterSlotRange();
        range.setFrom(1);
        range.setTo(2);

        assertThat(range.toString()).contains(ClusterSlotRange.class.getSimpleName());
    }
}
