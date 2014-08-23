package com.lambdaworks.redis;

import java.util.List;

/**
 * Complete asynchronous cluster Redis API with 400+ Methods..
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 3.0
 */
public interface RedisClusterAsyncConnection<K, V> extends RedisHashesAsyncConnection<K, V>, RedisKeysAsyncConnection<K, V>,
        RedisStringsAsyncConnection<K, V>, RedisListsAsyncConnection<K, V>, RedisSetsAsyncConnection<K, V>,
        RedisSortedSetsAsyncConnection<K, V>, RedisScriptingAsyncConnection<K, V>, RedisServerAsyncConnection<K, V>,
        RedisHLLAsyncConnection<K, V>, BaseRedisAsyncConnection<K, V> {

    RedisFuture<String> clusterMeet(String ip, int port);

    RedisFuture<String> clusterForget(String nodeId);

    RedisFuture<String> clusterAddSlots(int... slots);

    RedisFuture<String> clusterDelSlots(int... slots);

    RedisFuture<String> clusterInfo();

    RedisFuture<String> clusterNodes();

    RedisFuture<List<K>> clusterGetKeysInSlot(int slot, int count);

    /**
     * Get array of Cluster slot to node mappings.
     * 
     * @return RedisFuture&lt;List&lt;Object&gt;&gt; array-reply nested list of slot ranges with IP/Port mappings.
     */
    RedisFuture<List<Object>> clusterSlots();

    RedisFuture<String> clusterSetSlotNode(int slot, String nodeId);

    RedisFuture<String> clusterSetSlotMigrating(int slot, String nodeId);

    RedisFuture<String> clusterSetSlotImporting(int slot, String nodeId);

    RedisFuture<String> asking();

    RedisFuture<String> clusterReplicate(String nodeId);

    RedisFuture<String> clusterFailover(boolean force);

    RedisFuture<String> clusterReset(boolean hard);

    RedisFuture<String> clusterFlushslots();

    RedisFuture<List<String>> clusterSlaves(String nodeId);

}
