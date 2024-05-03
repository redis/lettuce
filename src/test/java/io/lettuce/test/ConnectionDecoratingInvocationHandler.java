package io.lettuce.test;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.internal.AbstractInvocationHandler;
import io.lettuce.core.sentinel.api.StatefulRedisSentinelConnection;

/**
 * @author Mark Paluch
 */
class ConnectionDecoratingInvocationHandler extends AbstractInvocationHandler {

    private final Object target;

    ConnectionDecoratingInvocationHandler(Object target) {
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
            } else if (result instanceof StatefulRedisSentinelConnection
                    && proxyMethod.getReturnType().isAssignableFrom(StatefulRedisSentinelConnection.class)) {
                interfaces = new Class[] { StatefulConnection.class, StatefulRedisSentinelConnection.class };
            } else {
                interfaces = new Class[] { StatefulConnection.class, StatefulRedisConnection.class };
            }

            return Proxy.newProxyInstance(getClass().getClassLoader(), interfaces,
                    new ConnectionDecoratingInvocationHandler(result));
        }

        return result;
    }

}
