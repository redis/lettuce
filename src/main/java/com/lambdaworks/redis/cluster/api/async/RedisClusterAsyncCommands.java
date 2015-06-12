package com.lambdaworks.redis.cluster.api.async;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.lambdaworks.redis.RedisClusterAsyncConnection;
import com.lambdaworks.redis.RedisFuture;
import com.lambdaworks.redis.api.async.*;

/**
 * A complete asynchronous and thread-safe cluster Redis API with 400+ Methods.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 4.0
 */
public interface RedisClusterAsyncCommands<K, V> extends RedisHashAsyncCommands<K, V>, RedisKeyAsyncCommands<K, V>,
        RedisStringAsyncCommands<K, V>, RedisListAsyncCommands<K, V>, RedisSetAsyncCommands<K, V>,
        RedisSortedSetAsyncCommands<K, V>, RedisScriptingAsyncCommands<K, V>, RedisServerAsyncCommands<K, V>,
        RedisHLLAsyncCommands<K, V>, BaseRedisAsyncCommands<K, V>, RedisClusterAsyncConnection<K, V> {

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

    RedisFuture<String> clusterMeet(String ip, int port);

    RedisFuture<String> clusterForget(String nodeId);

    RedisFuture<String> clusterAddSlots(int... slots);

    RedisFuture<String> clusterDelSlots(int... slots);

    RedisFuture<String> clusterInfo();

    RedisFuture<String> clusterMyId();

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

    /**
     * Tells a Redis cluster slave node that the client is ok reading possibly stale data and is not interested in running write
     * queries.
     *
     * @return String simple-string-reply
     */
    RedisFuture<String> readOnly();

    /**
     * Resets readOnly flag.
     *
     * @return String simple-string-reply
     */
    RedisFuture<String> readWrite();

    /**
     * Delete a key with pipelining. Cross-slot keys will result in multiple calls to the particular cluster nodes.
     *
     * @param keys the key
     * @return RedisFuture&lt;Long&gt; integer-reply The number of keys that were removed.
     */
    RedisFuture<Long> del(K... keys);

    /**
     * Get the values of all the given keys with pipelining. Cross-slot keys will result in multiple calls to the particular
     * cluster nodes.
     *
     * @param keys the key
     * @return RedisFuture&lt;List&lt;V&gt;&gt; array-reply list of values at the specified keys.
     */
    RedisFuture<List<V>> mget(K... keys);

    /**
     * Set multiple keys to multiple values with pipelining. Cross-slot keys will result in multiple calls to the particular
     * cluster nodes.
     *
     * @param map the null
     * @return RedisFuture&lt;String&gt; simple-string-reply always {@code OK} since {@code MSET} can't fail.
     */
    RedisFuture<String> mset(Map<K, V> map);

    /**
     * Set multiple keys to multiple values, only if none of the keys exist with pipelining. Cross-slot keys will result in
     * multiple calls to the particular cluster nodes.
     *
     * @param map the null
     * @return RedisFuture&lt;Boolean&gt; integer-reply specifically:
     *
     *         {@code 1} if the all the keys were set. {@code 0} if no key was set (at least one key already existed).
     */
    RedisFuture<Boolean> msetnx(Map<K, V> map);
}
