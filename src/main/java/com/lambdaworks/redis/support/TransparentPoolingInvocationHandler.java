package com.lambdaworks.redis.support;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.google.common.reflect.AbstractInvocationHandler;
import com.lambdaworks.redis.RedisConnectionPool;

/**
 * Invocation Handler with transparent pooling. This handler is thread-safe.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 15.05.14 21:14
 */
public class TransparentPoolingInvocationHandler extends AbstractInvocationHandler {

    private RedisConnectionPool pool;
    private long lastCheck;
    private long intervalMs;

    private Object cachedConnection;
    private Map<Method, Method> methodCache = new ConcurrentHashMap<Method, Method>();

    /**
     * 
     * @param pool
     * @param recheckInterval
     * @param unit
     */
    public TransparentPoolingInvocationHandler(RedisConnectionPool pool, long recheckInterval, TimeUnit unit) {
        this.pool = pool;
        intervalMs = TimeUnit.MILLISECONDS.convert(recheckInterval, unit);
    }

    @Override
    protected Object handleInvocation(Object proxy, Method method, Object[] args) throws Throwable {

        long now = System.currentTimeMillis();

        if (cachedConnection != null) {
            if (lastCheck + intervalMs < now) {
                pool.freeConnection(cachedConnection);
                cachedConnection = null;
            }
        }

        Method targetMethod = getMethod(method);
        try {
            if (cachedConnection == null) {
                cachedConnection = pool.allocateConnection();
                lastCheck = now;
            }
            return targetMethod.invoke(cachedConnection, args);
        } finally {
            if (method.getName().equals("close")) {
                pool.freeConnection(cachedConnection);
                cachedConnection = null;
            }
        }
    }

    /**
     * Lookup the target method using a cache.
     * 
     * @param method
     * @return
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
}
