package com.lambdaworks.redis;

import java.util.List;
import java.util.Map;

/**
 * Asynchronous executed commands for HyperLogLog (PF* commands).
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 17.05.14 21:14
 */
public interface RedisClusterAsyncConnection<K, V> {
    RedisFuture<String> clusterMeet(String ip, int port);

    RedisFuture<String> clusterForget(String nodeId);

    RedisFuture<String> clusterAddSlots(int... slots);

    RedisFuture<String> clusterDelSlots(int... slots);

    RedisFuture<List<String>> clusterInfo();

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

    RedisFuture<Map<K, V>> clusterSlaves();

}
