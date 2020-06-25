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

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;

import org.junit.jupiter.api.Test;

import io.lettuce.core.RedisURI;

/**
 * @author Mark Paluch
 */
class PartitionsUnitTests {

    private RedisClusterNode node1 = new RedisClusterNode(RedisURI.create("localhost", 6379), "a", true, "", 0, 0, 0,
            Arrays.asList(1, 2, 3), new HashSet<>());

    private RedisClusterNode node2 = new RedisClusterNode(RedisURI.create("localhost", 6380), "b", true, "", 0, 0, 0,
            Arrays.asList(4, 5, 6), new HashSet<>());

    @Test
    void contains() {

        Partitions partitions = new Partitions();
        partitions.add(node1);

        assertThat(partitions.contains(node1)).isTrue();
        assertThat(partitions.contains(node2)).isFalse();
    }

    @Test
    void containsUsesReadView() {

        Partitions partitions = new Partitions();
        partitions.getPartitions().add(node1);

        assertThat(partitions.contains(node1)).isFalse();
        partitions.updateCache();
        assertThat(partitions.contains(node1)).isTrue();
    }

    @Test
    void containsAll() {

        Partitions partitions = new Partitions();
        partitions.add(node1);
        partitions.add(node2);

        assertThat(partitions.containsAll(Arrays.asList(node1, node2))).isTrue();
    }

    @Test
    void containsAllUsesReadView() {

        Partitions partitions = new Partitions();
        partitions.getPartitions().add(node1);

        assertThat(partitions.containsAll(Arrays.asList(node1))).isFalse();
        partitions.updateCache();
        assertThat(partitions.containsAll(Arrays.asList(node1))).isTrue();
    }

    @Test
    void add() {

        Partitions partitions = new Partitions();
        partitions.add(node1);
        partitions.add(node2);

        assertThat(partitions.getPartitionBySlot(1)).isEqualTo(node1);
        assertThat(partitions.getPartitionBySlot(5)).isEqualTo(node2);
    }

    @Test
    void addPartitionClearsCache() {

        Partitions partitions = new Partitions();
        partitions.addPartition(node1);

        assertThat(partitions.getPartitionBySlot(1)).isNull();
    }

    @Test
    void addAll() {

        Partitions partitions = new Partitions();
        partitions.addAll(Arrays.asList(node1, node2));

        assertThat(partitions.getPartitionBySlot(1)).isEqualTo(node1);
        assertThat(partitions.getPartitionBySlot(5)).isEqualTo(node2);
    }

    @Test
    void getPartitionBySlot() {

        Partitions partitions = new Partitions();

        assertThat(partitions.getPartitionBySlot(1)).isNull();

        partitions.add(node1);
        assertThat(partitions.getPartitionBySlot(1)).isEqualTo(node1);
    }

    @Test
    void getPartitionByAlias() {

        Partitions partitions = new Partitions();
        node1.addAlias(RedisURI.create("foobar", 1234));
        partitions.add(node1);

        assertThat(partitions.getPartition(node1.getUri().getHost(), node1.getUri().getPort())).isEqualTo(node1);
        assertThat(partitions.getPartition("foobar", 1234)).isEqualTo(node1);
        assertThat(partitions.getPartition("unknown", 1234)).isNull();
    }

    @Test
    void remove() {

        Partitions partitions = new Partitions();
        partitions.addAll(Arrays.asList(node1, node2));
        partitions.remove(node1);

        assertThat(partitions.getPartitionBySlot(1)).isNull();
        assertThat(partitions.getPartitionBySlot(5)).isEqualTo(node2);
    }

    @Test
    void removeAll() {

        Partitions partitions = new Partitions();
        partitions.addAll(Arrays.asList(node1, node2));
        partitions.removeAll(Arrays.asList(node1));

        assertThat(partitions.getPartitionBySlot(1)).isNull();
        assertThat(partitions.getPartitionBySlot(5)).isEqualTo(node2);
    }

    @Test
    void clear() {

        Partitions partitions = new Partitions();
        partitions.addAll(Arrays.asList(node1, node2));
        partitions.clear();

        assertThat(partitions.getPartitionBySlot(1)).isNull();
        assertThat(partitions.getPartitionBySlot(5)).isNull();
    }

    @Test
    void retainAll() {

        Partitions partitions = new Partitions();
        partitions.addAll(Arrays.asList(node1, node2));
        partitions.retainAll(Arrays.asList(node2));

        assertThat(partitions.getPartitionBySlot(1)).isNull();
        assertThat(partitions.getPartitionBySlot(5)).isEqualTo(node2);
    }

    @Test
    void toArray() {

        Partitions partitions = new Partitions();
        partitions.addAll(Arrays.asList(node1, node2));

        assertThat(partitions.toArray()).contains(node1, node2);
    }

    @Test
    void toArrayUsesReadView() {

        Partitions partitions = new Partitions();
        partitions.getPartitions().addAll(Arrays.asList(node1, node2));

        assertThat(partitions.toArray()).doesNotContain(node1, node2);
        partitions.updateCache();
        assertThat(partitions.toArray()).contains(node1, node2);
    }

    @Test
    void toArray2() {

        Partitions partitions = new Partitions();
        partitions.addAll(Arrays.asList(node1, node2));

        assertThat(partitions.toArray(new RedisClusterNode[2])).contains(node1, node2);
    }

    @Test
    void toArray2UsesReadView() {

        Partitions partitions = new Partitions();
        partitions.getPartitions().addAll(Arrays.asList(node1, node2));

        assertThat(partitions.toArray(new RedisClusterNode[2])).doesNotContain(node1, node2);

        partitions.updateCache();

        assertThat(partitions.toArray(new RedisClusterNode[2])).contains(node1, node2);
    }

    @Test
    void getPartitionByNodeId() {

        Partitions partitions = new Partitions();
        partitions.add(node1);
        partitions.add(node2);

        assertThat(partitions.getPartitionByNodeId("a")).isEqualTo(node1);
        assertThat(partitions.getPartitionByNodeId("c")).isNull();
    }

    @Test
    void reload() {

        RedisClusterNode other = new RedisClusterNode(RedisURI.create("localhost", 6666), "c", true, "", 0, 0, 0,
                Arrays.asList(1, 2, 3, 4, 5, 6), new HashSet<>());

        Partitions partitions = new Partitions();
        partitions.add(other);

        partitions.reload(Arrays.asList(node1, node1));

        assertThat(partitions.getPartitionByNodeId("a")).isEqualTo(node1);
        assertThat(partitions.getPartitionBySlot(1)).isEqualTo(node1);
    }

    @Test
    void reloadEmpty() {

        Partitions partitions = new Partitions();
        partitions.reload(Arrays.asList());

        assertThat(partitions.getPartitionBySlot(1)).isNull();
    }

    @Test
    void isEmpty() {

        Partitions partitions = new Partitions();
        partitions.add(node1);

        assertThat(partitions.isEmpty()).isFalse();
    }

    @Test
    void size() {

        Partitions partitions = new Partitions();
        partitions.add(node1);

        assertThat(partitions.size()).isEqualTo(1);
    }

    @Test
    void sizeUsesReadView() {

        Partitions partitions = new Partitions();
        partitions.getPartitions().add(node1);

        assertThat(partitions.size()).isEqualTo(0);

        partitions.updateCache();

        assertThat(partitions.size()).isEqualTo(1);
    }

    @Test
    void getPartition() {

        Partitions partitions = new Partitions();
        partitions.add(node1);

        assertThat(partitions.getPartition(0)).isEqualTo(node1);
    }

    @Test
    void iterator() {

        Partitions partitions = new Partitions();
        partitions.add(node1);

        assertThat(partitions.iterator().next()).isEqualTo(node1);
    }

    @Test
    void iteratorUsesReadView() {

        Partitions partitions = new Partitions();
        partitions.getPartitions().add(node1);

        assertThat(partitions.iterator().hasNext()).isFalse();
        partitions.updateCache();

        assertThat(partitions.iterator().hasNext()).isTrue();
    }

    @Test
    void iteratorIsSafeDuringUpdate() {

        Partitions partitions = new Partitions();
        partitions.add(node1);
        partitions.add(node2);

        Iterator<RedisClusterNode> iterator = partitions.iterator();

        partitions.remove(node2);

        assertThat(iterator.hasNext()).isTrue();
        assertThat(iterator.next()).isEqualTo(node1);
        assertThat(iterator.next()).isEqualTo(node2);

        iterator = partitions.iterator();

        partitions.remove(node2);

        assertThat(iterator.hasNext()).isTrue();
        assertThat(iterator.next()).isEqualTo(node1);
        assertThat(iterator.hasNext()).isFalse();
    }

    @Test
    void testToString() {

        Partitions partitions = new Partitions();
        partitions.add(node1);

        assertThat(partitions.toString()).startsWith("Partitions [");
    }

}
