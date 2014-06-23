package com.lambdaworks.redis;

import java.io.Closeable;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * Asynchronous executed commands for Sentinel.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 15.05.14 21:04
 */
public interface RedisSentinelAsyncConnection<K, V> extends Closeable {

    /**
     * Return the ip and port number of the master with that name.
     * 
     * @param key
     * @return Future<SocketAddress>
     */
    Future<SocketAddress> getMasterAddrByName(K key);

    /**
     * Show the state and info of the specified master.
     * 
     * @param key
     * @return RedisFuture<Map<K, V>>
     */
    RedisFuture<Map<K, V>> master(K key);

    /**
     * Provides a list of slaves for the master with the specified name.
     * 
     * @param key
     * @return RedisFuture<Map<K, V>>
     */
    RedisFuture<Map<K, V>> slaves(K key);

    /**
     * This command will reset all the masters with matching name.
     * 
     * @param key
     * @return RedisFuture<Long>
     */
    RedisFuture<Long> reset(K key);

    /**
     * Perform a failover.
     * 
     * @param key the master id
     * @return RedisFuture<String>
     */
    RedisFuture<String> failover(K key);

    /**
     * This command tells the Sentinel to start monitoring a new master with the specified name, ip, port, and quorum.
     * 
     * @param key
     * @param ip
     * @param port
     * @param quorum
     * @return RedisFuture<String>
     */
    RedisFuture<String> monitor(K key, String ip, int port, int quorum);

    /**
     * Multiple option / value pairs can be specified (or none at all).
     * 
     * @param key the key
     * @param option the option
     * @param value the value
     * 
     * @return RedisFuture<String> simple-string-reply `OK` if `SET` was executed correctly.
     */
    RedisFuture<String> set(K key, String option, V value);

    /**
     * remove the specified master.
     * 
     * @param key
     * @return RedisFuture<String>
     */
    RedisFuture<String> remove(K key);

    /**
     * Ping the server.
     * 
     * @return RedisFuture<String> simple-string-reply
     */
    RedisFuture<String> ping();

    @Override
    void close();
}
