package com.lambdaworks.redis.support;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.reflect.AbstractInvocationHandler;
import com.lambdaworks.redis.RedisConnectionPool;
import com.lambdaworks.redis.RedisException;

/**
 * Invocation Handler with transparent pooling. This handler is thread-safe.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 15.05.14 21:14
 */
public class TransparentPoolingInvocationHandler<T> extends AbstractInvocationHandler {

    private RedisConnectionPool<T> pool;
    private final Map<Method, Method> methodCache = new ConcurrentHashMap<Method, Method>();

    /**
     * 
     * @param pool
     */
    public TransparentPoolingInvocationHandler(RedisConnectionPool<T> pool) {
        this.pool = pool;
    }

    @Override
    protected Object handleInvocation(Object proxy, Method method, Object[] args) throws Throwable {

        Method targetMethod = getMethod(method);

        if (pool == null) {
            throw new RedisException("Connection pool is closed");
        }

        if (method.getName().equals("close")) {
            pool.close();
            pool = null;
            return null;
        }

        T connection = pool.allocateConnection();
        try {

            return targetMethod.invoke(connection, args);
        } finally {
            pool.freeConnection(connection);
        }
    }

    /**
     * Lookup the target method using a cache.
     * 
     * @param method source method
     * @return the target method
     * @throws NoSuchMethodException
     */
    private Method getMethod(Method method) throws NoSuchMethodException {
        Method targetMethod = methodCache.get(method);
        if (targetMethod == null) {
            targetMethod = pool.getComponentType().getMethod(method.getName(), method.getParameterTypes());
            methodCache.put(method, targetMethod);
        }
        return targetMethod;
    }

    public RedisConnectionPool<T> getPool() {
        return pool;
    }

}
