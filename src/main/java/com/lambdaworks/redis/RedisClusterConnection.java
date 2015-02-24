package com.lambdaworks.redis;

import java.util.List;

/**
 * Complete synchronous cluster Redis API with 400+ Methods..
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 3.0
 */
public interface RedisClusterConnection<K, V> extends RedisHashesConnection<K, V>, RedisKeysConnection<K, V>,
        RedisStringsConnection<K, V>, RedisListsConnection<K, V>, RedisSetsConnection<K, V>, RedisSortedSetsConnection<K, V>,
        RedisScriptingConnection<K, V>, RedisServerConnection<K, V>, RedisHLLConnection<K, V> {

    String clusterMeet(String ip, int port);

    String clusterForget(String nodeId);

    String clusterAddSlots(int... slots);

    String clusterDelSlots(int... slots);

    String clusterInfo();

    String clusterNodes();

    List<K> clusterGetKeysInSlot(int slot, int count);

    /**
     * Get array of Cluster slot to node mappings.
     * 
     * @return List&lt;Object&gt; array-reply nested list of slot ranges with IP/Port mappings.
     */
    List<Object> clusterSlots();

    String clusterSetSlotNode(int slot, String nodeId);

    String clusterSetSlotMigrating(int slot, String nodeId);

    String clusterSetSlotImporting(int slot, String nodeId);

    String asking();

    String clusterReplicate(String nodeId);

    String clusterFailover(boolean force);

    String clusterReset(boolean hard);

    String clusterFlushslots();

    List<String> clusterSlaves(String nodeId);

    void close();

}
