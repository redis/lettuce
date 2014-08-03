package com.lambdaworks.redis.models.role;

/**
 * Represents a redis instance according to the ROLE output.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 03.08.14 10:41
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
