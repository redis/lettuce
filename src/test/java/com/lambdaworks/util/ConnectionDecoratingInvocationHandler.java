/*
 * Copyright 2011-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lambdaworks.util;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import com.lambdaworks.redis.api.StatefulConnection;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;
import com.lambdaworks.redis.internal.AbstractInvocationHandler;
import com.lambdaworks.redis.sentinel.api.StatefulRedisSentinelConnection;

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
