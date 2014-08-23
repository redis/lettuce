package com.lambdaworks.redis;

/**
 * Connection provider for redis connections.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @param <T> Connection type.
 * @since 3.0
 */
interface RedisConnectionProvider<T> {
    T createConnection();

    Class<? extends T> getComponentType();
}
