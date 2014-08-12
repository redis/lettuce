package com.lambdaworks.redis.cluster;

import com.lambdaworks.redis.RedisClusterConnection;
import com.lambdaworks.redis.cluster.models.partitions.ClusterPartitionParser;
import com.lambdaworks.redis.cluster.models.partitions.Partitions;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 09.08.14 19:50
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
