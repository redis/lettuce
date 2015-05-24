package com.lambdaworks.redis.cluster;

import java.util.Iterator;
import java.util.Map;

import com.lambdaworks.redis.RedisClusterAsyncConnection;
import com.lambdaworks.redis.RedisFuture;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public interface NodeSelection<K, V> extends Iterable<RedisClusterAsyncConnection<K, V>> {

    RedisClusterAsyncConnection<K, V> node(int index);

    int size();

    Map<RedisClusterNode, RedisClusterAsyncConnection<K, V>> asMap();

    Map<RedisClusterNode, RedisFuture<String>> get(K key);
}
