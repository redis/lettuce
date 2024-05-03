package io.lettuce.core.models.role;

import io.lettuce.core.RedisURI;

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
