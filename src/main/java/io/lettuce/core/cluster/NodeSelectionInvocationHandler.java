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
package io.lettuce.core.cluster;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.RedisCommandInterruptedException;
import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.api.NodeSelectionSupport;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.internal.AbstractInvocationHandler;
import io.lettuce.core.internal.LettuceAssert;

/**
 * Invocation handler to trigger commands on multiple connections and return a holder for the values.
 *
 * @author Mark Paluch
 * @since 4.4
 */
class NodeSelectionInvocationHandler extends AbstractInvocationHandler {

    private static final Method NULL_MARKER_METHOD;

    private final Map<Method, Method> nodeSelectionMethods = new ConcurrentHashMap<>();
    private final Map<Method, Method> connectionMethod = new ConcurrentHashMap<>();
    private final Class<?> commandsInterface;

    private AbstractNodeSelection<?, ?, ?, ?> selection;
    private ExecutionModel executionModel;
    private long timeout;
    private TimeUnit unit;

    static {
        try {
            NULL_MARKER_METHOD = NodeSelectionInvocationHandler.class.getDeclaredMethod("handleInvocation", Object.class,
                    Method.class, Object[].class);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    NodeSelectionInvocationHandler(AbstractNodeSelection<?, ?, ?, ?> selection, Class<?> commandsInterface,
            ExecutionModel executionModel) {
        this(selection, commandsInterface, 0, null, executionModel);
    }

    NodeSelectionInvocationHandler(AbstractNodeSelection<?, ?, ?, ?> selection, Class<?> commandsInterface, long timeout,
            TimeUnit unit) {
        this(selection, commandsInterface, timeout, unit, ExecutionModel.SYNC);
    }

    private NodeSelectionInvocationHandler(AbstractNodeSelection<?, ?, ?, ?> selection, Class<?> commandsInterface,
            long timeout, TimeUnit unit, ExecutionModel executionModel) {

        if (executionModel == ExecutionModel.SYNC) {
            LettuceAssert.isTrue(timeout > 0, "Timeout must be greater 0 when using sync mode");
            LettuceAssert.notNull(unit, "Unit must not be null when using sync mode");
        }

        LettuceAssert.notNull(executionModel, "ExecutionModel must not be null");

        this.selection = selection;
        this.commandsInterface = commandsInterface;
        this.unit = unit;
        this.timeout = timeout;
        this.executionModel = executionModel;
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected Object handleInvocation(Object proxy, Method method, Object[] args) throws Throwable {

        try {
            Method targetMethod = findMethod(commandsInterface, method, connectionMethod);

            Map<RedisClusterNode, StatefulRedisConnection<?, ?>> connections = new HashMap<>(selection.size(), 1);
            connections.putAll(selection.statefulMap());

            if (targetMethod != null) {

                Map<RedisClusterNode, Object> executions = new HashMap<>();

                for (Map.Entry<RedisClusterNode, StatefulRedisConnection<?, ?>> entry : connections.entrySet()) {

                    StatefulRedisConnection<?, ?> connection = entry.getValue();
                    Object result = targetMethod.invoke(
                            executionModel == ExecutionModel.REACTIVE ? connection.reactive() : connection.async(), args);

                    executions.put(entry.getKey(), result);
                }

                if (executionModel == ExecutionModel.SYNC) {

                    Map<RedisClusterNode, CompletionStage<?>> asyncExecutions = (Map) executions;
                    if (!awaitAll(timeout, unit, asyncExecutions.values())) {
                        throw createTimeoutException(asyncExecutions);
                    }

                    if (atLeastOneFailed(asyncExecutions)) {
                        throw createExecutionException(asyncExecutions);
                    }

                    return new SyncExecutionsImpl(asyncExecutions);
                }

                if (executionModel == ExecutionModel.REACTIVE) {
                    return new ReactiveExecutionsImpl(executions);

                }
                return new AsyncExecutionsImpl(executions);
            }

            if (method.getName().equals("commands") && args.length == 0) {
                return proxy;
            }

            targetMethod = findMethod(NodeSelectionSupport.class, method, nodeSelectionMethods);
            return targetMethod.invoke(selection, args);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }

    public static boolean awaitAll(long timeout, TimeUnit unit, Collection<CompletionStage<?>> futures) {
        boolean complete;

        try {
            long nanos = unit.toNanos(timeout);
            long time = System.nanoTime();

            for (CompletionStage<?> f : futures) {
                if (nanos < 0) {
                    return false;
                }
                try {
                    f.toCompletableFuture().get(nanos, TimeUnit.NANOSECONDS);
                } catch (ExecutionException e) {
                    // ignore
                }
                long now = System.nanoTime();
                nanos -= now - time;
                time = now;
            }

            complete = true;
        } catch (TimeoutException e) {
            complete = false;
        } catch (Exception e) {
            throw new RedisCommandInterruptedException(e);
        }

        return complete;
    }

    private boolean atLeastOneFailed(Map<RedisClusterNode, CompletionStage<?>> executions) {
        return executions.values().stream()
                .anyMatch(completionStage -> completionStage.toCompletableFuture().isCompletedExceptionally());
    }

    private RedisCommandTimeoutException createTimeoutException(Map<RedisClusterNode, CompletionStage<?>> executions) {
        List<RedisClusterNode> notFinished = new ArrayList<>();
        executions.forEach((redisClusterNode, completionStage) -> {
            if (!completionStage.toCompletableFuture().isDone()) {
                notFinished.add(redisClusterNode);
            }
        });
        String description = getNodeDescription(notFinished);
        return new RedisCommandTimeoutException("Command timed out for node(s): " + description);
    }

    private RedisCommandExecutionException createExecutionException(Map<RedisClusterNode, CompletionStage<?>> executions) {
        List<RedisClusterNode> failed = new ArrayList<>();
        executions.forEach((redisClusterNode, completionStage) -> {
            if (!completionStage.toCompletableFuture().isCompletedExceptionally()) {
                failed.add(redisClusterNode);
            }
        });

        RedisCommandExecutionException e = new RedisCommandExecutionException(
                "Multi-node command execution failed on node(s): " + getNodeDescription(failed));

        executions.forEach((redisClusterNode, completionStage) -> {
            CompletableFuture<?> completableFuture = completionStage.toCompletableFuture();
            if (completableFuture.isCompletedExceptionally()) {
                try {
                    completableFuture.get();
                } catch (Exception innerException) {

                    if (innerException instanceof ExecutionException) {
                        e.addSuppressed(innerException.getCause());
                    } else {
                        e.addSuppressed(innerException);
                    }
                }
            }
        });
        return e;
    }

    private String getNodeDescription(List<RedisClusterNode> notFinished) {
        return String.join(", ", notFinished.stream().map(this::getDescriptor).collect(Collectors.toList()));
    }

    private String getDescriptor(RedisClusterNode redisClusterNode) {
        StringBuffer buffer = new StringBuffer(redisClusterNode.getNodeId());
        buffer.append(" (");

        if (redisClusterNode.getUri() != null) {
            buffer.append(redisClusterNode.getUri().getHost()).append(':').append(redisClusterNode.getUri().getPort());
        }

        buffer.append(')');
        return buffer.toString();
    }

    private Method findMethod(Class<?> type, Method method, Map<Method, Method> cache) {

        Method result = cache.get(method);
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

    enum ExecutionModel {
        SYNC, ASYNC, REACTIVE;
    }
}
