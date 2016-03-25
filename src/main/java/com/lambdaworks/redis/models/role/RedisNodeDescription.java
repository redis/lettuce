package com.lambdaworks.redis.models.role;

import com.lambdaworks.redis.RedisURI;

/**
 * Description of a single Redis Node.
 * 
 * @author Mark Paluch
 * @since 4.0
 */
public interface RedisNodeDescription extends RedisInstance {

    /**
     * 
     * @return the URI of the node
     */
    RedisURI getUri();
}
