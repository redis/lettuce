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
package com.lambdaworks.redis.commands.rx;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Set;

import com.lambdaworks.redis.api.StatefulConnection;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;
import com.lambdaworks.redis.internal.AbstractInvocationHandler;
import com.lambdaworks.redis.internal.LettuceSets;
import com.lambdaworks.redis.sentinel.api.StatefulRedisSentinelConnection;
import com.lambdaworks.redis.sentinel.api.sync.RedisSentinelCommands;

import rx.Observable;

/**
 * Invocation handler for testing purposes that exposes a synchronous API by calling commands using the reactive API.
 * 
 * @author Mark Paluch
 */
public class RxSyncInvocationHandler<K, V> extends AbstractInvocationHandler {

    private final StatefulConnection<?, ?> connection;
    private final Object rxApi;

    public RxSyncInvocationHandler(StatefulConnection<?, ?> connection, Object rxApi) {
        this.connection = connection;
        this.rxApi = rxApi;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Object handleInvocation(Object proxy, Method method, Object[] args) throws Throwable {

        try {

            Method targetMethod = rxApi.getClass().getMethod(method.getName(), method.getParameterTypes());

            Object result = targetMethod.invoke(rxApi, args);

            if (result == null || !(result instanceof Observable<?>)) {
                return result;
            }
            Observable<?> observable = (Observable<?>) result;

            if (!method.getName().equals("exec") && !method.getName().equals("multi")) {
                if (connection instanceof StatefulRedisConnection && ((StatefulRedisConnection) connection).isMulti()) {
                    observable.subscribe();
                    return null;
                }
            }

            List<?> value = observable.toList().toBlocking().first();

            if (method.getReturnType().equals(List.class)) {
                return value;
            }

            if (method.getReturnType().equals(Set.class)) {
                return LettuceSets.newHashSet(value);
            }

            if (!value.isEmpty()) {
                return value.get(0);
            }

            return null;

        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }

    public static <K, V> RedisCommands<K, V> sync(StatefulRedisConnection<K, V> connection) {

        RxSyncInvocationHandler<K, V> handler = new RxSyncInvocationHandler<>(connection, connection.reactive());
        return (RedisCommands<K, V>) Proxy.newProxyInstance(handler.getClass().getClassLoader(),
                new Class<?>[] { RedisCommands.class }, handler);
    }

    public static <K, V> RedisCommands<K, V> sync(StatefulRedisClusterConnection<K, V> connection) {

        RxSyncInvocationHandler<K, V> handler = new RxSyncInvocationHandler<>(connection, connection.reactive());
        return (RedisCommands<K, V>) Proxy.newProxyInstance(handler.getClass().getClassLoader(),
                new Class<?>[] { RedisCommands.class }, handler);
    }

    public static <K, V> RedisSentinelCommands<K, V> sync(StatefulRedisSentinelConnection<K, V> connection) {

        RxSyncInvocationHandler<K, V> handler = new RxSyncInvocationHandler<>(connection, connection.reactive());
        return (RedisSentinelCommands<K, V>) Proxy.newProxyInstance(handler.getClass().getClassLoader(),
                new Class<?>[] { RedisSentinelCommands.class }, handler);
    }
}
