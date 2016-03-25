package com.lambdaworks.redis.masterslave;

import com.lambdaworks.redis.ReadFrom;
import com.lambdaworks.redis.api.StatefulRedisConnection;

/**
 * Redis Master-Slave connection. The connection allows slave reads by setting {@link ReadFrom}.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 4.1
 */
public interface StatefulRedisMasterSlaveConnection<K, V> extends StatefulRedisConnection<K, V> {

    /**
     * Set from which nodes data is read. The setting is used as default for read operations on this connection. See the
     * documentation for {@link ReadFrom} for more information.
     * 
     * @param readFrom the read from setting, must not be {@literal null}
     */
    void setReadFrom(ReadFrom readFrom);

    /**
     * Gets the {@link ReadFrom} setting for this connection. Defaults to {@link ReadFrom#MASTER} if not set.
     * 
     * @return the read from setting
     */
    ReadFrom getReadFrom();
}
