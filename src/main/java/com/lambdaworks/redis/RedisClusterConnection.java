package com.lambdaworks.redis;

import java.util.List;
import java.util.Map;

/**
 * Complete synchronous cluster Redis API with 400+ Methods..
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 17.05.14 21:14
 */
public interface RedisClusterConnection<K, V> extends RedisHashesConnection<K, V>, RedisKeysConnection<K, V>,
        RedisStringsConnection<K, V>, RedisListsConnection<K, V>, RedisSetsConnection<K, V>, RedisSortedSetsConnection<K, V>,
        RedisScriptingConnection<K, V>, RedisServerConnection<K, V>, RedisHLLConnection<K, V> {

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
