package com.lambdaworks.redis.models.role;

import java.io.Serializable;

/**
 * Redis slave instance.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 03.08.14 10:49
 */
public class RedisSlaveInstance implements RedisInstance, Serializable {
    private ReplicationPartner master;
    private State state;

    public RedisSlaveInstance(ReplicationPartner master, State state) {
        this.master = master;
        this.state = state;
    }

    /**
     *
     * @return always {@link com.lambdaworks.redis.models.role.RedisInstance.Role#SLAVE}
     */
    @Override
    public Role getRole() {
        return Role.SLAVE;
    }

    /**
     *
     * @return the replication master.
     */
    public ReplicationPartner getMaster() {
        return master;
    }

    /**
     *
     * @return Slave state.
     */
    public State getState() {
        return state;
    }

    /**
     * State of the slave.
     */
    public enum State {
        CONNECT, CONNECTING, SYNC, CONNECTED;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RedisSlaveInstance)) {
            return false;
        }

        RedisSlaveInstance that = (RedisSlaveInstance) o;

        if (master != null ? !master.equals(that.master) : that.master != null) {
            return false;
        }
        if (state != that.state) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = master != null ? master.hashCode() : 0;
        result = 31 * result + (state != null ? state.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer();
        sb.append(getClass().getSimpleName());
        sb.append(" [master=").append(master);
        sb.append(", state=").append(state);
        sb.append(']');
        return sb.toString();
    }
}
