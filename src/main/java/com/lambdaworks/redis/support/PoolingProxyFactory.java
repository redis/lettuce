package com.lambdaworks.redis.support;

import java.lang.reflect.Proxy;
import java.util.concurrent.TimeUnit;

import com.lambdaworks.redis.RedisConnectionPool;

/**
 * @author <a href="mailto:mark.paluch@1und1.de">Mark Paluch</a>
 * @since 15.05.14 21:12
 */
public class PoolingProxyFactory {

    public static <T> T create(RedisConnectionPool<T> connectionPool, long recheckInterval, TimeUnit unit) {
        Class<T> componentType = connectionPool.getComponentType();

        TransparentPoolingInvocationHandler h = new TransparentPoolingInvocationHandler(connectionPool, recheckInterval, unit);

        Object o = Proxy.newProxyInstance(PoolingProxyFactory.class.getClassLoader(), new Class[] { componentType }, h);

        return (T) o;
    }

}
