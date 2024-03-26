package io.lettuce.core.cluster;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

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
     * Create an API wrapper which exposes the {@link RedisCommands} API by using internally a cluster connection.
     *
     * @param connection
     * @return
     */
    public static RedisCommands<String, String> redisCommandsOverCluster(
            StatefulRedisClusterConnection<String, String> connection) {
        StatefulRedisClusterConnectionImpl clusterConnection = (StatefulRedisClusterConnectionImpl) connection;

        InvocationHandler h = new RoutingInvocationHandler(connection.async(),
                clusterConnection.syncInvocationHandler());
        return (RedisCommands<String, String>) Proxy.newProxyInstance(ClusterTestUtil.class.getClassLoader(),
                new Class[] { RedisCommands.class }, h);
    }
}
