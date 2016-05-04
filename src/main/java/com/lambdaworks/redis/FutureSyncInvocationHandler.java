package com.lambdaworks.redis;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.lambdaworks.redis.api.StatefulConnection;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.internal.AbstractInvocationHandler;

/**
 * Invocation-handler to synchronize API calls which use Futures as backend. This class leverages the need to implement a full
 * sync class which just delegates every request.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 3.0
 */
class FutureSyncInvocationHandler<K, V> extends AbstractInvocationHandler {

    private final StatefulConnection<?, ?> connection;
    private final Object asyncApi;
    private final MethodTranslator translator;

    public FutureSyncInvocationHandler(StatefulConnection<?, ?> connection, Object asyncApi, Class<?>[] interfaces) {
        this.connection = connection;
        this.asyncApi = asyncApi;
        this.translator = new MethodTranslator(asyncApi.getClass(), interfaces);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Object handleInvocation(Object proxy, Method method, Object[] args) throws Throwable {

        try {

            Method targetMethod = this.translator.get(method);
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
