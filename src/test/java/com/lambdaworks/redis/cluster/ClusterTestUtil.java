package com.lambdaworks.redis.cluster;

import com.lambdaworks.redis.RedisClusterConnection;
import com.lambdaworks.redis.cluster.models.partitions.ClusterPartitionParser;
import com.lambdaworks.redis.cluster.models.partitions.Partitions;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;

/**
 * @author Mark Paluch
 * @since 3.0
 */
public class ClusterTestUtil {

    public static String getNodeId(RedisClusterConnection<String, String> connection) {
        RedisClusterNode ownPartition = getOwnPartition(connection);
        if (ownPartition != null) {
            return ownPartition.getNodeId();
        }

        return null;
    }

    public static RedisClusterNode getOwnPartition(RedisClusterConnection<String, String> connection) {
        Partitions partitions = ClusterPartitionParser.parse(connection.clusterNodes());

        for (RedisClusterNode partition : partitions) {
            if (partition.getFlags().contains(RedisClusterNode.NodeFlag.MYSELF)) {
                return partition;
            }
        }
        return null;
    }
}
