/*
 * Copyright 2016-2017 the original author or authors.
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
package com.lambdaworks.redis.cluster;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import com.lambdaworks.redis.LettuceFutures;
import com.lambdaworks.redis.RedisFuture;
import com.lambdaworks.redis.api.StatefulConnection;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.cluster.api.NodeSelectionSupport;
import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;
import com.lambdaworks.redis.cluster.api.sync.RedisClusterCommands;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;
import com.lambdaworks.redis.internal.AbstractInvocationHandler;
import com.lambdaworks.redis.internal.DefaultMethods;

/**
 * Invocation-handler to synchronize API calls which use Futures as backend. This class leverages the need to implement a full
 * sync class which just delegates every request.
 *
 * @param <K> Key type.
 * @param <V> Value type.
 * @author Mark Paluch
 * @since 3.0
 */
@SuppressWarnings("unchecked")
class ClusterFutureSyncInvocationHandler<K, V> extends AbstractInvocationHandler {

    private final StatefulConnection<K, V> connection;
    private final Class<?> asyncCommandsInterface;
    private final Class<?> nodeSelectionInterface;
    private final Class<?> nodeSelectionCommandsInterface;
    private final Object asyncApi;

    private final Map<Method, Method> apiMethodCache = new ConcurrentHashMap<>(RedisClusterCommands.class.getMethods().length,
            1);
    private final Map<Method, Method> connectionMethodCache = new ConcurrentHashMap<>(5, 1);
    private final Map<Method, MethodHandle> methodHandleCache = new ConcurrentHashMap<>(5, 1);

    ClusterFutureSyncInvocationHandler(StatefulConnection<K, V> connection, Class<?> asyncCommandsInterface,
            Class<?> nodeSelectionInterface, Class<?> nodeSelectionCommandsInterface, Object asyncApi) {
        this.connection = connection;
        this.asyncCommandsInterface = asyncCommandsInterface;
        this.nodeSelectionInterface = nodeSelectionInterface;
        this.nodeSelectionCommandsInterface = nodeSelectionCommandsInterface;
        this.asyncApi = asyncApi;
    }

    /**
     * @see AbstractInvocationHandler#handleInvocation(Object, Method, Object[])
     */
    @Override
    protected Object handleInvocation(Object proxy, Method method, Object[] args) throws Throwable {

        try {

            if (method.isDefault()) {
                return methodHandleCache.computeIfAbsent(method, ClusterFutureSyncInvocationHandler::lookupDefaultMethod)
                        .bindTo(proxy).invokeWithArguments(args);
            }

            if (method.getName().equals("getConnection") && args.length > 0) {
                return getConnection(method, args);
            }

            if (method.getName().equals("readonly") && args.length == 1) {
                return nodes((Predicate<RedisClusterNode>) args[0], ClusterConnectionProvider.Intent.READ, false);
            }

            if (method.getName().equals("nodes") && args.length == 1) {
                return nodes((Predicate<RedisClusterNode>) args[0], ClusterConnectionProvider.Intent.WRITE, false);
            }

            if (method.getName().equals("nodes") && args.length == 2) {
                return nodes((Predicate<RedisClusterNode>) args[0], ClusterConnectionProvider.Intent.WRITE, (Boolean) args[1]);
            }

            Method targetMethod = apiMethodCache.computeIfAbsent(method, key -> {

                try {
                    return asyncApi.getClass().getMethod(key.getName(), key.getParameterTypes());
                } catch (NoSuchMethodException e) {
                    throw new IllegalStateException(e);
                }
            });

            Object result = targetMethod.invoke(asyncApi, args);

            if (result instanceof RedisFuture) {
                RedisFuture<?> command = (RedisFuture<?>) result;
                if (!method.getName().equals("exec") && !method.getName().equals("multi")) {
                    if (connection instanceof StatefulRedisConnection && ((StatefulRedisConnection) connection).isMulti()) {
                        return null;
                    }
                }
                return LettuceFutures.awaitOrCancel(command, connection.getTimeout(), connection.getTimeoutUnit());
            }

            return result;

        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }

    private Object getConnection(Method method, Object[] args) throws Exception {

        Method targetMethod = connectionMethodCache.computeIfAbsent(method, this::lookupMethod);

        Object result = targetMethod.invoke(connection, args);
        if (result instanceof StatefulRedisClusterConnection) {
            StatefulRedisClusterConnection<K, V> connection = (StatefulRedisClusterConnection<K, V>) result;
            return connection.sync();
        }

        if (result instanceof StatefulRedisConnection) {
            StatefulRedisConnection<K, V> connection = (StatefulRedisConnection<K, V>) result;
            return connection.sync();
        }

        throw new IllegalArgumentException("Cannot call method " + method);
    }

    private Method lookupMethod(Method key) {
        try {
            return connection.getClass().getMethod(key.getName(), key.getParameterTypes());
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(e);
        }
    }

    protected Object nodes(Predicate<RedisClusterNode> predicate, ClusterConnectionProvider.Intent intent, boolean dynamic) {

        NodeSelectionSupport<RedisCommands<K, V>, ?> selection = null;

        if (connection instanceof StatefulRedisClusterConnectionImpl) {

            StatefulRedisClusterConnectionImpl<K, V> impl = (StatefulRedisClusterConnectionImpl<K, V>) connection;

            if (dynamic) {
                selection = new DynamicNodeSelection<>(impl.getClusterDistributionChannelWriter(), predicate, intent,
                        StatefulRedisConnection::sync);
            } else {

                selection = new StaticNodeSelection<>(impl.getClusterDistributionChannelWriter(), predicate, intent,
                        StatefulRedisConnection::sync);
            }
        }

        if (connection instanceof StatefulRedisClusterPubSubConnectionImpl) {

            StatefulRedisClusterPubSubConnectionImpl<K, V> impl = (StatefulRedisClusterPubSubConnectionImpl<K, V>) connection;
            selection = new StaticNodeSelection<>(impl.getClusterDistributionChannelWriter(), predicate, intent,
                    StatefulRedisConnection::sync);
        }

        NodeSelectionInvocationHandler h = new NodeSelectionInvocationHandler((AbstractNodeSelection<?, ?, ?, ?>) selection,
                asyncCommandsInterface, connection.getTimeout(), connection.getTimeoutUnit());
        return Proxy.newProxyInstance(NodeSelectionSupport.class.getClassLoader(), new Class<?>[] {
                nodeSelectionCommandsInterface, nodeSelectionInterface }, h);
    }

    private static MethodHandle lookupDefaultMethod(Method method) {

        try {
            return DefaultMethods.lookupMethodHandle(method);
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
