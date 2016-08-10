package com.lambdaworks.redis.cluster;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.cluster.api.NodeSelectionSupport;
import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;

/**
 * Abstract base class to support node selections. A node selection represents a set of Redis Cluster nodes and allows command
 * execution on the selected cluster nodes.
 * 
 * @param <API> API type.
 * @param <CMD> Command command interface type to invoke multi-node operations.
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 */
abstract class AbstractNodeSelection<API, CMD, K, V> implements NodeSelectionSupport<API, CMD> {

    protected StatefulRedisClusterConnection<K, V> globalConnection;
    private ClusterConnectionProvider.Intent intent;
    protected ClusterDistributionChannelWriter writer;

    public AbstractNodeSelection(StatefulRedisClusterConnection<K, V> globalConnection, ClusterConnectionProvider.Intent intent) {
        this.globalConnection = globalConnection;
        this.intent = intent;
        writer = ((StatefulRedisClusterConnectionImpl) globalConnection).getClusterDistributionChannelWriter();
    }

    protected StatefulRedisConnection<K, V> getConnection(RedisClusterNode redisClusterNode) {
        RedisURI uri = redisClusterNode.getUri();
        return writer.getClusterConnectionProvider().getConnection(intent, uri.getHost(), uri.getPort());
    }

    /**
     * @return List of involved nodes
     */
    protected abstract List<RedisClusterNode> nodes();

    @Override
    public int size() {
        return nodes().size();
    }

    public Map<RedisClusterNode, StatefulRedisConnection<K, V>> statefulMap() {
        return nodes().stream().collect(
                Collectors.toMap(redisClusterNode -> redisClusterNode, redisClusterNode1 -> getConnection(redisClusterNode1)));
    }

    @Override
    public RedisClusterNode node(int index) {
        return nodes().get(index);
    }

}
