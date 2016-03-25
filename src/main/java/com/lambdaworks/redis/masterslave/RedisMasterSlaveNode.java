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

    public RedisMasterSlaveNode(String host, int port, Role role) {
        this.redisURI = RedisURI.Builder.redis(host, port).build();
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
}
