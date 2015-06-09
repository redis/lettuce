package com.lambdaworks.redis.cluster;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.google.common.reflect.AbstractInvocationHandler;
import com.lambdaworks.redis.LettuceFutures;
import com.lambdaworks.redis.RedisFuture;
import com.lambdaworks.redis.api.StatefulConnection;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.cluster.api.StatefulClusterConnection;

/**
 * Invocation-handler to synchronize API calls which use Futures as backend. This class leverages the need to implement a full
 * sync class which just delegates every request.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 3.0
 */
class ClusterFutureSyncInvocationHandler<K, V> extends AbstractInvocationHandler {

    private final StatefulConnection<K, V> connection;
    private final Object asyncApi;

    public ClusterFutureSyncInvocationHandler(StatefulConnection<K, V> connection, Object asyncApi) {
        this.connection = connection;
        this.asyncApi = asyncApi;
    }

    /**
     * 
     * @see AbstractInvocationHandler#handleInvocation(Object, Method, Object[])
     */
    @Override
    protected Object handleInvocation(Object proxy, Method method, Object[] args) throws Throwable {

        try {

            if (method.getName().equals("getConnection") && args.length > 0) {
                Method targetMethod = connection.getClass().getMethod(method.getName(), method.getParameterTypes());
                Object result = targetMethod.invoke(connection, args);
                if (result instanceof StatefulClusterConnection) {
                    StatefulClusterConnection connection = (StatefulClusterConnection) result;
                    return connection.sync();
                }
            }

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

            return result;

        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }
}
