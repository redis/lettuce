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
package io.lettuce.core;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.internal.AbstractInvocationHandler;
import io.lettuce.core.internal.TimeoutProvider;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.protocol.ProtocolKeyword;
import io.lettuce.core.protocol.RedisCommand;

/**
 * Invocation-handler to synchronize API calls which use Futures as backend. This class leverages the need to implement a full
 * sync class which just delegates every request.
 *
 * @author Mark Paluch
 * @author Tz Zhuo
 * @since 3.0
 */
class FutureSyncInvocationHandler extends AbstractInvocationHandler {

    private final StatefulConnection<?, ?> connection;

    private final TimeoutProvider timeoutProvider;

    private final Object asyncApi;

    private final MethodTranslator translator;

    FutureSyncInvocationHandler(StatefulConnection<?, ?> connection, Object asyncApi, Class<?>[] interfaces) {
        this.connection = connection;
        this.timeoutProvider = new TimeoutProvider(() -> connection.getOptions().getTimeoutOptions(),
                () -> connection.getTimeout().toNanos());
        this.asyncApi = asyncApi;
        this.translator = MethodTranslator.of(asyncApi.getClass(), interfaces);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Object handleInvocation(Object proxy, Method method, Object[] args) throws Throwable {

        try {

            Method targetMethod = this.translator.get(method);
            Object result = targetMethod.invoke(asyncApi, args);

            if (result instanceof RedisFuture<?>) {

                RedisFuture<?> command = (RedisFuture<?>) result;

                if (!isTxControlMethod(method.getName(), args) && isTransactionActive(connection)) {
                    return null;
                }

                long timeout = getTimeoutNs(command);

                return LettuceFutures.awaitOrCancel(command, timeout, TimeUnit.NANOSECONDS);
            }

            return result;
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }

    private long getTimeoutNs(RedisFuture<?> command) {

        if (command instanceof RedisCommand) {
            return timeoutProvider.getTimeoutNs((RedisCommand) command);
        }

        return connection.getTimeout().toNanos();
    }

    private static boolean isTransactionActive(StatefulConnection<?, ?> connection) {
        return connection instanceof StatefulRedisConnection && ((StatefulRedisConnection) connection).isMulti();
    }

    private static boolean isTxControlMethod(String methodName, Object[] args) {

        if (methodName.equals("exec") || methodName.equals("multi") || methodName.equals("discard")) {
            return true;
        }

        if (methodName.equals("dispatch") && args.length > 0 && args[0] instanceof ProtocolKeyword) {

            ProtocolKeyword keyword = (ProtocolKeyword) args[0];
            if (keyword.name().equals(CommandType.MULTI.name()) || keyword.name().equals(CommandType.EXEC.name())
                    || keyword.name().equals(CommandType.DISCARD.name())) {
                return true;
            }
        }

        return false;
    }

}
