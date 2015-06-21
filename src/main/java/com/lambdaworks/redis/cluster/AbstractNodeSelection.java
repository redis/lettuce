package com.lambdaworks.redis.cluster;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.cluster.api.NodeSelection;
import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
abstract class AbstractNodeSelection<T, CMDType, K, V> implements NodeSelection<T, CMDType> {

    protected StatefulRedisClusterConnection<K, V> globalConnection;
    private ClusterConnectionProvider.Intent intent;
    protected ClusterDistributionChannelWriter<K, V> writer;

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
    public RedisClusterNode get(int index) {
        return nodes().get(index);
    }

}
