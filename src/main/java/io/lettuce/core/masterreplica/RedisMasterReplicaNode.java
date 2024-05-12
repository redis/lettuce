package io.lettuce.core.masterreplica;

import io.lettuce.core.RedisURI;
import io.lettuce.core.models.role.RedisNodeDescription;

/**
 * A node within a Redis Master-Replica setup.
 *
 * @author Mark Paluch
 * @author Adam McElwee
 */
class RedisMasterReplicaNode implements RedisNodeDescription {

    private final RedisURI redisURI;

    private final Role role;

    RedisMasterReplicaNode(String host, int port, RedisURI seed, Role role) {

        this.redisURI = RedisURI.builder(seed).withHost(host).withPort(port).build();
        this.role = role;
    }

    @Override
    public RedisURI getUri() {
        return redisURI;
    }

    @Override
    public Role getRole() {
        return role;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof RedisMasterReplicaNode))
            return false;

        RedisMasterReplicaNode that = (RedisMasterReplicaNode) o;

        if (!redisURI.equals(that.redisURI))
            return false;
        return role == that.role;
    }

    @Override
    public int hashCode() {
        int result = redisURI.hashCode();
        result = 31 * result + role.hashCode();
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [redisURI=").append(redisURI);
        sb.append(", role=").append(role);
        sb.append(']');
        return sb.toString();
    }

}
