package com.lambdaworks.redis.models.role;

import java.io.Serializable;
import java.util.List;

/**
 * Represents a master instance.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 03.08.14 10:44
 */
public class RedisMasterInstance implements RedisInstance, Serializable {

    private long replicationOffset;
    private List<ReplicationPartner> slaves;

    public RedisMasterInstance(long replicationOffset, List<ReplicationPartner> slaves) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RedisMasterInstance)) {
            return false;
        }

        RedisMasterInstance that = (RedisMasterInstance) o;

        if (replicationOffset != that.replicationOffset) {
            return false;
        }
        if (slaves != null ? !slaves.equals(that.slaves) : that.slaves != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (replicationOffset ^ (replicationOffset >>> 32));
        result = 31 * result + (slaves != null ? slaves.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer();
        sb.append(getClass().getSimpleName());
        sb.append(" [replicationOffset=").append(replicationOffset);
        sb.append(", slaves=").append(slaves);
        sb.append(']');
        return sb.toString();
    }
}
