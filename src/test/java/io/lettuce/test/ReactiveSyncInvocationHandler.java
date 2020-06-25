/*
 * Copyright 2018-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Set;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.internal.LettuceSets;
import io.lettuce.core.sentinel.api.StatefulRedisSentinelConnection;
import io.lettuce.core.sentinel.api.sync.RedisSentinelCommands;

/**
 * Invocation handler for testing purposes.
 *
 * @param <K>
 * @param <V>
 */
public class ReactiveSyncInvocationHandler<K, V> extends ConnectionDecoratingInvocationHandler {

    private final StatefulConnection<?, ?> connection;

    private ReactiveSyncInvocationHandler(StatefulConnection<?, ?> connection, Object rxApi) {
        super(rxApi);
        this.connection = connection;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Object handleInvocation(Object proxy, Method method, Object[] args) throws Throwable {

        try {

            Object result = super.handleInvocation(proxy, method, args);

            if (result == null) {
                return result;
            }

            if (result instanceof StatefulConnection) {
                return result;
            }

            if (result instanceof Flux<?>) {
                Flux<?> flux = (Flux<?>) result;

                if (!method.getName().equals("exec") && !method.getName().equals("multi")) {
                    if (connection instanceof StatefulRedisConnection && ((StatefulRedisConnection) connection).isMulti()) {
                        flux.subscribe();
                        return null;
                    }
                }

                List<?> value = flux.collectList().block();

                if (method.getReturnType().equals(List.class)) {
                    return value;
                }

                if (method.getReturnType().equals(Set.class)) {
                    return LettuceSets.newHashSet(value);
                }

                if (!value.isEmpty()) {
                    return value.get(0);
                }
            }

            if (result instanceof Mono<?>) {
                Mono<?> mono = (Mono<?>) result;

                if (!method.getName().equals("exec") && !method.getName().equals("multi")) {
                    if (connection instanceof StatefulRedisConnection && ((StatefulRedisConnection) connection).isMulti()) {
                        mono.subscribe();
                        return null;
                    }
                }

                return mono.block();
            }

            return result;

        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }

    public static <K, V> RedisCommands<K, V> sync(StatefulRedisConnection<K, V> connection) {

        ReactiveSyncInvocationHandler<K, V> handler = new ReactiveSyncInvocationHandler<>(connection, connection.reactive());
        return (RedisCommands<K, V>) Proxy.newProxyInstance(handler.getClass().getClassLoader(),
                new Class<?>[] { RedisCommands.class }, handler);
    }

    public static <K, V> RedisCommands<K, V> sync(StatefulRedisClusterConnection<K, V> connection) {

        ReactiveSyncInvocationHandler<K, V> handler = new ReactiveSyncInvocationHandler<>(connection, connection.reactive());
        return (RedisCommands<K, V>) Proxy.newProxyInstance(handler.getClass().getClassLoader(),
                new Class<?>[] { RedisCommands.class }, handler);
    }

    public static <K, V> RedisSentinelCommands<K, V> sync(StatefulRedisSentinelConnection<K, V> connection) {

        ReactiveSyncInvocationHandler<K, V> handler = new ReactiveSyncInvocationHandler<>(connection, connection.reactive());
        return (RedisSentinelCommands<K, V>) Proxy.newProxyInstance(handler.getClass().getClassLoader(),
                new Class<?>[] { RedisSentinelCommands.class }, handler);
    }

}
