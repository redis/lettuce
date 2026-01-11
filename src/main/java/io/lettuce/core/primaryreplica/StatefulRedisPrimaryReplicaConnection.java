package io.lettuce.core.primaryreplica;

import io.lettuce.core.ReadFrom;
import io.lettuce.core.api.StatefulRedisConnection;

/**
 * Redis Primary-Replica connection. The connection allows replica reads by setting {@link ReadFrom}.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author
 * @since 7.3
 */
public interface StatefulRedisPrimaryReplicaConnection<K, V> extends StatefulRedisConnection<K, V> {

    /**
     * Set from which nodes data is read. The setting is used as default for read operations on this connection. See the
     * documentation for {@link ReadFrom} for more information.
     *
     * @param readFrom the read from setting, must not be {@code null}
     */
    void setReadFrom(ReadFrom readFrom);

    /**
     * Gets the {@link ReadFrom} setting for this connection. Defaults to {@link ReadFrom#PRIMARY} if not set.
     *
     * @return the read from setting
     */
    ReadFrom getReadFrom();

}
