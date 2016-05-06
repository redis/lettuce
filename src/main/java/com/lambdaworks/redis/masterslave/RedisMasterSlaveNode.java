package com.lambdaworks.redis.masterslave;

import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.models.role.RedisNodeDescription;

/**
 * A node within a Redis Master-Slave setup.
 * 
 * @author Mark Paluch
 */
class RedisMasterSlaveNode implements RedisNodeDescription {

    private final RedisURI redisURI;
    private final Role role;

    public RedisMasterSlaveNode(String host, int port, RedisURI seed, Role role) {

        RedisURI.Builder builder = RedisURI.Builder.redis(host, port);
        if (seed.getPassword() != null && seed.getPassword().length != 0) {
            builder.withPassword(new String(seed.getPassword()));
        }

        this.redisURI = builder.withDatabase(seed.getDatabase()).build();
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
        if (!(o instanceof RedisMasterSlaveNode))
            return false;

        RedisMasterSlaveNode that = (RedisMasterSlaveNode) o;

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
        final StringBuffer sb = new StringBuffer();
        sb.append(getClass().getSimpleName());
        sb.append(" [redisURI=").append(redisURI);
        sb.append(", role=").append(role);
        sb.append(']');
        return sb.toString();
    }
}
