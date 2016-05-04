package com.lambdaworks.redis.models.role;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import com.lambdaworks.redis.internal.LettuceAssert;

/**
 * Represents a master instance.
 * 
 * @author Mark Paluch
 * @since 3.0
 */
@SuppressWarnings("serial")
public class RedisMasterInstance implements RedisInstance, Serializable {

    private long replicationOffset;
    private List<ReplicationPartner> slaves = Collections.emptyList();

    public RedisMasterInstance() {
    }

    /**
     * Constructs a {@link RedisMasterInstance}
     * 
     * @param replicationOffset the replication offset
     * @param slaves list of slaves, must not be {@literal null} but may be empty
     */
    public RedisMasterInstance(long replicationOffset, List<ReplicationPartner> slaves) {
        LettuceAssert.notNull(slaves, "slaves must not be null");
        this.replicationOffset = replicationOffset;
        this.slaves = slaves;
    }

    /**
     *
     * @return always {@link com.lambdaworks.redis.models.role.RedisInstance.Role#MASTER}
     */
    @Override
    public Role getRole() {
        return Role.MASTER;
    }

    public long getReplicationOffset() {
        return replicationOffset;
    }

    public List<ReplicationPartner> getSlaves() {
        return slaves;
    }

    public void setReplicationOffset(long replicationOffset) {
        this.replicationOffset = replicationOffset;
    }

    public void setSlaves(List<ReplicationPartner> slaves) {
        LettuceAssert.notNull(slaves, "slaves must not be null");
        this.slaves = slaves;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [replicationOffset=").append(replicationOffset);
        sb.append(", slaves=").append(slaves);
        sb.append(']');
        return sb.toString();
    }
}
