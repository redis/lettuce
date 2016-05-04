package com.lambdaworks.redis.models.role;

import java.io.Serializable;

import com.lambdaworks.redis.internal.LettuceAssert;

/**
 * Redis slave instance.
 * 
 * @author Mark Paluch
 * @since 3.0
 */
@SuppressWarnings("serial")
public class RedisSlaveInstance implements RedisInstance, Serializable {
    private ReplicationPartner master;
    private State state;

    public RedisSlaveInstance() {
    }

    /**
     * Constructs a {@link RedisSlaveInstance}
     * 
     * @param master master for the replication, must not be {@literal null}
     * @param state slave state, must not be {@literal null}
     */
    RedisSlaveInstance(ReplicationPartner master, State state) {
        LettuceAssert.notNull(master, "master must not be null");
        LettuceAssert.notNull(state, "state must not be null");
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

    public void setMaster(ReplicationPartner master) {
        LettuceAssert.notNull(master, "master must not be null");
        this.master = master;
    }

    public void setState(State state) {
        LettuceAssert.notNull(state, "state must not be null");
        this.state = state;
    }

    /**
     * State of the slave.
     */
    public enum State {
        /**
         * the instance needs to connect to its master.
         */
        CONNECT,

        /**
         * the slave-master connection is in progress.
         */
        CONNECTING,

        /**
         * the master and slave are trying to perform the synchronization.
         */
        SYNC,

        /**
         * the slave is online.
         */
        CONNECTED;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [master=").append(master);
        sb.append(", state=").append(state);
        sb.append(']');
        return sb.toString();
    }
}
