package com.lambdaworks.redis;

import java.util.List;
import java.util.Map;

/**
 * Asynchronous executed commands for HyperLogLog (PF* commands).
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 17.05.14 21:14
 */
public interface RedisClusterConnection<K, V> {
    String clusterMeet(String ip, int port);

    String clusterForget(String nodeId);

    String clusterAddSlots(int... slots);

    String clusterDelSlots(int... slots);

    Map<K, V> clusterInfo();

    List<K> clusterGetKeysInSlot(int slot, int count);

    String clusterSetSlotNode(int slot, String nodeId);

    String clusterSetSlotMigrating(int slot, String nodeId);

    String clusterSetSlotImporting(int slot, String nodeId);

    String asking();

    String clusterReplicate(String nodeId);

    String clusterFailover(boolean force);

    String clusterReset(boolean hard);

    String clusterFlushslots();

    Map<K, V> clusterSlaves();

}
