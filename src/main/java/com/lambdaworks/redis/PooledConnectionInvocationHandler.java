package com.lambdaworks.redis;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.AbstractInvocationHandler;

/**
 * Invocation handler which takes care of connection.close(). Connections are returned to the pool on a close()-call.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @param <T> Connection type.
 * @since 3.0
 */
class PooledConnectionInvocationHandler<T> extends AbstractInvocationHandler {
    public static final Set<String> DISABLED_METHODS = ImmutableSet.of("auth", "select", "quit", "setAutoFlushCommands");

    private T connection;
    private final RedisConnectionPool<T> pool;
    private final LoadingCache<Method, Method> methodCache;

    public PooledConnectionInvocationHandler(final T connection, RedisConnectionPool<T> pool) {
        this.connection = connection;
        this.pool = pool;
        methodCache = CacheBuilder.newBuilder().build(new CacheLoader<Method, Method>() {
            @Override
            public Method load(Method key) throws Exception {
                return connection.getClass().getMethod(key.getName(), key.getParameterTypes());
            }
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Object handleInvocation(Object proxy, Method method, Object[] args) throws Throwable {

        if (DISABLED_METHODS.contains(method.getName())) {
            throw new UnsupportedOperationException("Calls to " + method.getName() + " are not supported on pooled connections");
        }

        if (connection == null) {
            throw new RedisException("Connection is deallocated and cannot be used anymore.");
        }

        if (method.getName().equals("close")) {
            pool.freeConnection((T) proxy);
            return null;
        }

        Method targetMethod = methodCache.get(method);

        try {
            return targetMethod.invoke(connection, args);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }

    public T getConnection() {
        return connection;
    }
}
