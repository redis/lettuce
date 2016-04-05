package com.lambdaworks.redis.masterslave;

import java.util.List;

import com.lambdaworks.redis.RedisException;
import com.lambdaworks.redis.models.role.RedisNodeDescription;

/**
 * Topology provider for Master-Slave topology discovery during runtime. Implementors of this interface return an unordered list
 * of {@link RedisNodeDescription} instances.
 * 
 * @author Mark Paluch
 * @since 4.1
 */
@FunctionalInterface
public interface TopologyProvider {

    /**
     * Lookup the nodes within the topology.
     * 
     * @return list of {@link RedisNodeDescription} instances
     * @throws RedisException on errors that occured during the lookup
     */
    List<RedisNodeDescription> getNodes();
}
