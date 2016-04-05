package com.lambdaworks.redis.support;

import java.lang.reflect.Proxy;

import com.lambdaworks.redis.RedisConnectionPool;

/**
 * Pooling proxy factory to create transparent pooling proxies. These proxies will allocate internally connections and use
 * always valid connections. You don't need to allocate/free the connections anymore.
 * 
 * @author Mark Paluch
 * @since 3.0
 */
public class PoolingProxyFactory {

    /**
     * Utility constructor.
     */
    private PoolingProxyFactory() {

    }

    /**
     * Creates a transparent connection pooling proxy. Will re-check the connection every 5 secs.
     * 
     * @param connectionPool The Redis connection pool
     * @param <T> Type of the connection.
     * @return Transparent pooling proxy.
     */
    @SuppressWarnings("unchecked")
    public static <T> T create(RedisConnectionPool<T> connectionPool) {
        Class<?> componentType = connectionPool.getComponentType();

        TransparentPoolingInvocationHandler<T> h = new TransparentPoolingInvocationHandler<T>(connectionPool);

        Object o = Proxy.newProxyInstance(PoolingProxyFactory.class.getClassLoader(), new Class<?>[] { componentType }, h);

        return (T) o;
    }

}
