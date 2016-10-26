/*
 * Copyright 2011-2016 the original author or authors.
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
package com.lambdaworks.redis;

import java.io.Closeable;
import java.net.SocketAddress;
import java.util.List;
import java.util.Map;

import com.lambdaworks.redis.sentinel.api.async.RedisSentinelAsyncCommands;

/**
 * Asynchronous executed commands for Sentinel.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 3.0
 * @deprecated Use {@link RedisSentinelAsyncCommands}
 */
@Deprecated
public interface RedisSentinelAsyncConnection<K, V> extends Closeable {

    /**
     * Return the ip and port number of the master with that name.
     * 
     * @param key the key
     * @return Future&lt;SocketAddress&gt;
     */
    RedisFuture<SocketAddress> getMasterAddrByName(K key);

    /**
     * Enumerates all the monitored masters and their states.
     * 
     * @return RedisFuture&lt;Map&lt;K, V&gt;&gt;
     */
    RedisFuture<List<Map<K, V>>> masters();

    /**
     * Show the state and info of the specified master.
     * 
     * @param key the key
     * @return RedisFuture&lt;Map&lt;K, V&gt;&gt;
     */
    RedisFuture<Map<K, V>> master(K key);

    /**
     * Provides a list of slaves for the master with the specified name.
     * 
     * @param key the key
     * @return RedisFuture&lt;List&lt;Map&lt;K, V&gt;&gt;&gt;
     */
    RedisFuture<List<Map<K, V>>> slaves(K key);

    /**
     * This command will reset all the masters with matching name.
     * 
     * @param key the key
     * @return RedisFuture&lt;Long&gt;
     */
    RedisFuture<Long> reset(K key);

    /**
     * Perform a failover.
     * 
     * @param key the master id
     * @return RedisFuture&lt;String&gt;
     */
    RedisFuture<String> failover(K key);

    /**
     * This command tells the Sentinel to start monitoring a new master with the specified name, ip, port, and quorum.
     * 
     * @param key the key
     * @param ip the IP address
     * @param port the port
     * @param quorum the quorum count
     * @return RedisFuture&lt;String&gt;
     */
    RedisFuture<String> monitor(K key, String ip, int port, int quorum);

    /**
     * Multiple option / value pairs can be specified (or none at all).
     * 
     * @param key the key
     * @param option the option
     * @param value the value
     * 
     * @return RedisFuture&lt;String&gt; simple-string-reply {@code OK} if {@code SET} was executed correctly.
     */
    RedisFuture<String> set(K key, String option, V value);

    /**
     * remove the specified master.
     * 
     * @param key the key
     * @return RedisFuture&lt;String&gt;
     */
    RedisFuture<String> remove(K key);

    /**
     * Ping the server.
     * 
     * @return RedisFuture&lt;String&gt; simple-string-reply
     */
    RedisFuture<String> ping();

    @Override
    void close();

    /**
     *
     * @return true if the connection is open (connected and not closed).
     */
    boolean isOpen();
}
