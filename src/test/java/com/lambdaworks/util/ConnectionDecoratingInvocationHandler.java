package com.lambdaworks.util;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import com.lambdaworks.redis.api.StatefulConnection;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;
import com.lambdaworks.redis.internal.AbstractInvocationHandler;

/**
 * @author Mark Paluch
 */
public class ConnectionDecoratingInvocationHandler extends AbstractInvocationHandler {

    private final Object target;

    public ConnectionDecoratingInvocationHandler(Object target) {
        this.target = target;
    }

    @Override
    protected Object handleInvocation(Object proxy, Method method, Object[] args) throws Throwable {

        Method targetMethod = target.getClass().getMethod(method.getName(), method.getParameterTypes());
        Method proxyMethod = proxy.getClass().getMethod(method.getName(), method.getParameterTypes());

        Object result = targetMethod.invoke(target, args);

        if (result instanceof StatefulConnection) {

            Class[] interfaces;
            if (result instanceof StatefulRedisClusterConnection
                    && proxyMethod.getReturnType().isAssignableFrom(StatefulRedisClusterConnection.class)) {
                interfaces = new Class[] { StatefulConnection.class, StatefulRedisClusterConnection.class };
            } else {
                interfaces = new Class[] { StatefulConnection.class, StatefulRedisConnection.class };
            }

            return Proxy.newProxyInstance(getClass().getClassLoader(), interfaces,
                    new ConnectionDecoratingInvocationHandler(result));
        }

        return result;
    }
}
