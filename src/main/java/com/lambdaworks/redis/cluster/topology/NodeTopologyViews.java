package com.lambdaworks.redis.cluster.topology;

import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.cluster.models.partitions.Partitions;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;

import java.util.*;

/**
 * @author Mark Paluch
 */
class NodeTopologyViews {

    private List<NodeTopologyView> views = new ArrayList<>();

    public NodeTopologyViews(List<NodeTopologyView> views) {
        this.views = views;
    }

    /**
     * Return cluster node URI's using the topology query sources and partitions.
     *
     * @return
     */
    public Set<RedisURI> getClusterNodes() {

        Set<RedisURI> result = new HashSet<>();

        Map<String, RedisURI> knownUris = new HashMap<>();
        for (NodeTopologyView view : views) {
            knownUris.put(view.getNodeId(), view.getRedisURI());
        }

        for (NodeTopologyView view : views) {
            for (RedisClusterNode redisClusterNode : view.getPartitions()) {
                if (knownUris.containsKey(redisClusterNode.getNodeId())) {
                    result.add(knownUris.get(redisClusterNode.getNodeId()));
                } else {
                    result.add(redisClusterNode.getUri());
                }
            }
        }

        return result;
    }

    public Map<RedisURI, Partitions> toMap() {

        Map<RedisURI, Partitions> nodeSpecificViews = new TreeMap<>(TopologyComparators.RedisUriComparator.INSTANCE);

        for (NodeTopologyView view : views) {
            nodeSpecificViews.put(view.getRedisURI(), view.getPartitions());
        }

        return nodeSpecificViews;
    }
}
