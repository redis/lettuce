package io.lettuce.core.models.role;

import java.util.List;

/**
 * Represents an upstream (primary) instance.
 *
 * @author
 * @since 7.3
 */
@SuppressWarnings("serial")
public class RedisPrimaryInstance extends RedisUpstreamInstance {

    public RedisPrimaryInstance() {
    }

    /**
     * Constructs a {@link RedisPrimaryInstance}
     *
     * @param replicationOffset the replication offset
     * @param replicas list of replicas, must not be {@code null} but may be empty
     */
    public RedisPrimaryInstance(long replicationOffset, List<ReplicationPartner> replicas) {
        super(replicationOffset, replicas);
    }

    /**
     * @return always {@link io.lettuce.core.models.role.RedisInstance.Role#PRIMARY}
     */
    @Override
    public Role getRole() {
        return Role.PRIMARY;
    }

}
