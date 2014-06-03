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
    Future<SocketAddress> getMasterAddrByName(K key);

    RedisFuture<Map<K, V>> master(K key);

    RedisFuture<Map<K, V>> slaves(K key);

    RedisFuture<Long> reset(K key);

    RedisFuture<String> failover(K key);

    RedisFuture<String> monitor(K key, String ip, int port, int quorum);

    RedisFuture<String> set(K key, String option, V value);

    RedisFuture<String> remove(K key);

    RedisFuture<String> ping();

    @Override
    void close();
}
