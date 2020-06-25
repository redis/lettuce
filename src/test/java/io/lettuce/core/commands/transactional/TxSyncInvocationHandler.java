/*
 * Copyright 2011-2020 the original author or authors.
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
package io.lettuce.core.commands.transactional;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import io.lettuce.core.TransactionResult;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.internal.AbstractInvocationHandler;

/**
 * Invocation handler for testing purposes that wraps each call into a transaction.
 *
 * @param <K>
 * @param <V>
 */
class TxSyncInvocationHandler<K, V> extends AbstractInvocationHandler {

    private final Object api;

    private final Method multi;

    private final Method discard;

    private final Method exec;

    private final Method ping;

    private TxSyncInvocationHandler(Object api) throws Exception {

        this.api = api;
        this.multi = api.getClass().getMethod("multi");
        this.exec = api.getClass().getMethod("exec");
        this.discard = api.getClass().getMethod("discard");
        this.ping = api.getClass().getMethod("ping");
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Object handleInvocation(Object proxy, Method method, Object[] args) throws Throwable {

        try {

            if (method.getName().equals("exec") || method.getName().equals("multi")) {
                throw new IllegalStateException("Cannot execute transaction commands over this transactional wrapper");
            }

            Method targetMethod = api.getClass().getMethod(method.getName(), method.getParameterTypes());

            if (!method.getName().equals("close") && !method.getName().equals("getStatefulConnection")) {

                multi.invoke(api);
                ping.invoke(api);

                targetMethod.invoke(api, args);

                Object result = exec.invoke(api);

                if (result == null || !(result instanceof TransactionResult)) {
                    return result;
                }

                TransactionResult txResult = (TransactionResult) result;

                if (txResult.size() > 1) {

                    result = txResult.get(1);
                    if (result instanceof Exception) {
                        throw (Exception) result;
                    }

                    return result;
                }

                return null;
            }

            return targetMethod.invoke(api, args);

        } catch (InvocationTargetException e) {
            try {
                discard.invoke(api);
            } catch (Exception e1) {
            }
            throw e.getTargetException();
        }
    }

    /**
     * Create a transactional wrapper proxy for {@link RedisCommands}.
     *
     * @param connection the connection
     * @return the wrapper proxy.
     */
    @SuppressWarnings("unchecked")
    public static <K, V> RedisCommands<K, V> sync(StatefulRedisConnection<K, V> connection) {

        try {
            TxSyncInvocationHandler<K, V> handler = new TxSyncInvocationHandler<>(connection.sync());
            return (RedisCommands<K, V>) Proxy.newProxyInstance(handler.getClass().getClassLoader(),
                    new Class<?>[] { RedisCommands.class }, handler);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

}
