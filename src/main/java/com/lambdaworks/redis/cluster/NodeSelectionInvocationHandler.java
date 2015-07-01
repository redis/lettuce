package com.lambdaworks.redis.cluster;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
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
    private Cache<Method, Method> nodeSelectionMethods = CacheBuilder.newBuilder().build();
    private Cache<Method, Method> connectionMethod = CacheBuilder.newBuilder().build();
    public final static Method NULL_MARKER_METHOD;

    static {
        try {
            NULL_MARKER_METHOD = NodeSelectionInvocationHandler.class.getDeclaredMethod("handleInvocation", Object.class,
                    Method.class, Object[].class);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

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

            if (method.getName().equals("commands") && args.length == 0) {
                return proxy;
            }

            targetMethod = findMethod(NodeSelection.class, method, nodeSelectionMethods);
            return targetMethod.invoke(selection, args);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }

    private Method findMethod(Class<?> type, Method method, Cache<Method, Method> cache) {

        Method result = cache.getIfPresent(method);
        if (result != null && result != NULL_MARKER_METHOD) {
            return result;
        }

        for (Method typeMethod : type.getMethods()) {
            if (!typeMethod.getName().equals(method.getName())
                    || !Arrays.equals(typeMethod.getParameterTypes(), method.getParameterTypes())) {
                continue;
            }

            cache.put(method, typeMethod);
            return typeMethod;
        }

        // Null-marker to avoid full class method scans.
        cache.put(method, NULL_MARKER_METHOD);
        return null;
    }
}
