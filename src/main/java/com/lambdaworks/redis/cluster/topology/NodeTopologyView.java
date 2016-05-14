package com.lambdaworks.redis.cluster.topology;

import java.util.concurrent.ExecutionException;

import com.lambdaworks.redis.RedisFuture;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.cluster.models.partitions.ClusterPartitionParser;
import com.lambdaworks.redis.cluster.models.partitions.Partitions;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;

/**
 * @author Mark Paluch
 */
class NodeTopologyView {

    private final boolean available;
    private final RedisURI redisURI;

    private Partitions partitions;
    private final int connectedClients;

    private final long latency;
    private final String clusterNodes;

    private final String clientList;

    NodeTopologyView(RedisURI redisURI) {

        this.available = false;
        this.redisURI = redisURI;
        this.partitions = new Partitions();
        this.connectedClients = 0;
        this.clusterNodes = null;
        this.clientList = null;
        this.latency = 0;
    }

    NodeTopologyView(RedisURI redisURI, String clusterNodes, String clientList, long latency) {

        this.available = true;
        this.redisURI = redisURI;

        this.partitions = ClusterPartitionParser.parse(clusterNodes);
        this.connectedClients = getClients(clientList);

        this.clusterNodes = clusterNodes;
        this.clientList = clientList;
        this.latency = latency;

        getOwnPartition().setUri(redisURI);
    }

    static NodeTopologyView from(RedisURI redisURI, Requests clusterNodesRequests, Requests clientListRequests)
            throws ExecutionException, InterruptedException {

        TimedAsyncCommand<String, String, String> nodes = clusterNodesRequests.getRequest(redisURI);
        TimedAsyncCommand<String, String, String> clients = clientListRequests.getRequest(redisURI);

        if (resultAvailable(nodes) && resultAvailable(clients)) {
            return new NodeTopologyView(redisURI, nodes.get(), clients.get(), nodes.duration());
        }
        return new NodeTopologyView(redisURI);
    }

    static boolean resultAvailable(RedisFuture<?> redisFuture) {

        if (redisFuture != null && redisFuture.isDone() && !redisFuture.isCancelled()) {
            return true;
        }

        return false;
    }

    private int getClients(String rawClientsOutput) {
        return rawClientsOutput.trim().split("\\n").length;
    }

    long getLatency() {
        return latency;
    }

    boolean isAvailable() {
        return available;
    }

    Partitions getPartitions() {
        return partitions;
    }

    int getConnectedClients() {
        return connectedClients;
    }

    String getNodeId() {
        return getOwnPartition().getNodeId();
    }

    RedisURI getRedisURI() {
        return getOwnPartition().getUri();
    }

    private RedisClusterNode getOwnPartition() {
        for (RedisClusterNode partition : partitions) {
            if (partition.is(RedisClusterNode.NodeFlag.MYSELF)) {
                return partition;
            }
        }

        throw new IllegalStateException("Cannot determine own partition");
    }

    String getClientList() {
        return clientList;
    }

    String getClusterNodes() {
        return clusterNodes;
    }

    void setPartitions(Partitions partitions) {
        this.partitions = partitions;
    }
}
