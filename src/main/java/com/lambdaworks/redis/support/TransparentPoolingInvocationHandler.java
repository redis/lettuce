package com.lambdaworks.redis.support;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.google.common.reflect.AbstractInvocationHandler;
import com.lambdaworks.redis.RedisConnectionPool;

/**
 * @author <a href="mailto:mark.paluch@1und1.de">Mark Paluch</a>
 * @since 15.05.14 21:14
 */
public class TransparentPoolingInvocationHandler extends AbstractInvocationHandler {

    private RedisConnectionPool pool;
    private long recheckInterval;
    private TimeUnit unit;
    private long lastCheck;
    private long intervalMs;

    private Object cachedConnection;
    private Map<Method, Method> methodCache = new ConcurrentHashMap<Method, Method>();

    public TransparentPoolingInvocationHandler(RedisConnectionPool pool, long recheckInterval, TimeUnit unit) {
        this.pool = pool;
        this.recheckInterval = recheckInterval;
        this.unit = unit;
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

    private Method getMethod(Method method) throws NoSuchMethodException {
        Method targetMethod = methodCache.get(method);
        if (targetMethod == null) {
            targetMethod = pool.getComponentType().getMethod(method.getName(), method.getParameterTypes());
            methodCache.put(method, targetMethod);
        }
        return targetMethod;
    }
}
