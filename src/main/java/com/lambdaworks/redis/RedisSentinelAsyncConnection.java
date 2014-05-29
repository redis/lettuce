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

    Future<Map<K, V>> master(K key);

    Future<Map<K, V>> slaves(K key);

    Future<Long> reset(K key);

    Future<Long> failover(K key);

    Future<String> monitor(K key, String ip, int port, int quorum);

    Future<String> set(K key, String option, V value);

    Future<String> remove(K key);

    Future<String> ping();

    @Override
    void close();
}
