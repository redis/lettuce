package io.lettuce.core.masterreplica;

import io.lettuce.core.ReadFrom;
import io.lettuce.core.primaryreplica.StatefulRedisPrimaryReplicaConnection;

/**
 * Redis Master-Replica connection. The connection allows replica reads by setting {@link ReadFrom}.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 4.1
 * @deprecated since 7.3, use {@link io.lettuce.core.primaryreplica.StatefulRedisPrimaryReplicaConnection}.
 */
@Deprecated
public interface StatefulRedisMasterReplicaConnection<K, V> extends StatefulRedisPrimaryReplicaConnection<K, V> {

    /**
     * Set from which nodes data is read. The setting is used as default for read operations on this connection. See the
     * documentation for {@link ReadFrom} for more information.
     *
     * @param readFrom the read from setting, must not be {@code null}
     * @deprecated since 7.3, use {@link StatefulRedisPrimaryReplicaConnection#setReadFrom(ReadFrom)}.
     */
    @Deprecated
    void setReadFrom(ReadFrom readFrom);

    /**
     * Gets the {@link ReadFrom} setting for this connection. Defaults to {@link ReadFrom#UPSTREAM} if not set.
     *
     * @return the read from setting
     * @deprecated since 7.3, use {@link StatefulRedisPrimaryReplicaConnection#getReadFrom()}.
     */
    @Deprecated
    ReadFrom getReadFrom();

}
