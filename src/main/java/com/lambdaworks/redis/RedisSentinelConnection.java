package com.lambdaworks.redis;

import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * @author <a href="mailto:mark.paluch@1und1.de">Mark Paluch</a>
 * @since 15.05.14 21:04
 */
public interface RedisSentinelConnection<K, V> extends AutoCloseable {
    Future<SocketAddress> getMasterAddrByName(K key);

    Future<Map<K, V>> getMaster(K key);

    Future<Map<K, V>> getSlaves(K key);

    Future<Long> reset(K key);

    Future<Long> failover(K key);

    Future<String> monitor(K key, String ip, int port, int quorum);

    Future<String> set(K key, String option, V value);

    Future<String> remove(K key);

    Future<String> ping();

    void close();
}
