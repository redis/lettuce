package com.lambdaworks.redis;

/**
 * @author <a href="mailto:mark.paluch@1und1.de">Mark Paluch</a>
 * @since 14.05.14 21:58
 */
public interface RedisConnectionProvider<T> {
    T createConnection();

    Class<T> getComponentType();
}
