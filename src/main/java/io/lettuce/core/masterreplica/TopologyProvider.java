package io.lettuce.core.masterreplica;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.lettuce.core.RedisException;
import io.lettuce.core.models.role.RedisNodeDescription;

/**
 * Topology provider for Master-Replica topology discovery during runtime. Implementors of this interface return an unordered
 * list of {@link RedisNodeDescription} instances.
 *
 * @author Mark Paluch
 * @since 4.1
 */
@FunctionalInterface
interface TopologyProvider {

    /**
     * Lookup nodes within the topology.
     *
     * @return list of {@link RedisNodeDescription} instances
     * @throws RedisException on errors that occurred during the lookup
     */
    List<RedisNodeDescription> getNodes();

    /**
     * Lookup nodes asynchronously within the topology.
     *
     * @return list of {@link RedisNodeDescription} instances
     * @throws RedisException on errors that occurred during the lookup
     * @since 5.1
     */
    default CompletableFuture<List<RedisNodeDescription>> getNodesAsync() {
        return CompletableFuture.completedFuture(getNodes());
    }

}
