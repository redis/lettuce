package io.lettuce.core.models.role;

import java.io.Serializable;

import io.lettuce.core.internal.LettuceAssert;

/**
 * Redis replica instance.
 *
 * @author Mark Paluch
 * @since 3.0
 */
@SuppressWarnings("serial")
public class RedisReplicaInstance implements RedisInstance, Serializable {

    private ReplicationPartner upstream;

    private State state;

    public RedisReplicaInstance() {
    }

    /**
     * Constructs a {@link RedisReplicaInstance}
     *
     * @param upstream primary for the replication, must not be {@code null}
     * @param state replica state, must not be {@code null}
     */
    RedisReplicaInstance(ReplicationPartner upstream, State state) {
        LettuceAssert.notNull(upstream, "Upstream must not be null");
        LettuceAssert.notNull(state, "State must not be null");
        this.upstream = upstream;
        this.state = state;
    }

    /**
     *
     * @return always {@link Role#REPLICA}
     */
    @Override
    public Role getRole() {
        return Role.REPLICA;
    }

    /**
     *
     * @return the replication origin.
     */
    public ReplicationPartner getUpstream() {
        return upstream;
    }

    /**
     * @return Replica state.
     */
    public State getState() {
        return state;
    }

    public void setUpstream(ReplicationPartner upstream) {
        LettuceAssert.notNull(upstream, "Master must not be null");
        this.upstream = upstream;
    }

    public void setState(State state) {
        LettuceAssert.notNull(state, "State must not be null");
        this.state = state;
    }

    /**
     * State of the Replica.
     */
    public enum State {

        /**
         * Nothing to replicate.
         */
        NONE,

        /**
         * unknown state.
         */
        UNKNOWN,

        /**
         * the instance is in the handshake state.
         */
        HANDSHAKE,
        /**
         * the instance needs to connect to its primary.
         */
        CONNECT,

        /**
         * the replica-primary connection is in progress.
         */
        CONNECTING,

        /**
         * the primary and replica are trying to perform the synchronization.
         */
        SYNC,

        /**
         * the replica is online.
         */
        CONNECTED;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [primary=").append(upstream);
        sb.append(", state=").append(state);
        sb.append(']');
        return sb.toString();
    }

}
