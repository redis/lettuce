package com.lambdaworks.redis.cluster.api.sync;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import com.lambdaworks.redis.KeyScanCursor;
import com.lambdaworks.redis.ScanArgs;
import com.lambdaworks.redis.ScanCursor;
import com.lambdaworks.redis.StreamScanCursor;
import com.lambdaworks.redis.api.sync.RedisKeyCommands;
import com.lambdaworks.redis.api.sync.RedisScriptingCommands;
import com.lambdaworks.redis.api.sync.RedisServerCommands;
import com.lambdaworks.redis.api.sync.RedisStringCommands;
import com.lambdaworks.redis.cluster.RedisAdvancedClusterConnection;
import com.lambdaworks.redis.cluster.api.NodeSelectionSupport;
import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;
import com.lambdaworks.redis.output.KeyStreamingChannel;

/**
 * Advanced synchronous and thread-safe Redis Cluster API.
 * 
 * @author Mark Paluch
 * @since 4.0
 */
public interface RedisAdvancedClusterCommands<K, V> extends RedisClusterCommands<K, V>, RedisAdvancedClusterConnection<K, V> {

    /**
     * Retrieve a connection to the specified cluster node using the nodeId. Host and port are looked up in the node list. In
     * contrast to the {@link RedisAdvancedClusterCommands}, node-connections do not route commands to other cluster nodes
     * 
     * @param nodeId the node Id
     * @return a connection to the requested cluster node
     */
    RedisClusterCommands<K, V> getConnection(String nodeId);

    /**
     * Retrieve a connection to the specified cluster node using the nodeId. In contrast to the
     * {@link RedisAdvancedClusterCommands}, node-connections do not route commands to other cluster nodes
     * 
     * @param host the host
     * @param port the port
     * @return a connection to the requested cluster node
     */
    RedisClusterCommands<K, V> getConnection(String host, int port);

    /**
     * @return the underlying connection.
     */
    StatefulRedisClusterConnection<K, V> getStatefulConnection();

    /**
     * Select all masters.
     *
     * @return API with synchronous executed commands on a selection of master cluster nodes.
     */
    default NodeSelection<K, V> masters() {
        return nodes(redisClusterNode -> redisClusterNode.is(RedisClusterNode.NodeFlag.MASTER));
    }

    /**
     * Select all slaves.
     *
     * @return API with synchronous executed commands on a selection of slave cluster nodes.
     */
    default NodeSelection<K, V> slaves() {
        return readonly(redisClusterNode -> redisClusterNode.is(RedisClusterNode.NodeFlag.SLAVE));
    }

    /**
     * Select all slaves.
     *
     * @param predicate Predicate to filter nodes
     * @return API with synchronous executed commands on a selection of slave cluster nodes.
     */
    default NodeSelection<K, V> slaves(Predicate<RedisClusterNode> predicate) {
        return readonly(redisClusterNode -> predicate.test(redisClusterNode)
                && redisClusterNode.is(RedisClusterNode.NodeFlag.SLAVE));
    }

    /**
     * Select all known cluster nodes.
     *
     * @return API with synchronous executed commands on a selection of all cluster nodes.
     */
    default NodeSelection<K, V> all() {
        return nodes(redisClusterNode -> true);
    }

    /**
     * Select slave nodes by a predicate and keeps a static selection. Slave connections operate in {@literal READONLY} mode.
     * The set of nodes within the {@link NodeSelectionSupport} does not change when the cluster view changes.
     *
     * @param predicate Predicate to filter nodes
     * @return API with synchronous executed commands on a selection of cluster nodes matching {@code predicate}
     */
    NodeSelection<K, V> readonly(Predicate<RedisClusterNode> predicate);

    /**
     * Select nodes by a predicate and keeps a static selection. The set of nodes within the {@link NodeSelectionSupport} does
     * not change when the cluster view changes.
     *
     * @param predicate Predicate to filter nodes
     * @return API with synchronous executed commands on a selection of cluster nodes matching {@code predicate}
     */
    NodeSelection<K, V> nodes(Predicate<RedisClusterNode> predicate);

    /**
     * Select nodes by a predicate
     *
     * @param predicate Predicate to filter nodes
     * @param dynamic Defines, whether the set of nodes within the {@link NodeSelectionSupport} can change when the cluster view
     *        changes.
     * @return API with synchronous executed commands on a selection of cluster nodes matching {@code predicate}
     */
    NodeSelection<K, V> nodes(Predicate<RedisClusterNode> predicate, boolean dynamic);

    /**
     * Delete one or more keys with pipelining. Cross-slot keys will result in multiple calls to the particular cluster nodes.
     *
     * @param keys the keys
     * @return Long integer-reply The number of keys that were removed.
     * @see RedisKeyCommands#del(Object[])
     */
    Long del(K... keys);

    /**
     * Unlink one or more keys with pipelining. Cross-slot keys will result in multiple calls to the particular cluster nodes.
     *
     * @param keys the keys
     * @return Long integer-reply The number of keys that were removed.
     * @see RedisKeyCommands#unlink(Object[])
     */
    Long unlink(K... keys);

    /**
     * Get the values of all the given keys with pipelining. Cross-slot keys will result in multiple calls to the particular
     * cluster nodes.
     * 
     * @param keys the key
     * @return List&lt;V&gt; array-reply list of values at the specified keys.
     * @see RedisStringCommands#mget(Object[])
     */
    List<V> mget(K... keys);

    /**
     * Set multiple keys to multiple values with pipelining. Cross-slot keys will result in multiple calls to the particular
     * cluster nodes.
     * 
     * @param map the map
     * @return String simple-string-reply always {@code OK} since {@code MSET} can't fail.
     * @see RedisStringCommands#mset(Map)
     */
    String mset(Map<K, V> map);

    /**
     * Set multiple keys to multiple values, only if none of the keys exist with pipelining. Cross-slot keys will result in
     * multiple calls to the particular cluster nodes.
     * 
     * @param map the null
     * @return Boolean integer-reply specifically:
     * 
     *         {@code 1} if the all the keys were set. {@code 0} if no key was set (at least one key already existed).
     * @see RedisStringCommands#msetnx(Map)
     */
    Boolean msetnx(Map<K, V> map);

    /**
     * Set the current connection name on all known cluster nodes with pipelining.
     *
     * @param name the client name
     * @return simple-string-reply {@code OK} if the connection name was successfully set.
     * @see RedisServerCommands#clientSetname(Object)
     */
    String clientSetname(K name);

    /**
     * Remove all keys from all databases on all cluster masters with pipelining.
     *
     * @return String simple-string-reply
     * @see RedisServerCommands#flushall()
     */
    String flushall();

    /**
     * Remove all keys from the current database on all cluster masters with pipelining.
     *
     * @return String simple-string-reply
     * @see RedisServerCommands#flushdb()
     */
    String flushdb();

    /**
     * Return the number of keys in the selected database on all cluster masters.
     *
     * @return Long integer-reply
     * @see RedisServerCommands#dbsize()
     */
    Long dbsize();

    /**
     * Find all keys matching the given pattern on all cluster masters.
     *
     * @param pattern the pattern type: patternkey (pattern)
     * @return List&lt;K&gt; array-reply list of keys matching {@code pattern}.
     * @see RedisKeyCommands#keys(Object)
     */
    List<K> keys(K pattern);

    /**
     * Find all keys matching the given pattern on all cluster masters.
     *
     * @param channel the channel
     * @param pattern the pattern
     * @return Long array-reply list of keys matching {@code pattern}.
     * @see RedisKeyCommands#keys(KeyStreamingChannel, Object)
     */
    Long keys(KeyStreamingChannel<K> channel, K pattern);

    /**
     * Return a random key from the keyspace on a random master.
     *
     * @return V bulk-string-reply the random key, or {@literal null} when the database is empty.
     * @see RedisKeyCommands#randomkey()
     */
    V randomkey();

    /**
     * Remove all the scripts from the script cache on all cluster nodes.
     *
     * @return String simple-string-reply
     * @see RedisScriptingCommands#scriptFlush()
     */
    String scriptFlush();

    /**
     * Kill the script currently in execution on all cluster nodes. This call does not fail even if no scripts are running.
     *
     * @return String simple-string-reply, always {@literal OK}.
     * @see RedisScriptingCommands#scriptKill()
     */
    String scriptKill();

    /**
     * Synchronously save the dataset to disk and then shut down all nodes of the cluster.
     * 
     * @param save {@literal true} force save operation
     * @see RedisServerCommands#shutdown(boolean)
     */
    void shutdown(boolean save);

    /**
     * Incrementally iterate the keys space over the whole Cluster.
     *
     * @return KeyScanCursor&lt;K&gt; scan cursor.
     * @see RedisKeyCommands#scan()
     */
    KeyScanCursor<K> scan();

    /**
     * Incrementally iterate the keys space over the whole Cluster.
     *
     * @param scanArgs scan arguments
     * @return KeyScanCursor&lt;K&gt; scan cursor.
     * @see RedisKeyCommands#scan(ScanArgs)
     */
    KeyScanCursor<K> scan(ScanArgs scanArgs);

    /**
     * Incrementally iterate the keys space over the whole Cluster.
     *
     * @param scanCursor cursor to resume the scan. It's required to reuse the {@code scanCursor} instance from the previous
     *        {@link #scan()} call.
     * @param scanArgs scan arguments
     * @return KeyScanCursor&lt;K&gt; scan cursor.
     * @see RedisKeyCommands#scan(ScanCursor, ScanArgs)
     */
    KeyScanCursor<K> scan(ScanCursor scanCursor, ScanArgs scanArgs);

    /**
     * Incrementally iterate the keys space over the whole Cluster.
     *
     * @param scanCursor cursor to resume the scan. It's required to reuse the {@code scanCursor} instance from the previous
     *        {@link #scan()} call.
     * @return KeyScanCursor&lt;K&gt; scan cursor.
     * @see RedisKeyCommands#scan(ScanCursor)
     */
    KeyScanCursor<K> scan(ScanCursor scanCursor);

    /**
     * Incrementally iterate the keys space over the whole Cluster.
     *
     * @param channel streaming channel that receives a call for every key
     * @return StreamScanCursor scan cursor.
     * @see RedisKeyCommands#scan(KeyStreamingChannel)
     */
    StreamScanCursor scan(KeyStreamingChannel<K> channel);

    /**
     * Incrementally iterate the keys space over the whole Cluster.
     *
     * @param channel streaming channel that receives a call for every key
     * @param scanArgs scan arguments
     * @return StreamScanCursor scan cursor.
     * @see RedisKeyCommands#scan(KeyStreamingChannel, ScanArgs)
     */
    StreamScanCursor scan(KeyStreamingChannel<K> channel, ScanArgs scanArgs);

    /**
     * Incrementally iterate the keys space over the whole Cluster.
     *
     * @param channel streaming channel that receives a call for every key
     * @param scanCursor cursor to resume the scan. It's required to reuse the {@code scanCursor} instance from the previous
     *        {@link #scan()} call.
     * @param scanArgs scan arguments
     * @return StreamScanCursor scan cursor.
     * @see RedisKeyCommands#scan(KeyStreamingChannel, ScanCursor, ScanArgs)
     */
    StreamScanCursor scan(KeyStreamingChannel<K> channel, ScanCursor scanCursor, ScanArgs scanArgs);

    /**
     * Incrementally iterate the keys space over the whole Cluster.
     *
     * @param channel streaming channel that receives a call for every key
     * @param scanCursor cursor to resume the scan. It's required to reuse the {@code scanCursor} instance from the previous
     *        {@link #scan()} call.
     * @return StreamScanCursor scan cursor.
     * @see RedisKeyCommands#scan(ScanCursor, ScanArgs)
     */
    StreamScanCursor scan(KeyStreamingChannel<K> channel, ScanCursor scanCursor);
}
