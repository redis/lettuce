package io.lettuce.core.cluster.topology;

import io.lettuce.core.cluster.models.partitions.RedisClusterNode;

/**
 * @author Mark Paluch
 */
@SuppressWarnings("serial")
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
