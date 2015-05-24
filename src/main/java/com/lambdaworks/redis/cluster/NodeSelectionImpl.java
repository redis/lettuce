package com.lambdaworks.redis.cluster;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.lambdaworks.redis.RedisClusterAsyncConnection;
import com.lambdaworks.redis.RedisFuture;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;
import io.netty.util.concurrent.CompleteFuture;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class NodeSelectionImpl<K, V> implements NodeSelection<K, V> {

    private List<RedisClusterNode> nodes = Lists.newArrayList();
    private RedisAdvancedClusterConnectionImpl<K, V> globalConnection;
    private ClusterDistributionChannelWriter writer;

    public NodeSelectionImpl(List<RedisClusterNode> nodes, RedisAdvancedClusterConnectionImpl<K, V> globalConnection) {
        this.nodes = nodes;
        this.globalConnection = globalConnection;
        CompletableFuture cf1 = new CompletableFuture();

        writer = (ClusterDistributionChannelWriter) globalConnection.getChannelWriter();
    }

    @Override
    public RedisClusterAsyncConnection<K, V> node(int index) {
        return null;
    }

    private RedisClusterAsyncConnection<K, V> getConnection(RedisClusterNode redisClusterNode) {

        RedisURI uri = redisClusterNode.getUri();
        return writer.getClusterConnectionProvider().getConnection(ClusterConnectionProvider.Intent.WRITE, uri.getHost(),
                uri.getPort());
    }

    @Override
    public int size() {
        return nodes.size();
    }

    @Override
    public Map<RedisClusterNode, RedisClusterAsyncConnection<K, V>> asMap() {
        return nodes.stream().collect(
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
        return nodes.stream().map(this::getConnection).iterator();
    }

}
