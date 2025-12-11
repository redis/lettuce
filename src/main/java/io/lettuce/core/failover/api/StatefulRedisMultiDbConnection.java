package io.lettuce.core.failover.api;

import io.lettuce.core.api.StatefulRedisConnection;

/**
 * Stateful multi-database Redis connection that supports failover between multiple endpoints. Each endpoint has its own circuit
 * breaker for tracking command metrics.
 *
 * @param <K> Key type
 * @param <V> Value type
 * 
 * @author Ali Takavci
 * @since 7.1
 */
public interface StatefulRedisMultiDbConnection<K, V> extends StatefulRedisConnection<K, V>, BaseRedisMultiDbConnection {

}
