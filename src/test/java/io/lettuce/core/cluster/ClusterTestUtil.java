package io.lettuce.core.cluster;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.sync.RedisClusterCommands;
import io.lettuce.core.cluster.models.partitions.ClusterPartitionParser;
import io.lettuce.core.cluster.models.partitions.Partitions;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.test.RoutingInvocationHandler;

/**
 * @author Mark Paluch
 * @since 3.0
 */
public class ClusterTestUtil {

    /**
     * Retrieve the cluster node Id from the {@code connection}.
     *
     * @param connection
     * @return
     */
    public static String getNodeId(RedisClusterCommands<?, ?> connection) {
        RedisClusterNode ownPartition = getOwnPartition(connection);
        if (ownPartition != null) {
            return ownPartition.getNodeId();
        }

        return null;
    }

    /**
     * Retrieve the {@link RedisClusterNode} from the {@code connection}.
     *
     * @param connection
     * @return
     */
    public static RedisClusterNode getOwnPartition(RedisClusterCommands<?, ?> connection) {
        Partitions partitions = ClusterPartitionParser.parse(connection.clusterNodes());

        for (RedisClusterNode partition : partitions) {
            if (partition.getFlags().contains(RedisClusterNode.NodeFlag.MYSELF)) {
                return partition;
            }
        }
        return null;
    }

    /**
     * Flush databases of all cluster nodes.
     *
     * @param connection the cluster connection
     */
    public static void flushDatabaseOfAllNodes(StatefulRedisClusterConnection<?, ?> connection) {
        for (RedisClusterNode node : connection.getPartitions()) {
            try {
                connection.getConnection(node.getNodeId()).sync().flushall();
                connection.getConnection(node.getNodeId()).sync().flushdb();
            } catch (Exception o_O) {
                // ignore
            }
        }
    }

    /**
     * Return a stable key that hashes to {@code slot}. The full slot-to-key mapping is computed once and cached, so repeated
     * calls are cheap and always return the same key for the same slot — giving deterministic routing in cluster tests instead
     * of hoping that arbitrary keys spread across nodes.
     *
     * @param slot the target slot, in {@code [0, }{@link SlotHash#SLOT_COUNT}{@code )}.
     * @return a key {@code k} such that {@link SlotHash#getSlot(String) SlotHash.getSlot(k) == slot}.
     */
    public static String keyForSlot(int slot) {
        if (slot < 0 || slot >= SlotHash.SLOT_COUNT) {
            throw new IllegalArgumentException("Slot must be in [0, " + SlotHash.SLOT_COUNT + "), was " + slot);
        }
        return slotToKeyTable()[slot];
    }

    /**
     * Return a key that routes to {@code node}, by hashing to the first slot the node owns. Deterministic across runs. The node
     * must own at least one slot (i.e. be an upstream node with an assigned slot range).
     *
     * @param node the target node.
     * @return a key owned by {@code node}.
     */
    public static String keyForNode(RedisClusterNode node) {
        if (node.getSlots().isEmpty()) {
            throw new IllegalArgumentException("Node " + node.getNodeId() + " owns no slots; cannot route a key to it");
        }
        return keyForSlot(node.getSlots().get(0));
    }

    /**
     * Return the upstream (master) nodes of {@code connection}'s partition view that own at least one slot — i.e. the nodes a
     * key can be deterministically routed to via {@link #keyForNode(RedisClusterNode)}.
     *
     * @param connection the cluster connection.
     * @return the upstream nodes owning slots.
     */
    public static List<RedisClusterNode> upstreamNodesWithSlots(StatefulRedisClusterConnection<?, ?> connection) {

        List<RedisClusterNode> nodes = new ArrayList<>();
        for (RedisClusterNode node : connection.getPartitions()) {
            if (node.is(RedisClusterNode.NodeFlag.UPSTREAM) && !node.getSlots().isEmpty()) {
                nodes.add(node);
            }
        }
        return nodes;
    }

    private static volatile String[] slotToKey;

    private static String[] slotToKeyTable() {

        String[] table = slotToKey;
        if (table == null) {
            synchronized (ClusterTestUtil.class) {
                table = slotToKey;
                if (table == null) {
                    table = buildSlotToKeyTable();
                    slotToKey = table;
                }
            }
        }
        return table;
    }

    private static String[] buildSlotToKeyTable() {

        String[] table = new String[SlotHash.SLOT_COUNT];
        int found = 0;
        for (long candidate = 0; found < SlotHash.SLOT_COUNT; candidate++) {
            if (candidate > 100_000_000L) {
                throw new IllegalStateException("Unable to find keys covering all " + SlotHash.SLOT_COUNT + " slots");
            }
            String key = Long.toString(candidate);
            int slot = SlotHash.getSlot(key);
            if (table[slot] == null) {
                table[slot] = key;
                found++;
            }
        }
        return table;
    }

    /**
     * Create an API wrapper which exposes the {@link RedisCommands} API by using internally a cluster connection.
     *
     * @param connection
     * @return
     */
    public static RedisCommands<String, String> redisCommandsOverCluster(
            StatefulRedisClusterConnection<String, String> connection) {
        StatefulRedisClusterConnectionImpl clusterConnection = (StatefulRedisClusterConnectionImpl) connection;

        InvocationHandler h = new RoutingInvocationHandler(connection.async(), clusterConnection.syncInvocationHandler());
        return (RedisCommands<String, String>) Proxy.newProxyInstance(ClusterTestUtil.class.getClassLoader(),
                new Class[] { RedisCommands.class }, h);
    }

}
