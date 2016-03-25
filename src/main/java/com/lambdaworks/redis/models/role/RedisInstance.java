package com.lambdaworks.redis.models.role;

/**
 * Represents a redis instance according to the {@code ROLE} output.
 * 
 * @author Mark Paluch
 * @since 3.0
 */
public interface RedisInstance {

    /**
     * 
     * @return Redis instance role, see {@link com.lambdaworks.redis.models.role.RedisInstance.Role}
     */
    Role getRole();

    /**
     * Possible Redis instance roles.
     */
    public enum Role {
        MASTER, SLAVE, SENTINEL;
    }
}
