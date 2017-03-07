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
package io.lettuce.core.cluster.models.partitions;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;

import org.junit.Test;

import io.lettuce.core.RedisURI;

/**
 * @author Mark Paluch
 */
public class PartitionsTest {

    private RedisClusterNode node1 = new RedisClusterNode(RedisURI.create("localhost", 6379), "a", true, "", 0, 0, 0,
            Arrays.asList(1, 2, 3), new HashSet<>());
    private RedisClusterNode node2 = new RedisClusterNode(RedisURI.create("localhost", 6380), "b", true, "", 0, 0, 0,
            Arrays.asList(4, 5, 6), new HashSet<>());

    @Test
    public void contains() throws Exception {

        Partitions partitions = new Partitions();
        partitions.add(node1);

        assertThat(partitions.contains(node1)).isTrue();
        assertThat(partitions.contains(node2)).isFalse();
    }

    @Test
    public void containsUsesReadView() throws Exception {

        Partitions partitions = new Partitions();
        partitions.getPartitions().add(node1);

        assertThat(partitions.contains(node1)).isFalse();
        partitions.updateCache();
        assertThat(partitions.contains(node1)).isTrue();
    }

    @Test
    public void containsAll() throws Exception {

        Partitions partitions = new Partitions();
        partitions.add(node1);
        partitions.add(node2);

        assertThat(partitions.containsAll(Arrays.asList(node1, node2))).isTrue();
    }

    @Test
    public void containsAllUsesReadView() throws Exception {

        Partitions partitions = new Partitions();
        partitions.getPartitions().add(node1);

        assertThat(partitions.containsAll(Arrays.asList(node1))).isFalse();
        partitions.updateCache();
        assertThat(partitions.containsAll(Arrays.asList(node1))).isTrue();
    }

    @Test
    public void add() throws Exception {

        Partitions partitions = new Partitions();
        partitions.add(node1);
        partitions.add(node2);

        assertThat(partitions.getPartitionBySlot(1)).isEqualTo(node1);
        assertThat(partitions.getPartitionBySlot(5)).isEqualTo(node2);
    }

    @Test
    public void addPartitionClearsCache() throws Exception {

        Partitions partitions = new Partitions();
        partitions.addPartition(node1);

        assertThat(partitions.getPartitionBySlot(1)).isNull();
    }

    @Test
    public void addAll() throws Exception {

        Partitions partitions = new Partitions();
        partitions.addAll(Arrays.asList(node1, node2));

        assertThat(partitions.getPartitionBySlot(1)).isEqualTo(node1);
        assertThat(partitions.getPartitionBySlot(5)).isEqualTo(node2);
    }

    @Test
    public void getPartitionBySlot() throws Exception {

        Partitions partitions = new Partitions();

        assertThat(partitions.getPartitionBySlot(1)).isNull();

        partitions.add(node1);
        assertThat(partitions.getPartitionBySlot(1)).isEqualTo(node1);
    }

    @Test
    public void remove() throws Exception {

        Partitions partitions = new Partitions();
        partitions.addAll(Arrays.asList(node1, node2));
        partitions.remove(node1);

        assertThat(partitions.getPartitionBySlot(1)).isNull();
        assertThat(partitions.getPartitionBySlot(5)).isEqualTo(node2);
    }

    @Test
    public void removeAll() throws Exception {

        Partitions partitions = new Partitions();
        partitions.addAll(Arrays.asList(node1, node2));
        partitions.removeAll(Arrays.asList(node1));

        assertThat(partitions.getPartitionBySlot(1)).isNull();
        assertThat(partitions.getPartitionBySlot(5)).isEqualTo(node2);
    }

    @Test
    public void clear() throws Exception {

        Partitions partitions = new Partitions();
        partitions.addAll(Arrays.asList(node1, node2));
        partitions.clear();

        assertThat(partitions.getPartitionBySlot(1)).isNull();
        assertThat(partitions.getPartitionBySlot(5)).isNull();
    }

    @Test
    public void retainAll() throws Exception {

        Partitions partitions = new Partitions();
        partitions.addAll(Arrays.asList(node1, node2));
        partitions.retainAll(Arrays.asList(node2));

        assertThat(partitions.getPartitionBySlot(1)).isNull();
        assertThat(partitions.getPartitionBySlot(5)).isEqualTo(node2);
    }

    @Test
    public void toArray() throws Exception {

        Partitions partitions = new Partitions();
        partitions.addAll(Arrays.asList(node1, node2));

        assertThat(partitions.toArray()).contains(node1, node2);
    }

    @Test
    public void toArrayUsesReadView() throws Exception {

        Partitions partitions = new Partitions();
        partitions.getPartitions().addAll(Arrays.asList(node1, node2));

        assertThat(partitions.toArray()).doesNotContain(node1, node2);
        partitions.updateCache();
        assertThat(partitions.toArray()).contains(node1, node2);
    }

    @Test
    public void toArray2() throws Exception {

        Partitions partitions = new Partitions();
        partitions.addAll(Arrays.asList(node1, node2));

        assertThat(partitions.toArray(new RedisClusterNode[2])).contains(node1, node2);
    }

    @Test
    public void toArray2UsesReadView() throws Exception {

        Partitions partitions = new Partitions();
        partitions.getPartitions().addAll(Arrays.asList(node1, node2));

        assertThat(partitions.toArray(new RedisClusterNode[2])).doesNotContain(node1, node2);

        partitions.updateCache();

        assertThat(partitions.toArray(new RedisClusterNode[2])).contains(node1, node2);
    }

    @Test
    public void getPartitionByNodeId() throws Exception {

        Partitions partitions = new Partitions();
        partitions.add(node1);
        partitions.add(node2);

        assertThat(partitions.getPartitionByNodeId("a")).isEqualTo(node1);
        assertThat(partitions.getPartitionByNodeId("c")).isNull();
    }

    @Test
    public void reload() throws Exception {

        RedisClusterNode other = new RedisClusterNode(RedisURI.create("localhost", 6666), "c", true, "", 0, 0, 0,
                Arrays.asList(1, 2, 3, 4, 5, 6), new HashSet<>());

        Partitions partitions = new Partitions();
        partitions.add(other);

        partitions.reload(Arrays.asList(node1, node1));

        assertThat(partitions.getPartitionByNodeId("a")).isEqualTo(node1);
        assertThat(partitions.getPartitionBySlot(1)).isEqualTo(node1);
    }

    @Test
    public void reloadEmpty() throws Exception {

        Partitions partitions = new Partitions();
        partitions.reload(Arrays.asList());

        assertThat(partitions.getPartitionBySlot(1)).isNull();
    }

    @Test
    public void isEmpty() throws Exception {

        Partitions partitions = new Partitions();
        partitions.add(node1);

        assertThat(partitions.isEmpty()).isFalse();
    }

    @Test
    public void size() throws Exception {

        Partitions partitions = new Partitions();
        partitions.add(node1);

        assertThat(partitions.size()).isEqualTo(1);
    }

    @Test
    public void sizeUsesReadView() throws Exception {

        Partitions partitions = new Partitions();
        partitions.getPartitions().add(node1);

        assertThat(partitions.size()).isEqualTo(0);

        partitions.updateCache();

        assertThat(partitions.size()).isEqualTo(1);
    }

    @Test
    public void getPartition() throws Exception {

        Partitions partitions = new Partitions();
        partitions.add(node1);

        assertThat(partitions.getPartition(0)).isEqualTo(node1);
    }

    @Test
    public void iterator() throws Exception {

        Partitions partitions = new Partitions();
        partitions.add(node1);

        assertThat(partitions.iterator().next()).isEqualTo(node1);
    }

    @Test
    public void iteratorUsesReadView() throws Exception {

        Partitions partitions = new Partitions();
        partitions.getPartitions().add(node1);

        assertThat(partitions.iterator().hasNext()).isFalse();
        partitions.updateCache();

        assertThat(partitions.iterator().hasNext()).isTrue();
    }

    @Test
    public void iteratorIsSafeDuringUpdate() throws Exception {

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
    public void testToString() throws Exception {

        Partitions partitions = new Partitions();
        partitions.add(node1);

        assertThat(partitions.toString()).startsWith("Partitions [");
    }
}
