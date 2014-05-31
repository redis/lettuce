package com.lambdaworks.redis;

import java.util.List;

/**
 * Complete asynchronous cluster Redis API with 400+ Methods..
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 17.05.14 21:14
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
