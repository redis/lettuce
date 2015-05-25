package com.lambdaworks.redis.cluster;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.lambdaworks.redis.RedisClusterAsyncConnection;
import com.lambdaworks.redis.RedisFuture;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
abstract class AbstractNodeSelection<K, V> implements NodeSelection<K, V> {

    protected RedisAdvancedClusterConnectionImpl<K, V> globalConnection;
    protected ClusterDistributionChannelWriter writer;

    public AbstractNodeSelection(RedisAdvancedClusterConnectionImpl<K, V> globalConnection) {
        this.globalConnection = globalConnection;
        writer = (ClusterDistributionChannelWriter) globalConnection.getChannelWriter();
    }

    @Override
    public RedisClusterAsyncConnection<K, V> node(int index) {

        RedisClusterNode redisClusterNode = nodes().get(index);
        return getConnection(redisClusterNode);
    }

    private RedisClusterAsyncConnection<K, V> getConnection(RedisClusterNode redisClusterNode) {
        RedisURI uri = redisClusterNode.getUri();
        return writer.getClusterConnectionProvider().getConnection(ClusterConnectionProvider.Intent.WRITE, uri.getHost(),
                uri.getPort());
    }

    /**
     * @return List of involved nodes
     */
    protected abstract List<RedisClusterNode> nodes();

    @Override
    public int size() {
        return nodes().size();
    }

    @Override
    public Map<RedisClusterNode, RedisClusterAsyncConnection<K, V>> asMap() {
        return nodes().stream().collect(
                Collectors.toMap(redisClusterNode -> redisClusterNode, redisClusterNode1 -> getConnection(redisClusterNode1)));
    }

    @Override
    public Map<RedisClusterNode, RedisFuture<String>> get(K key) {
        for (RedisClusterAsyncConnection<K, V> kvRedisClusterAsyncConnection : this) {

        }
        return null;
    }

    @Override
    public Iterator<RedisClusterAsyncConnection<K, V>> iterator() {
        return nodes().stream().map(this::getConnection).iterator();
    }
}
