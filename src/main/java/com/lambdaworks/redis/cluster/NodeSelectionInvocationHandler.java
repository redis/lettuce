package com.lambdaworks.redis.cluster;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CompletionStage;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.reflect.AbstractInvocationHandler;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.cluster.api.NodeSelection;
import com.lambdaworks.redis.cluster.api.async.RedisClusterAsyncCommands;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
class NodeSelectionInvocationHandler extends AbstractInvocationHandler {

    private AbstractNodeSelection<?, ?, ?, ?> selection;
    private boolean sync;
    private Map<Method, Method> nodeSelectionMethods = new WeakHashMap<>();
    private Map<Method, Method> connectionMethod = new WeakHashMap<>();

    public NodeSelectionInvocationHandler(AbstractNodeSelection<?, ?, ?, ?> selection, boolean sync) {
        this.selection = selection;
        this.sync = sync;
    }

    @Override
    protected Object handleInvocation(Object proxy, Method method, Object[] args) throws Throwable {

        try {
            Method targetMethod = findMethod(RedisClusterAsyncCommands.class, method, connectionMethod);

            Map<RedisClusterNode, StatefulRedisConnection<?, ?>> connections = ImmutableMap.copyOf(selection.statefulMap());
            List<RedisClusterNode> nodes = ImmutableList.copyOf(selection.nodes());

            if (targetMethod != null) {

                Map<RedisClusterNode, CompletionStage<?>> executions = Maps.newHashMap();

                for (Map.Entry<RedisClusterNode, StatefulRedisConnection<?, ?>> entry : connections.entrySet()) {

                    CompletionStage<?> result = (CompletionStage<?>) targetMethod.invoke(entry.getValue().async(), args);
                    executions.put(entry.getKey(), result);
                }

                return new AsyncExecutionsImpl<>((Map) executions);
            }

            if (method.getName().equals("asMap")) {
                ImmutableMap.Builder<RedisClusterNode, Object> builder = ImmutableMap.builder();
                nodes.forEach(redisClusterNode -> {
                    if (sync) {
                        builder.put(redisClusterNode, connections.get(redisClusterNode).sync());
                    } else {
                        builder.put(redisClusterNode, connections.get(redisClusterNode).async());
                    }
                });

                return builder.build();
            }

            if (method.getName().equals("commands") && args.length == 0) {
                return proxy;
            }

            if (method.getName().equals("node") && args.length == 1) {
                RedisClusterNode redisClusterNode = nodes.get(((Number) args[0]).intValue());
                if (sync) {
                    return connections.get(redisClusterNode).sync();
                }
                return connections.get(redisClusterNode).async();
            }

            if (method.getName().equals("iterator")) {
                return nodes.stream().map(redisClusterNode -> {
                    if (sync) {
                        return connections.get(redisClusterNode).sync();
                    }
                    return connections.get(redisClusterNode).async();
                }).iterator();
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
}
