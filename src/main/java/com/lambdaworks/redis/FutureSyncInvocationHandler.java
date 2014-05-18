package com.lambdaworks.redis;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import com.google.common.reflect.AbstractInvocationHandler;
import com.lambdaworks.redis.protocol.Command;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 14.05.14 12:28
 */
public class FutureSyncInvocationHandler<K, V> extends AbstractInvocationHandler {

    private RedisAsyncConnectionImpl<K, V> connection;
    protected long timeout;
    protected TimeUnit unit;

    public FutureSyncInvocationHandler(RedisAsyncConnectionImpl<K, V> connection) {
        this.connection = connection;
        this.timeout = connection.timeout;
        this.unit = connection.unit;
    }

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

            if (result instanceof Command) {
                Command command = (Command) result;
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
