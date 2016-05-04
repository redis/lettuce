package com.lambdaworks.redis.support;

import java.lang.reflect.Method;

import com.lambdaworks.redis.RedisConnectionPool;
import com.lambdaworks.redis.RedisException;
import com.lambdaworks.redis.internal.AbstractInvocationHandler;

/**
 * Invocation Handler with transparent pooling. This handler is thread-safe.
 * 
 * @author Mark Paluch
 * @since 3.0
 */
public class TransparentPoolingInvocationHandler<T> extends AbstractInvocationHandler {

    private RedisConnectionPool<T> pool;

    public TransparentPoolingInvocationHandler(RedisConnectionPool<T> pool) {
        this.pool = pool;
    }

    @Override
    protected Object handleInvocation(Object proxy, Method method, Object[] args) throws Throwable {

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
            return method.invoke(connection, args);
        } finally {
            pool.freeConnection(connection);
        }
    }

    public RedisConnectionPool<T> getPool() {
        return pool;
    }

}
