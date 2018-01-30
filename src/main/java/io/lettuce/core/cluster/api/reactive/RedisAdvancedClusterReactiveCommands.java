/*
 * Copyright 2011-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.cluster.api.reactive;

import java.util.Map;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import io.lettuce.core.*;
import io.lettuce.core.api.reactive.RedisKeyReactiveCommands;
import io.lettuce.core.api.reactive.RedisScriptingReactiveCommands;
import io.lettuce.core.api.reactive.RedisServerReactiveCommands;
import io.lettuce.core.api.reactive.RedisStringReactiveCommands;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.output.KeyStreamingChannel;

/**
 * Advanced reactive and thread-safe Redis Cluster API.
 *
 * @author Mark Paluch
 * @since 5.0
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
     * Retrieve a connection to the specified cluster node using host and port. In contrast to the
     * {@link RedisAdvancedClusterReactiveCommands}, node-connections do not route commands to other cluster nodes. Host and
     * port connections are verified by default for cluster membership, see
     * {@link ClusterClientOptions#isValidateClusterNodeMembership()}.
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
    Mono<Long> del(K... keys);

    /**
     * Unlink one or more keys with pipelining. Cross-slot keys will result in multiple calls to the particular cluster nodes.
     *
     * @param keys the keys
     * @return Long integer-reply The number of keys that were removed.
     * @see RedisKeyReactiveCommands#unlink(Object[])
     */
    Mono<Long> unlink(K... keys);

    /**
     * Determine how many keys exist with pipelining. Cross-slot keys will result in multiple calls to the particular cluster
     * nodes.
     *
     * @param keys the keys
     * @return Long integer-reply specifically: Number of existing keys
     */
    Mono<Long> exists(K... keys);

    /**
     * Get the values of all the given keys with pipelining. Cross-slot keys will result in multiple calls to the particular
     * cluster nodes.
     *
     * @param keys the key
     * @return V array-reply list of values at the specified keys.
     * @see RedisStringReactiveCommands#mget(Object[])
     */
    Flux<KeyValue<K, V>> mget(K... keys);

    /**
     * Set multiple keys to multiple values with pipelining. Cross-slot keys will result in multiple calls to the particular
     * cluster nodes.
     *
     * @param map the map
     * @return String simple-string-reply always {@code OK} since {@code MSET} can't fail.
     * @see RedisStringReactiveCommands#mset(Map)
     */
    Mono<String> mset(Map<K, V> map);

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
    Mono<Boolean> msetnx(Map<K, V> map);

    /**
     * Set the current connection name on all cluster nodes with pipelining.
     *
     * @param name the client name
     * @return simple-string-reply {@code OK} if the connection name was successfully set.
     * @see RedisServerReactiveCommands#clientSetname(Object)
     */
    Mono<String> clientSetname(K name);

    /**
     * Remove all keys from all databases on all cluster masters with pipelining.
     *
     * @return String simple-string-reply
     * @see RedisServerReactiveCommands#flushall()
     */
    Mono<String> flushall();

    /**
     * Remove all keys from the current database on all cluster masters with pipelining.
     *
     * @return String simple-string-reply
     * @see RedisServerReactiveCommands#flushdb()
     */
    Mono<String> flushdb();

    /**
     * Return the number of keys in the selected database on all cluster masters.
     *
     * @return Long integer-reply
     * @see RedisServerReactiveCommands#dbsize()
     */
    Mono<Long> dbsize();

    /**
     * Find all keys matching the given pattern on all cluster masters.
     *
     * @param pattern the pattern type: patternkey (pattern)
     * @return List&lt;K&gt; array-reply list of keys matching {@code pattern}.
     * @see RedisKeyReactiveCommands#keys(Object)
     */
    Flux<K> keys(K pattern);

    /**
     * Find all keys matching the given pattern on all cluster masters.
     *
     * @param channel the channel
     * @param pattern the pattern
     * @return Long array-reply list of keys matching {@code pattern}.
     * @see RedisKeyReactiveCommands#keys(KeyStreamingChannel, Object)
     */
    Mono<Long> keys(KeyStreamingChannel<K> channel, K pattern);

    /**
     * Return a random key from the keyspace on a random master.
     *
     * @return V bulk-string-reply the random key, or {@literal null} when the database is empty.
     * @see RedisKeyReactiveCommands#randomkey()
     */
    Mono<V> randomkey();

    /**
     * Remove all the scripts from the script cache on all cluster nodes.
     *
     * @return String simple-string-reply
     * @see RedisScriptingReactiveCommands#scriptFlush()
     */
    Mono<String> scriptFlush();

    /**
     * Kill the script currently in execution on all cluster nodes. This call does not fail even if no scripts are running.
     *
     * @return String simple-string-reply, always {@literal OK}.
     * @see RedisScriptingReactiveCommands#scriptKill()
     */
    Mono<String> scriptKill();

    /**
     * Load the specified Lua script into the script cache on all cluster nodes.
     *
     * @param script script content
     * @return String bulk-string-reply This command returns the SHA1 digest of the script added into the script cache.
     */
    Mono<String> scriptLoad(V script);

    /**
     * Synchronously save the dataset to disk and then shut down all nodes of the cluster.
     *
     * @param save {@literal true} force save operation
     * @see RedisServerReactiveCommands#shutdown(boolean)
     */
    Mono<Void> shutdown(boolean save);

    /**
     * Incrementally iterate the keys space over the whole Cluster.
     *
     * @return KeyScanCursor&lt;K&gt; scan cursor.
     * @see RedisKeyReactiveCommands#scan(ScanArgs)
     */
    Mono<KeyScanCursor<K>> scan();

    /**
     * Incrementally iterate the keys space over the whole Cluster.
     *
     * @param scanArgs scan arguments
     * @return KeyScanCursor&lt;K&gt; scan cursor.
     * @see RedisKeyReactiveCommands#scan(ScanArgs)
     */
    Mono<KeyScanCursor<K>> scan(ScanArgs scanArgs);

    /**
     * Incrementally iterate the keys space over the whole Cluster.
     *
     * @param scanCursor cursor to resume the scan. It's required to reuse the {@code scanCursor} instance from the previous
     *        {@link #scan()} call.
     * @param scanArgs scan arguments
     * @return KeyScanCursor&lt;K&gt; scan cursor.
     * @see RedisKeyReactiveCommands#scan(ScanCursor, ScanArgs)
     */
    Mono<KeyScanCursor<K>> scan(ScanCursor scanCursor, ScanArgs scanArgs);

    /**
     * Incrementally iterate the keys space over the whole Cluster.
     *
     * @param scanCursor cursor to resume the scan. It's required to reuse the {@code scanCursor} instance from the previous
     *        {@link #scan()} call.
     * @return KeyScanCursor&lt;K&gt; scan cursor.
     * @see RedisKeyReactiveCommands#scan(ScanCursor)
     */
    Mono<KeyScanCursor<K>> scan(ScanCursor scanCursor);

    /**
     * Incrementally iterate the keys space over the whole Cluster.
     *
     * @param channel streaming channel that receives a call for every key
     * @return StreamScanCursor scan cursor.
     * @see RedisKeyReactiveCommands#scan(KeyStreamingChannel)
     */
    Mono<StreamScanCursor> scan(KeyStreamingChannel<K> channel);

    /**
     * Incrementally iterate the keys space over the whole Cluster.
     *
     * @param channel streaming channel that receives a call for every key
     * @param scanArgs scan arguments
     * @return StreamScanCursor scan cursor.
     * @see RedisKeyReactiveCommands#scan(KeyStreamingChannel, ScanArgs)
     */
    Mono<StreamScanCursor> scan(KeyStreamingChannel<K> channel, ScanArgs scanArgs);

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
    Mono<StreamScanCursor> scan(KeyStreamingChannel<K> channel, ScanCursor scanCursor, ScanArgs scanArgs);

    /**
     * Incrementally iterate the keys space over the whole Cluster.
     *
     * @param channel streaming channel that receives a call for every key
     * @param scanCursor cursor to resume the scan. It's required to reuse the {@code scanCursor} instance from the previous
     *        {@link #scan()} call.
     * @return StreamScanCursor scan cursor.
     * @see RedisKeyReactiveCommands#scan(ScanCursor, ScanArgs)
     */
    Mono<StreamScanCursor> scan(KeyStreamingChannel<K> channel, ScanCursor scanCursor);

    /**
     * Touch one or more keys with pipelining. Touch sets the last accessed time for a key. Non-exsitent keys wont get created.
     * Cross-slot keys will result in multiple calls to the particular cluster nodes.
     *
     * @param keys the keys
     * @return Long integer-reply the number of found keys.
     */
    Mono<Long> touch(K... keys);

}
