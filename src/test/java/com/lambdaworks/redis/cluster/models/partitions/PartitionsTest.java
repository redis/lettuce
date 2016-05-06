package com.lambdaworks.redis.cluster.models.partitions;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import org.junit.Test;

import com.lambdaworks.redis.RedisURI;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Paluch
 */
public class PartitionsTest {

    private RedisClusterNode node1 = new RedisClusterNode(RedisURI.create("redis://localhost:6379"), "a", true, "", 0, 0, 0,
            Arrays.asList(1, 2, 3), new HashSet<RedisClusterNode.NodeFlag>());
    private RedisClusterNode node2 = new RedisClusterNode(RedisURI.create("redis://localhost:6379"), "b", true, "", 0, 0, 0,
            Arrays.asList(4, 5, 6), new HashSet<RedisClusterNode.NodeFlag>());

    @Test
    public void contains() throws Exception {

        Partitions partitions = new Partitions();
        partitions.add(node1);

        assertThat(partitions.contains(node1)).isTrue();
        assertThat(partitions.contains(node2)).isFalse();
    }

    @Test
    public void containsAll() throws Exception {

        Partitions partitions = new Partitions();
        partitions.add(node1);
        partitions.add(node2);

        assertThat(partitions.containsAll(Arrays.asList(node1, node2))).isTrue();
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
    public void toArray2() throws Exception {

        Partitions partitions = new Partitions();
        partitions.addAll(Arrays.asList(node1, node2));

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

        RedisClusterNode other = new RedisClusterNode(RedisURI.create("redis://localhost:6666"), "c", true, "", 0, 0, 0,
                Arrays.asList(1, 2, 3, 4, 5, 6), new HashSet<RedisClusterNode.NodeFlag>());

        Partitions partitions = new Partitions();
        partitions.add(other);

        partitions.reload(Arrays.asList(node1, node1));

        assertThat(partitions.getPartitionByNodeId("a")).isEqualTo(node1);
        assertThat(partitions.getPartitionBySlot(1)).isEqualTo(node1);
    }

    @Test
    public void reloadEmpty() throws Exception {

        Partitions partitions = new Partitions();
        partitions.reload(new ArrayList<RedisClusterNode>());

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
    public void testToString() throws Exception {

        Partitions partitions = new Partitions();
        partitions.add(node1);

        assertThat(partitions.toString()).startsWith("Partitions [");
    }
}
