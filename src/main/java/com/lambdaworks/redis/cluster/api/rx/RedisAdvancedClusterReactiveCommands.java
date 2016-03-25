package com.lambdaworks.redis.cluster.api.rx;

import java.util.Map;

import rx.Observable;

import com.lambdaworks.redis.KeyScanCursor;
import com.lambdaworks.redis.ScanArgs;
import com.lambdaworks.redis.ScanCursor;
import com.lambdaworks.redis.StreamScanCursor;
import com.lambdaworks.redis.api.rx.*;
import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;
import com.lambdaworks.redis.output.KeyStreamingChannel;

/**
 * Advanced reactive and thread-safe Redis Cluster API.
 *
 * @author Mark Paluch
 * @since 4.0
 */
public interface RedisAdvancedClusterReactiveCommands<K, V> extends RedisClusterReactiveCommands<K, V> {

    /**
     * Retrieve a connection to the specified cluster node using the nodeId. Host and port are looked up in the node list. In
     * contrast to the {@link RedisAdvancedClusterReactiveCommands}, node-connections do not route commands to other cluster
     * nodes
     *
     * @param nodeId the node Id
     * @return a connection to the requested cluster node
     */
    RedisClusterReactiveCommands<K, V> getConnection(String nodeId);

    /**
     * Retrieve a connection to the specified cluster node using the nodeId. In contrast to the
     * {@link RedisAdvancedClusterReactiveCommands}, node-connections do not route commands to other cluster nodes
     *
     * @param host the host
     * @param port the port
     * @return a connection to the requested cluster node
     */
    RedisClusterReactiveCommands<K, V> getConnection(String host, int port);

    /**
     * @return the underlying connection.
     */
    StatefulRedisClusterConnection<K, V> getStatefulConnection();

    /**
     * Delete one or more keys with pipelining. Cross-slot keys will result in multiple calls to the particular cluster nodes.
     *
     * @param keys the keys
     * @return Long integer-reply The number of keys that were removed.
     * @see RedisKeyReactiveCommands#del(Object[])
     */
    Observable<Long> del(K... keys);

    /**
     * Unlink one or more keys with pipelining. Cross-slot keys will result in multiple calls to the particular cluster nodes.
     *
     * @param keys the keys
     * @return Long integer-reply The number of keys that were removed.
     * @see RedisKeyReactiveCommands#unlink(Object[])
     */
    Observable<Long> unlink(K... keys);

    /**
     * Get the values of all the given keys with pipelining. Cross-slot keys will result in multiple calls to the particular
     * cluster nodes.
     *
     * @param keys the key
     * @return V array-reply list of values at the specified keys.
     * @see RedisStringReactiveCommands#mget(Object[])
     */
    Observable<V> mget(K... keys);

    /**
     * Set multiple keys to multiple values with pipelining. Cross-slot keys will result in multiple calls to the particular
     * cluster nodes.
     *
     * @param map the map
     * @return String simple-string-reply always {@code OK} since {@code MSET} can't fail.
     * @see RedisStringReactiveCommands#mset(Map)
     */
    Observable<String> mset(Map<K, V> map);

    /**
     * Set multiple keys to multiple values, only if none of the keys exist with pipelining. Cross-slot keys will result in
     * multiple calls to the particular cluster nodes.
     *
     * @param map the null
     * @return Boolean integer-reply specifically:
     *
     *         {@code 1} if the all the keys were set. {@code 0} if no key was set (at least one key already existed).
     *
     * @see RedisStringReactiveCommands#msetnx(Map)
     */
    Observable<Boolean> msetnx(Map<K, V> map);

    /**
     * Set the current connection name on all cluster nodes with pipelining.
     *
     * @param name the client name
     * @return simple-string-reply {@code OK} if the connection name was successfully set.
     * @see RedisServerReactiveCommands#clientSetname(Object)
     */
    Observable<String> clientSetname(K name);

    /**
     * Remove all keys from all databases on all cluster masters with pipelining.
     *
     * @return String simple-string-reply
     * @see RedisServerReactiveCommands#flushall()
     */
    Observable<String> flushall();

    /**
     * Remove all keys from the current database on all cluster masters with pipelining.
     *
     * @return String simple-string-reply
     * @see RedisServerReactiveCommands#flushdb()
     */
    Observable<String> flushdb();

    /**
     * Return the number of keys in the selected database on all cluster masters.
     *
     * @return Long integer-reply
     * @see RedisServerReactiveCommands#dbsize()
     */
    Observable<Long> dbsize();

    /**
     * Find all keys matching the given pattern on all cluster masters.
     *
     * @param pattern the pattern type: patternkey (pattern)
     * @return List&lt;K&gt; array-reply list of keys matching {@code pattern}.
     * @see RedisKeyReactiveCommands#keys(Object)
     */
    Observable<K> keys(K pattern);

    /**
     * Find all keys matching the given pattern on all cluster masters.
     *
     * @param channel the channel
     * @param pattern the pattern
     * @return Long array-reply list of keys matching {@code pattern}.
     * @see RedisKeyReactiveCommands#keys(KeyStreamingChannel, Object)
     */
    Observable<Long> keys(KeyStreamingChannel<K> channel, K pattern);

    /**
     * Return a random key from the keyspace on a random master.
     *
     * @return V bulk-string-reply the random key, or {@literal null} when the database is empty.
     * @see RedisKeyReactiveCommands#randomkey()
     */
    Observable<V> randomkey();

    /**
     * Remove all the scripts from the script cache on all cluster nodes.
     *
     * @return String simple-string-reply
     * @see RedisScriptingReactiveCommands#scriptFlush()
     */
    Observable<String> scriptFlush();

    /**
     * Kill the script currently in execution on all cluster nodes. This call does not fail even if no scripts are running.
     *
     * @return String simple-string-reply, always {@literal OK}.
     * @see RedisScriptingReactiveCommands#scriptKill()
     */
    Observable<String> scriptKill();

    /**
     * Synchronously save the dataset to disk and then shut down all nodes of the cluster.
     *
     * @param save {@literal true} force save operation
     * @see RedisServerReactiveCommands#shutdown(boolean)
     */
    Observable<Success> shutdown(boolean save);

    /**
     * Incrementally iterate the keys space over the whole Cluster.
     *
     * @return KeyScanCursor&lt;K&gt; scan cursor.
     * @see RedisKeyReactiveCommands#scan(ScanArgs)
     */
    Observable<KeyScanCursor<K>> scan();

    /**
     * Incrementally iterate the keys space over the whole Cluster.
     *
     * @param scanArgs scan arguments
     * @return KeyScanCursor&lt;K&gt; scan cursor.
     * @see RedisKeyReactiveCommands#scan(ScanArgs)
     */
    Observable<KeyScanCursor<K>> scan(ScanArgs scanArgs);

    /**
     * Incrementally iterate the keys space over the whole Cluster.
     *
     * @param scanCursor cursor to resume the scan. It's required to reuse the {@code scanCursor} instance from the previous
     *        {@link #scan()} call.
     * @param scanArgs scan arguments
     * @return KeyScanCursor&lt;K&gt; scan cursor.
     * @see RedisKeyReactiveCommands#scan(ScanCursor, ScanArgs)
     */
    Observable<KeyScanCursor<K>> scan(ScanCursor scanCursor, ScanArgs scanArgs);

    /**
     * Incrementally iterate the keys space over the whole Cluster.
     *
     * @param scanCursor cursor to resume the scan. It's required to reuse the {@code scanCursor} instance from the previous
     *        {@link #scan()} call.
     * @return KeyScanCursor&lt;K&gt; scan cursor.
     * @see RedisKeyReactiveCommands#scan(ScanCursor)
     */
    Observable<KeyScanCursor<K>> scan(ScanCursor scanCursor);

    /**
     * Incrementally iterate the keys space over the whole Cluster.
     *
     * @param channel streaming channel that receives a call for every key
     * @return StreamScanCursor scan cursor.
     * @see RedisKeyReactiveCommands#scan(KeyStreamingChannel)
     */
    Observable<StreamScanCursor> scan(KeyStreamingChannel<K> channel);

    /**
     * Incrementally iterate the keys space over the whole Cluster.
     *
     * @param channel streaming channel that receives a call for every key
     * @param scanArgs scan arguments
     * @return StreamScanCursor scan cursor.
     * @see RedisKeyReactiveCommands#scan(KeyStreamingChannel, ScanArgs)
     */
    Observable<StreamScanCursor> scan(KeyStreamingChannel<K> channel, ScanArgs scanArgs);

    /**
     * Incrementally iterate the keys space over the whole Cluster.
     *
     * @param channel streaming channel that receives a call for every key
     * @param scanCursor cursor to resume the scan. It's required to reuse the {@code scanCursor} instance from the previous
     *        {@link #scan()} call.
     * @param scanArgs scan arguments
     * @return StreamScanCursor scan cursor.
     * @see RedisKeyReactiveCommands#scan(KeyStreamingChannel, ScanCursor, ScanArgs)
     */
    Observable<StreamScanCursor> scan(KeyStreamingChannel<K> channel, ScanCursor scanCursor, ScanArgs scanArgs);

    /**
     * Incrementally iterate the keys space over the whole Cluster.
     *
     * @param channel streaming channel that receives a call for every key
     * @param scanCursor cursor to resume the scan. It's required to reuse the {@code scanCursor} instance from the previous
     *        {@link #scan()} call.
     * @return StreamScanCursor scan cursor.
     * @see RedisKeyReactiveCommands#scan(ScanCursor, ScanArgs)
     */
    Observable<StreamScanCursor> scan(KeyStreamingChannel<K> channel, ScanCursor scanCursor);

}
