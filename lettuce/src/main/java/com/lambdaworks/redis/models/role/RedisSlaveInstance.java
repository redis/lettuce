package com.lambdaworks.redis.models.role;

import static com.google.common.base.Preconditions.*;

import java.io.Serializable;

/**
 * Redis slave instance.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 03.08.14 10:49
 */
@SuppressWarnings("serial")
public class RedisSlaveInstance implements RedisInstance, Serializable {
    private ReplicationPartner master;
    private State state;

    protected RedisSlaveInstance() {
    }

    /**
     * Constructs a {@link RedisSlaveInstance}
     * 
     * @param master master for the replication, must not be {@literal null}
     * @param state slave state, must not be {@literal null}
     */
    public RedisSlaveInstance(ReplicationPartner master, State state) {
        checkArgument(master != null, "master must not be null");
        checkArgument(state != null, "state must not be null");
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
    public String toString() {
        final StringBuffer sb = new StringBuffer();
        sb.append(getClass().getSimpleName());
        sb.append(" [master=").append(master);
        sb.append(", state=").append(state);
        sb.append(']');
        return sb.toString();
    }
}
