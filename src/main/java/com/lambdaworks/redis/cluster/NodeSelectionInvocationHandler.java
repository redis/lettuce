package com.lambdaworks.redis.cluster;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CompletionStage;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.reflect.AbstractInvocationHandler;
import com.lambdaworks.redis.RedisClusterAsyncConnection;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
class NodeSelectionInvocationHandler extends AbstractInvocationHandler {

    private NodeSelection<?, ?> selection;
    private Map<Method, Method> nodeSelectionMethods = new WeakHashMap<>();
    private Map<Method, Method> connectionMethod = new WeakHashMap<>();

    public NodeSelectionInvocationHandler(NodeSelection<?, ?> selection) {
        this.selection = selection;
    }

    @Override
    protected Object handleInvocation(Object proxy, Method method, Object[] args) throws Throwable {

        try {
            Method targetMethod = findMethod(RedisClusterAsyncConnection.class, method, connectionMethod);
            if (targetMethod != null) {
                Map<RedisClusterNode, ? extends RedisClusterAsyncConnection<?, ?>> connections = ImmutableMap.copyOf(selection
                        .asMap());
                Map<RedisClusterNode, CompletionStage<?>> executions = Maps.newHashMap();

                for (Map.Entry<RedisClusterNode, ? extends RedisClusterAsyncConnection<?, ?>> entry : connections.entrySet()) {

                    CompletionStage<?> result = (CompletionStage<?>) targetMethod.invoke(entry.getValue(), args);
                    executions.put(entry.getKey(), result);
                }

                return new AsyncExecutionsImpl<>((Map) executions);

            }

            targetMethod = findMethod(NodeSelection.class, method, nodeSelectionMethods);
            return targetMethod.invoke(selection, args);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }

    private Method findMethod(Class<?> type, Method method, Map<Method, Method> cache) {
        if (cache.containsKey(method)) {
            return cache.get(method);
        }

        for (Method typeMethod : type.getMethods()) {
            if (!typeMethod.getName().equals(method.getName())
                    || !Arrays.equals(typeMethod.getParameterTypes(), method.getParameterTypes())) {
                continue;
            }

            synchronized (cache) {
                cache.put(method, typeMethod);
                return typeMethod;
            }
        }

        // Null-marker to avoid full class method scans.
        cache.put(method, null);
        return null;

    }

    private static class Tuple<K, V> {
        public K key;
        public V value;

        public Tuple(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }
}
