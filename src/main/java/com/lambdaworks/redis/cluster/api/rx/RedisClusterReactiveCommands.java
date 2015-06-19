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

    /**
     * Meet another cluster node to include the node into the cluster. The command starts the cluster handshake and returns with
     * {@literal OK} when the node was added to the cluster.
     *
     * @param ip IP address of the host
     * @param port port number.
     * @return String simple-string-reply
     */
    Observable<String> clusterMeet(String ip, int port);

    /**
     * Blacklist and remove the cluster node from the cluster.
     *
     * @param nodeId the node Id
     * @return String simple-string-reply
     */
    Observable<String> clusterForget(String nodeId);

    /**
     * Adds slots to the cluster node. The current node will become the master for the specified slots.
     *
     * @param slots one or more slots from {@literal 0} to {@literal 16384}
     * @return String simple-string-reply
     */
    Observable<String> clusterAddSlots(int... slots);

    /**
     * Removes slots from the cluster node.
     *
     * @param slots one or more slots from {@literal 0} to {@literal 16384}
     * @return String simple-string-reply
     */
    Observable<String> clusterDelSlots(int... slots);

    /**
     * Assign a slot to a node. The command migrates the specified slot from the current node to the specified node in
     * {@code nodeId}
     *
     * @param slot the slot
     * @param nodeId the id of the node that will become the master for the slot
     * @return String simple-string-reply
     */
    Observable<String> clusterSetSlotNode(int slot, String nodeId);

    /**
     * Flag a slot as {@literal MIGRATING} (outgoing) towards the node specified in {@code nodeId}. The slot must be handled by
     * the current node in order to be migrated.
     *
     * @param slot the slot
     * @param nodeId the id of the node is targeted to become the master for the slot
     * @return String simple-string-reply
     */
    Observable<String> clusterSetSlotMigrating(int slot, String nodeId);

    /**
     * Flag a slot as {@literal IMPORTING} (incoming) from the node specified in {@code nodeId}.
     *
     * @param slot the slot
     * @param nodeId the id of the node is the master of the slot
     * @return String simple-string-reply
     */
    Observable<String> clusterSetSlotImporting(int slot, String nodeId);

    /**
     * Get information and statistics about the cluster viewed by the current node.
     *
     * @return String bulk-string-reply as a collection of text lines.
     */
    Observable<String> clusterInfo();

    /**
     * Obtain the nodeId for the currently connected node.
     *
     * @return String simple-string-reply
     */
    Observable<String> clusterMyId();

    /**
     * Obtain details about all cluster nodes. Can be parsed using
     * {@link com.lambdaworks.redis.cluster.models.partitions.ClusterPartitionParser#parse}
     *
     * @return String bulk-string-reply as a collection of text lines
     */
    Observable<String> clusterNodes();

    /**
     * List slaves for a certain node identified by its {@code nodeId}. Can be parsed using
     * {@link com.lambdaworks.redis.cluster.models.partitions.ClusterPartitionParser#parse}
     *
     * @param nodeId node id of the master node
     * @return
     */
    Observable<String> clusterSlaves(String nodeId);

    /**
     * Retrieve the list of keys within the {@code slot}.
     *
     * @param slot the slot
     * @param count maximal number of keys
     * @return List&lt;K&gt; array-reply list of keys
     */
    Observable<K> clusterGetKeysInSlot(int slot, int count);

    /**
     * Get array of cluster slots to node mappings.
     *
     * @return List&lt;Object&gt; array-reply nested list of slot ranges with IP/Port mappings.
     */
    Observable<Object> clusterSlots();

    /**
     *
     * @return String simple-string-reply
     */
    Observable<String> asking();

    /**
     * Turn this node into a slave of the node with the id {@code nodeId}.
     *
     * @param nodeId master node id
     * @return String simple-string-reply
     */
    Observable<String> clusterReplicate(String nodeId);

    /**
     * Failover a cluster node. Turns the currently connected node into a master and the master into its slave.
     *
     * @param force do not coordinate with master if {@literal true}
     * @return String simple-string-reply
     */
    Observable<String> clusterFailover(boolean force);

    /**
     * Reset a node performing a soft or hard reset:
     * <ul>
     * <li>All other nodes are forgotten</li>
     * <li>All the assigned / open slots are released</li>
     * <li>If the node is a slave, it turns into a master</li>
     * <li>Only for hard reset: a new Node ID is generated</li>
     * <li>Only for hard reset: currentEpoch and configEpoch are set to 0</li>
     * <li>The new configuration is saved and the cluster state updated</li>
     * <li>If the node was a slave, the whole data set is flushed away</li>
     * </ul>
     *
     * @param hard {@literal true} for hard reset. Generates a new nodeId and currentEpoch/configEpoch are set to 0
     * @return String simple-string-reply
     */
    Observable<String> clusterReset(boolean hard);

    /**
     * Delete all the slots associated with the specified node. The number of deleted slots is returned.
     *
     * @return String simple-string-reply
     */
    Observable<String> clusterFlushslots();

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
