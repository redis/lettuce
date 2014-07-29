package com.lambdaworks.redis;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import com.google.common.reflect.AbstractInvocationHandler;
import com.lambdaworks.redis.protocol.RedisCommand;

/**
 * Invocation-handler to synchronize API calls which use Futures as backend. This class leverages the need to implement a full
 * sync class which just delegates every request.
 * 
 * @param <K> Key type.
 * @param <V> Value type.
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 14.05.14 12:28
 */
class FutureSyncInvocationHandler<K, V> extends AbstractInvocationHandler {

    private final RedisAsyncConnectionImpl<K, V> connection;
    protected long timeout;
    protected TimeUnit unit;

    public FutureSyncInvocationHandler(RedisAsyncConnectionImpl<K, V> connection) {
        this.connection = connection;
        this.timeout = connection.timeout;
        this.unit = connection.unit;
    }

    /**
     * 
     * @see com.google.common.reflect.AbstractInvocationHandler#handleInvocation(java.lang.Object, java.lang.reflect.Method,
     *      java.lang.Object[])
     */
    @Override
    protected Object handleInvocation(Object proxy, Method method, Object[] args) throws Throwable {

        try {

            // void setTimeout(long timeout, TimeUnit unit)
            if (method.getName().equals("setTimeout")) {
                setTimeout((Long) args[0], (TimeUnit) args[1]);
                return null;
            }

            Method targetMethod = connection.getClass().getMethod(method.getName(), method.getParameterTypes());

            Object result = targetMethod.invoke(connection, args);

            if (result instanceof RedisCommand) {
                RedisCommand<?, ?, ?> command = (RedisCommand<?, ?, ?>) result;
                if (!method.getName().equals("exec") && !method.getName().equals("multi")) {
                    if (connection.isMulti()) {
                        return null;
                    }
                }

                return LettuceFutures.await(command, timeout, unit);
            }

            return result;

        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }

    }

    private void setTimeout(long timeout, TimeUnit unit) {
        this.timeout = timeout;
        this.unit = unit;
    }
}
