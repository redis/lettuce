package com.lambdaworks.redis;

/**
 * Connection provider for redis connections.
 * 
 * @author Mark Paluch
 * @param <T> Connection type.
 * @since 3.0
 */
interface RedisConnectionProvider<T> {
    T createConnection();

    Class<? extends T> getComponentType();
}
