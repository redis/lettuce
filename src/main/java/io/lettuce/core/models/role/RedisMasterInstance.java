package io.lettuce.core.models.role;

import java.util.List;

/**
 * Represents a upstream (master) instance.
 *
 * @author Mark Paluch
 * @since 3.0
 */
@SuppressWarnings("serial")
public class RedisMasterInstance extends RedisUpstreamInstance {

    public RedisMasterInstance() {
    }

    /**
     * Constructs a {@link RedisMasterInstance}
     *
     * @param replicationOffset the replication offset
     * @param replicas list of replicas, must not be {@code null} but may be empty
     */
    public RedisMasterInstance(long replicationOffset, List<ReplicationPartner> replicas) {
        super(replicationOffset, replicas);
    }

    /**
     * @return always {@link io.lettuce.core.models.role.RedisInstance.Role#MASTER}
     */
    @Override
    public Role getRole() {
        return Role.MASTER;
    }

}
