package com.lambdaworks.redis;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.reflect.AbstractInvocationHandler;
import com.lambdaworks.redis.api.StatefulConnection;
import com.lambdaworks.redis.api.StatefulRedisConnection;

/**
 * Invocation-handler to synchronize API calls which use Futures as backend. This class leverages the need to implement a full
 * sync class which just delegates every request.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 3.0
 */
class FutureSyncInvocationHandler<K, V> extends AbstractInvocationHandler {

    private final StatefulConnection<?, ?> connection;
    private final Object asyncApi;
    private LoadingCache<Method, Method> methodCache;

    public FutureSyncInvocationHandler(StatefulConnection<?, ?> connection, Object asyncApi) {
        this.connection = connection;
        this.asyncApi = asyncApi;

        methodCache = CacheBuilder.newBuilder().build(new CacheLoader<Method, Method>() {
            @Override
            public Method load(Method key) throws Exception {
                return asyncApi.getClass().getMethod(key.getName(), key.getParameterTypes());
            }
        });

    }

    /**
     * 
     * @see com.google.common.reflect.AbstractInvocationHandler#handleInvocation(java.lang.Object, java.lang.reflect.Method,
     *      java.lang.Object[])
     */
    @Override
    @SuppressWarnings("unchecked")
    protected Object handleInvocation(Object proxy, Method method, Object[] args) throws Throwable {

        try {

            Method targetMethod = methodCache.get(method);
            Object result = targetMethod.invoke(asyncApi, args);

            if (result instanceof RedisFuture) {
                RedisFuture<?> command = (RedisFuture<?>) result;
                if (!method.getName().equals("exec") && !method.getName().equals("multi")) {
                    if (connection instanceof StatefulRedisConnection && ((StatefulRedisConnection) connection).isMulti()) {
                        return null;
                    }
                }

                LettuceFutures.awaitOrCancel(command, connection.getTimeout(), connection.getTimeoutUnit());
                return command.get();
            }
            return result;
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }

    }
}
