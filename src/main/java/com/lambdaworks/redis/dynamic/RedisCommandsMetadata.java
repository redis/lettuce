package com.lambdaworks.redis.dynamic;

import java.lang.reflect.Method;
import java.util.Collection;

/**
 * Interface exposing Redis command interface metadata.
 * 
 * @author Mark Paluch
 * @since 5.0
 */
public interface RedisCommandsMetadata {

    Collection<Method> getMethods();

    /**
     * Returns the Redis Commands interface.
     *
     * @return
     */
    Class<?> getCommandsInterface();
}
