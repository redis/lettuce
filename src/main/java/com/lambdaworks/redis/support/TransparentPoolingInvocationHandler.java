package com.lambdaworks.redis.support;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.reflect.AbstractInvocationHandler;
import com.lambdaworks.redis.RedisConnectionPool;
import com.lambdaworks.redis.RedisException;

/**
 * Invocation Handler with transparent pooling. This handler is thread-safe.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 3.0
 */
public class TransparentPoolingInvocationHandler<T> extends AbstractInvocationHandler {

    private RedisConnectionPool<T> pool;
    private final LoadingCache<Method, Method> methodCache = CacheBuilder.newBuilder().build(new CacheLoader<Method, Method>() {
        @Override
        public Method load(Method key) throws Exception {
            return pool.getComponentType().getMethod(key.getName(), key.getParameterTypes());
        }
    });

    public TransparentPoolingInvocationHandler(RedisConnectionPool<T> pool) {
        this.pool = pool;
    }

    @Override
    protected Object handleInvocation(Object proxy, Method method, Object[] args) throws Throwable {

        Method targetMethod = methodCache.get(method);

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

    public RedisConnectionPool<T> getPool() {
        return pool;
    }

}
