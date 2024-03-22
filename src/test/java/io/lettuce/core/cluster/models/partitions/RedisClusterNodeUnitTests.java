/*
 * Copyright 2011-2024 the original author or authors.
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
package io.lettuce.core.cluster.models.partitions;

import static org.assertj.core.api.Assertions.*;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.SlotHash;

/**
 * Unit tests for {@link RedisClusterNode}.
 *
 * @author Mark Paluch
 */
class RedisClusterNodeUnitTests {

    @Test
    void shouldCopyNode() {

        RedisClusterNode node = new RedisClusterNode();
        node.setSlots(Arrays.asList(1, 2, 3, SlotHash.SLOT_COUNT - 1));
        node.addAlias(RedisURI.create("foo", 6379));

        RedisClusterNode copy = new RedisClusterNode(node);

        assertThat(copy.getSlots()).containsExactly(1, 2, 3, SlotHash.SLOT_COUNT - 1);
        assertThat(copy.hasSlot(1)).isTrue();
        assertThat(copy.hasSlot(SlotHash.SLOT_COUNT - 1)).isTrue();
        assertThat(copy.getAliases()).contains(RedisURI.create("foo", 6379));
    }

    @Test
    void hasSameSlotsAs() {

        // When both nodes have the same slots
        RedisClusterNode nodeWithSameSlots1 = new RedisClusterNode();
        nodeWithSameSlots1.setSlots(Arrays.asList(1, 2, 3, SlotHash.SLOT_COUNT - 1));

        RedisClusterNode nodeWithSameSlots2 = new RedisClusterNode();
        nodeWithSameSlots2.setSlots(Arrays.asList(1, 2, 3, SlotHash.SLOT_COUNT - 1));

        assertThat(nodeWithSameSlots1.hasSameSlotsAs(nodeWithSameSlots2)).isTrue();

        // When nodes have different slots
        RedisClusterNode nodeWithDifferentSlots1 = new RedisClusterNode();
        nodeWithDifferentSlots1.setSlots(Arrays.asList(1, 2, 3, SlotHash.SLOT_COUNT - 1));

        RedisClusterNode nodeWithDifferentSlots2 = new RedisClusterNode();
        nodeWithDifferentSlots2.setSlots(Arrays.asList(1, 2, 3, SlotHash.SLOT_COUNT - 2));

        assertThat(nodeWithDifferentSlots1.hasSameSlotsAs(nodeWithDifferentSlots2)).isFalse();

        // When both nodes' slots are empty
        RedisClusterNode emptySlotsNode1 = new RedisClusterNode();
        emptySlotsNode1.setSlots(List.of());

        RedisClusterNode emptySlotsNode2 = new RedisClusterNode();
        emptySlotsNode2.setSlots(List.of());

        assertThat(emptySlotsNode1.hasSameSlotsAs(emptySlotsNode2)).isTrue();

        // When one node's slots are empty and the other node's slots are not empty
        RedisClusterNode oneEmptySlotsNode = new RedisClusterNode();
        oneEmptySlotsNode.setSlots(List.of());

        RedisClusterNode nonEmptySlotsNode = new RedisClusterNode();
        nonEmptySlotsNode.setSlots(Arrays.asList(1, 2, 3));

        assertThat(oneEmptySlotsNode.hasSameSlotsAs(nonEmptySlotsNode)).isFalse();
    }

    @Test // considers nodeId only
    void testEquality() {

        RedisClusterNode node = RedisClusterNode.of("1");

        assertThat(node).isEqualTo(RedisClusterNode.of("1"));
        assertThat(node).hasSameHashCodeAs(RedisClusterNode.of("1"));

        node.setUri(RedisURI.create("127.0.0.1", 1));
        assertThat(node).hasSameHashCodeAs(RedisClusterNode.of("1"));
        assertThat(node).isEqualTo(RedisClusterNode.of("1"));

        assertThat(node).doesNotHaveSameHashCodeAs(RedisClusterNode.of("2"));
        assertThat(node).isNotEqualTo(RedisClusterNode.of("2"));
    }

    @Test
    void testToString() {

        RedisClusterNode node = new RedisClusterNode();

        assertThat(node.toString()).contains(RedisClusterNode.class.getSimpleName());
    }
}
