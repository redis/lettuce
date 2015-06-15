package com.lambdaworks.redis.cluster.api.rx;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import rx.Observable;

import com.lambdaworks.redis.api.rx.BaseRedisReactiveCommands;
import com.lambdaworks.redis.api.rx.RedisHLLReactiveCommands;
import com.lambdaworks.redis.api.rx.RedisHashReactiveCommands;
import com.lambdaworks.redis.api.rx.RedisKeyReactiveCommands;
import com.lambdaworks.redis.api.rx.RedisListReactiveCommands;
import com.lambdaworks.redis.api.rx.RedisScriptingReactiveCommands;
import com.lambdaworks.redis.api.rx.RedisServerReactiveCommands;
import com.lambdaworks.redis.api.rx.RedisSetReactiveCommands;
import com.lambdaworks.redis.api.rx.RedisSortedSetReactiveCommands;
import com.lambdaworks.redis.api.rx.RedisStringReactiveCommands;

/**
 * A complete reactive and thread-safe cluster Redis API with 400+ Methods.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 4.0
 */
public interface RedisClusterReactiveCommands<K, V> extends RedisHashReactiveCommands<K, V>, RedisKeyReactiveCommands<K, V>,
        RedisStringReactiveCommands<K, V>, RedisListReactiveCommands<K, V>, RedisSetReactiveCommands<K, V>,
        RedisSortedSetReactiveCommands<K, V>, RedisScriptingReactiveCommands<K, V>, RedisServerReactiveCommands<K, V>,
        RedisHLLReactiveCommands<K, V>, BaseRedisReactiveCommands<K, V> {

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
    Observable<String> auth(String password);

    Observable<String> clusterMeet(String ip, int port);

    Observable<String> clusterForget(String nodeId);

    Observable<String> clusterAddSlots(int... slots);

    Observable<String> clusterDelSlots(int... slots);

    Observable<String> clusterInfo();

    Observable<String> clusterMyId();

    Observable<String> clusterNodes();

    Observable<List<K>> clusterGetKeysInSlot(int slot, int count);

    /**
     * Get array of Cluster slot to node mappings.
     * 
     * @return Observable&lt;List&lt;Object&gt;&gt; array-reply nested list of slot ranges with IP/Port mappings.
     */
    Observable<Object> clusterSlots();

    Observable<String> clusterSetSlotNode(int slot, String nodeId);

    Observable<String> clusterSetSlotMigrating(int slot, String nodeId);

    Observable<String> clusterSetSlotImporting(int slot, String nodeId);

    Observable<String> asking();

    Observable<String> clusterReplicate(String nodeId);

    Observable<String> clusterFailover(boolean force);

    Observable<String> clusterReset(boolean hard);

    Observable<String> clusterFlushslots();

    Observable<List<String>> clusterSlaves(String nodeId);

    /**
     * Tells a Redis cluster slave node that the client is ok reading possibly stale data and is not interested in running write
     * queries.
     *
     * @return String simple-string-reply
     */
    Observable<String> readOnly();

    /**
     * Resets readOnly flag.
     *
     * @return String simple-string-reply
     */
    Observable<String> readWrite();

    /**
     * Delete a key with pipelining. Cross-slot keys will result in multiple calls to the particular cluster nodes.
     *
     * @param keys the key
     * @return Observable&lt;Long&gt; integer-reply The number of keys that were removed.
     */
    Observable<Long> del(K... keys);

    /**
     * Get the values of all the given keys with pipelining. Cross-slot keys will result in multiple calls to the particular
     * cluster nodes.
     *
     * @param keys the key
     * @return Observable&lt;List&lt;V&gt;&gt; array-reply list of values at the specified keys.
     */
    Observable<V> mget(K... keys);

    /**
     * Set multiple keys to multiple values with pipelining. Cross-slot keys will result in multiple calls to the particular
     * cluster nodes.
     *
     * @param map the null
     * @return Observable&lt;String&gt; simple-string-reply always {@code OK} since {@code MSET} can't fail.
     */
    Observable<String> mset(Map<K, V> map);

    /**
     * Set multiple keys to multiple values, only if none of the keys exist with pipelining. Cross-slot keys will result in
     * multiple calls to the particular cluster nodes.
     *
     * @param map the null
     * @return Observable&lt;Boolean&gt; integer-reply specifically:
     *
     *         {@code 1} if the all the keys were set. {@code 0} if no key was set (at least one key already existed).
     */
    Observable<Boolean> msetnx(Map<K, V> map);
}
