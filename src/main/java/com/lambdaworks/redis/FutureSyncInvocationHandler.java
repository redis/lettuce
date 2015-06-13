package com.lambdaworks.redis;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.google.common.reflect.AbstractInvocationHandler;
import com.lambdaworks.redis.api.StatefulConnection;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.cluster.api.async.RedisClusterAsyncCommands;
import com.lambdaworks.redis.cluster.api.sync.RedisClusterCommands;

/**
 * Invocation-handler to synchronize API calls which use Futures as backend. This class leverages the need to implement a full
 * sync class which just delegates every request.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 3.0
 */
public class FutureSyncInvocationHandler<K, V> extends AbstractInvocationHandler {

    private final StatefulConnection<K, V> connection;
    private final Object asyncApi;

    public FutureSyncInvocationHandler(StatefulConnection<K, V> connection, Object asyncApi) {
        this.connection = connection;
        this.asyncApi = asyncApi;
    }

    /**
     * 
     * @see com.google.common.reflect.AbstractInvocationHandler#handleInvocation(java.lang.Object, java.lang.reflect.Method,
     *      java.lang.Object[])
     */
    @Override
    protected Object handleInvocation(Object proxy, Method method, Object[] args) throws Throwable {

        try {

            Method targetMethod = asyncApi.getClass().getMethod(method.getName(), method.getParameterTypes());

            Object result = targetMethod.invoke(asyncApi, args);

            if (result instanceof RedisFuture) {
                RedisFuture<?> command = (RedisFuture<?>) result;
                if (!method.getName().equals("exec") && !method.getName().equals("multi")) {
                    if (connection instanceof StatefulRedisConnection && ((StatefulRedisConnection) connection).isMulti()) {
                        return null;
                    }
                }

                return LettuceFutures.await(command, connection.getTimeout(), connection.getTimeoutUnit());
            }

            if (result instanceof RedisClusterAsyncCommands) {
                return AbstractRedisClient.syncHandler((RedisChannelHandler) result, RedisConnection.class,
                        RedisClusterConnection.class, RedisCommands.class, RedisClusterCommands.class);
            }

            return result;

        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }

    }
}
