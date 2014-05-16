package com.lambdaworks.redis;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 14.05.14 21:58
 */
public interface RedisConnectionProvider<T> {
    T createConnection();

    Class<T> getComponentType();
}
