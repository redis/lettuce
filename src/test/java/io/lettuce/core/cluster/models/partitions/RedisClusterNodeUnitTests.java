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
package io.lettuce.core.cluster.models.partitions;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.SlotHash;

/**
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
    void testEquality() {

        RedisClusterNode node = new RedisClusterNode();

        assertThat(node).isEqualTo(new RedisClusterNode());
        assertThat(node.hashCode()).isEqualTo(new RedisClusterNode().hashCode());

        node.setUri(new RedisURI());
        assertThat(node.hashCode()).isNotEqualTo(new RedisClusterNode());
    }

    @Test
    void testToString() {

        RedisClusterNode node = new RedisClusterNode();

        assertThat(node.toString()).contains(RedisClusterNode.class.getSimpleName());
    }

}
