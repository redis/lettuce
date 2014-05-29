package com.lambdaworks.redis;

/**
 * Connection provider for redis connections.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @param <T> Connection type.
 * @since 14.05.14 21:58
 */
public interface RedisConnectionProvider<T> {
    T createConnection();

    Class<?> getComponentType();
}
