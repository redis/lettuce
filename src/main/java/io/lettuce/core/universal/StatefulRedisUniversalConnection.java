package io.lettuce.core.universal;

import io.lettuce.core.api.CommandsFactory;
import io.lettuce.core.api.StatefulConnection;

/**
 * A connection to either a standalone server or an OSS cluster, created by {@link UniversalClient} after detecting the
 * topology. The application asks for the command API flavour it wants through {@link #commands(CommandsFactory)} using the
 * existing factories, e.g. {@code RedisReactiveCommands.factory()} or {@code RedisAdvancedClusterReactiveCommands.factory()}.
 * <p>
 * The factory must match the detected {@link TopologyMode}; a mismatch fails fast with {@link IllegalStateException}.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @since 7.x (PoC)
 */
public interface StatefulRedisUniversalConnection<K, V> extends StatefulConnection<K, V> {

    /**
     * Obtain a command API from an existing standalone or cluster {@link CommandsFactory}.
     *
     * @param factory a factory typed on {@code StatefulRedisConnection} or {@code StatefulRedisClusterConnection}.
     * @return the command API, cached per factory key by the underlying connection.
     * @throws IllegalStateException if the factory targets a connection type other than the detected one.
     */
    <T> T commands(CommandsFactory<? extends StatefulConnection<K, V>, T> factory);

    /**
     * @return the topology detected at connect time. Fixed for the lifetime of this connection.
     */
    TopologyMode getTopologyMode();

    /**
     * @return {@code true} if {@link #getTopologyMode()} is {@link TopologyMode#CLUSTER}.
     */
    default boolean isCluster() {
        return getTopologyMode() == TopologyMode.CLUSTER;
    }

}
