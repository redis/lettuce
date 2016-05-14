package com.lambdaworks.redis.cluster.topology;

import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;

/**
 * @author Mark Paluch
 */
class RedisClusterNodeSnapshot extends RedisClusterNode {

    private Long latencyNs;
    private Integer connectedClients;

    public RedisClusterNodeSnapshot() {
    }

    public RedisClusterNodeSnapshot(RedisClusterNode redisClusterNode) {
        super(redisClusterNode);
    }

    Long getLatencyNs() {
        return latencyNs;
    }

    void setLatencyNs(Long latencyNs) {
        this.latencyNs = latencyNs;
    }

    Integer getConnectedClients() {
        return connectedClients;
    }

    void setConnectedClients(Integer connectedClients) {
        this.connectedClients = connectedClients;
    }
}
