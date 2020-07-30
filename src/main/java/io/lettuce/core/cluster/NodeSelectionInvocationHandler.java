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
package io.lettuce.core.cluster;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.reactivestreams.Publisher;

import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.cluster.api.NodeSelectionSupport;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.internal.AbstractInvocationHandler;
import io.lettuce.core.internal.ExceptionFactory;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.internal.Futures;
import io.lettuce.core.internal.TimeoutProvider;
import io.lettuce.core.protocol.RedisCommand;

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

    private final AbstractNodeSelection<?, ?, ?, ?> selection;

    private final ExecutionModel executionModel;

    private final TimeoutProvider timeoutProvider;

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
        this(selection, commandsInterface, null, executionModel);
    }

    NodeSelectionInvocationHandler(AbstractNodeSelection<?, ?, ?, ?> selection, Class<?> commandsInterface,
            TimeoutProvider timeoutProvider) {
        this(selection, commandsInterface, timeoutProvider, ExecutionModel.SYNC);
    }

    private NodeSelectionInvocationHandler(AbstractNodeSelection<?, ?, ?, ?> selection, Class<?> commandsInterface,
            TimeoutProvider timeoutProvider, ExecutionModel executionModel) {

        if (executionModel == ExecutionModel.SYNC) {
            LettuceAssert.notNull(timeoutProvider, "TimeoutProvider must not be null");
        }

        LettuceAssert.notNull(executionModel, "ExecutionModel must not be null");

        this.selection = selection;
        this.commandsInterface = commandsInterface;
        this.timeoutProvider = timeoutProvider;
        this.executionModel = executionModel;
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected Object handleInvocation(Object proxy, Method method, Object[] args) throws Throwable {

        try {

            if (method.getName().equals("commands") && args.length == 0) {
                return proxy;
            }

            Method targetMethod = findMethod(commandsInterface, method, connectionMethod);

            if (targetMethod == null) {

                Method nodeSelectionMethod = findMethod(NodeSelectionSupport.class, method, nodeSelectionMethods);
                return nodeSelectionMethod.invoke(selection, args);
            }

            Map<RedisClusterNode, CompletableFuture<? extends StatefulRedisConnection<?, ?>>> connections = new LinkedHashMap<>(
                    selection.size(), 1);
            connections.putAll(selection.statefulMap());
            Map<RedisClusterNode, Object> executions = new LinkedHashMap<>(selection.size(), 1);

            AtomicLong timeout = new AtomicLong();

            for (Map.Entry<RedisClusterNode, CompletableFuture<? extends StatefulRedisConnection<?, ?>>> entry : connections
                    .entrySet()) {

                CompletableFuture<? extends StatefulRedisConnection<?, ?>> connection = entry.getValue();

                CompletableFuture<Object> result = connection.thenCompose(it -> {

                    try {

                        Object resultValue = targetMethod
                                .invoke(executionModel == ExecutionModel.REACTIVE ? it.reactive() : it.async(), args);

                        if (timeoutProvider != null && resultValue instanceof RedisCommand && timeout.get() == 0) {
                            timeout.set(timeoutProvider.getTimeoutNs((RedisCommand) resultValue));
                        }

                        if (resultValue instanceof CompletionStage<?>) {
                            return (CompletionStage<Object>) resultValue;
                        }

                        return CompletableFuture.completedFuture(resultValue);
                    } catch (InvocationTargetException e) {

                        CompletableFuture<Object> future = new CompletableFuture<>();
                        future.completeExceptionally(e.getTargetException());
                        return future;
                    } catch (Exception e) {

                        CompletableFuture<Object> future = new CompletableFuture<>();
                        future.completeExceptionally(e);
                        return future;
                    }
                });

                executions.put(entry.getKey(), result);
            }

            return getExecutions(executions, timeout.get());
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }

    @SuppressWarnings("unchecked")
    private Object getExecutions(Map<RedisClusterNode, Object> executions, long timeoutNs)
            throws ExecutionException, InterruptedException {

        if (executionModel == ExecutionModel.REACTIVE) {
            Map<RedisClusterNode, CompletionStage<? extends Publisher<?>>> reactiveExecutions = (Map) executions;
            return new ReactiveExecutionsImpl<>(reactiveExecutions);
        }

        Map<RedisClusterNode, CompletionStage<?>> asyncExecutions = (Map) executions;

        if (executionModel == ExecutionModel.SYNC) {

            long timeoutToUse = timeoutNs >= 0 ? timeoutNs : timeoutProvider.getTimeoutNs(null);

            if (!awaitAll(timeoutToUse, TimeUnit.NANOSECONDS, asyncExecutions.values())) {
                throw createTimeoutException(asyncExecutions, Duration.ofNanos(timeoutToUse));
            }

            if (atLeastOneFailed(asyncExecutions)) {
                throw createExecutionException(asyncExecutions);
            }

            return new SyncExecutionsImpl<>(asyncExecutions);
        }

        return new AsyncExecutionsImpl<>(asyncExecutions);
    }

    @SuppressWarnings("rawtypes")
    private static boolean awaitAll(long timeout, TimeUnit unit, Collection<CompletionStage<?>> stages) {

        CompletableFuture[] futures = new CompletableFuture[stages.size()];

        int i = 0;
        for (CompletionStage<?> completableFuture : stages) {
            futures[i++] = completableFuture.toCompletableFuture();
        }

        return Futures.awaitAll(timeout, unit, futures);
    }

    private boolean atLeastOneFailed(Map<RedisClusterNode, CompletionStage<?>> executions) {
        return executions.values().stream()
                .anyMatch(completionStage -> completionStage.toCompletableFuture().isCompletedExceptionally());
    }

    private RedisCommandTimeoutException createTimeoutException(Map<RedisClusterNode, CompletionStage<?>> executions,
            Duration timeout) {

        List<RedisClusterNode> notFinished = new ArrayList<>();
        executions.forEach((redisClusterNode, completionStage) -> {
            if (!completionStage.toCompletableFuture().isDone()) {
                notFinished.add(redisClusterNode);
            }
        });

        String description = getNodeDescription(notFinished);
        return ExceptionFactory.createTimeoutException("Command timed out for node(s): " + description, timeout);
    }

    private RedisCommandExecutionException createExecutionException(Map<RedisClusterNode, CompletionStage<?>> executions) {

        List<RedisClusterNode> failed = new ArrayList<>();
        executions.forEach((redisClusterNode, completionStage) -> {
            if (!completionStage.toCompletableFuture().isCompletedExceptionally()) {
                failed.add(redisClusterNode);
            }
        });

        RedisCommandExecutionException e = ExceptionFactory
                .createExecutionException("Multi-node command execution failed on node(s): " + getNodeDescription(failed));

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

        StringBuilder buffer = new StringBuilder(redisClusterNode.getNodeId());
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
        SYNC, ASYNC, REACTIVE
    }

}
