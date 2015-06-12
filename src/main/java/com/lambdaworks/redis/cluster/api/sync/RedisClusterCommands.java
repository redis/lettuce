package com.lambdaworks.redis.cluster.api.sync;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.lambdaworks.redis.RedisClusterConnection;
import com.lambdaworks.redis.api.sync.*;

/**
 * A complete synchronous and thread-safe cluster Redis API with 400+ Methods.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 4.0
 */
public interface RedisClusterCommands<K, V> extends RedisHashCommands<K, V>, RedisKeyCommands<K, V>, RedisStringCommands<K, V>,
        RedisListCommands<K, V>, RedisSetCommands<K, V>, RedisSortedSetCommands<K, V>, RedisScriptingCommands<K, V>,
        RedisServerCommands<K, V>, RedisHLLCommands<K, V>, AutoCloseable, RedisClusterConnection<K, V> {

    /**
     * Set the default timeout for operations.
     *
     * @param timeout the timeout value
     * @param unit the unit of the timeout value
     */
    void setTimeout(long timeout, TimeUnit unit);

    /**
     * Authenticate to the server.
     *
     * @param password the password
     * @return String simple-string-reply
     */
    String auth(String password);

    String clusterMeet(String ip, int port);

    String clusterForget(String nodeId);

    String clusterAddSlots(int... slots);

    String clusterDelSlots(int... slots);

    String clusterInfo();

    String clusterMyId();

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

    /**
     * Tells a Redis cluster slave node that the client is ok reading possibly stale data and is not interested in running write
     * queries.
     * 
     * @return String simple-string-reply
     */
    String readOnly();

    /**
     * Resets readOnly flag.
     * 
     * @return String simple-string-reply
     */
    String readWrite();

}
