package com.lambdaworks.redis.cluster;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.lambdaworks.redis.RedisCommandExecutionException;
import com.lambdaworks.redis.RedisCommandInterruptedException;
import com.lambdaworks.redis.RedisCommandTimeoutException;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.cluster.api.NodeSelectionSupport;
import com.lambdaworks.redis.cluster.api.async.RedisClusterAsyncCommands;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;
import com.lambdaworks.redis.internal.AbstractInvocationHandler;
import com.lambdaworks.redis.internal.LettuceAssert;

/**
 * Invocation handler to trigger commands on multiple connections and return a holder for the values.
 * 
 * @author Mark Paluch
 */
class NodeSelectionInvocationHandler extends AbstractInvocationHandler {

    private AbstractNodeSelection<?, ?, ?, ?> selection;
    private boolean sync;
    private long timeout;
    private TimeUnit unit;
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

    public NodeSelectionInvocationHandler(AbstractNodeSelection<?, ?, ?, ?> selection) {
        this(selection, false, 0, null);
    }

    public NodeSelectionInvocationHandler(AbstractNodeSelection<?, ?, ?, ?> selection, boolean sync, long timeout, TimeUnit unit) {
        if (sync) {
            LettuceAssert.isTrue(timeout > 0, "timeout must be greater 0 when using sync mode");
            LettuceAssert.notNull(unit, "unit must not be null when using sync mode");
        }

        this.selection = selection;
        this.sync = sync;
        this.unit = unit;
        this.timeout = timeout;
    }

    @Override
    @SuppressWarnings("rawtypes")
    protected Object handleInvocation(Object proxy, Method method, Object[] args) throws Throwable {

        try {
            Method targetMethod = findMethod(RedisClusterAsyncCommands.class, method, connectionMethod);

            Map<RedisClusterNode, StatefulRedisConnection<?, ?>> connections = new HashMap<>(selection.statefulMap());

            if (targetMethod != null) {

                Map<RedisClusterNode, CompletionStage<?>> executions = new HashMap<>();
                for (Map.Entry<RedisClusterNode, StatefulRedisConnection<?, ?>> entry : connections.entrySet()) {

                    CompletionStage<?> result = (CompletionStage<?>) targetMethod.invoke(entry.getValue().async(), args);
                    executions.put(entry.getKey(), result);
                }

                if (sync) {
                    if (!awaitAll(timeout, unit, executions.values())) {
                        RedisCommandTimeoutException e = createTimeoutException(executions);
                        throw e;
                    }

                    if (atLeastOneFailed(executions)) {
                        RedisCommandExecutionException e = createExecutionException(executions);
                        throw e;
                    }

                    return new SyncExecutionsImpl(executions);

                }
                return new AsyncExecutionsImpl<>((Map) executions);
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
                .filter(completionStage -> completionStage.toCompletableFuture().isCompletedExceptionally()).findFirst()
                .isPresent();
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
        return String.join(", ",
                notFinished.stream().map(redisClusterNode -> getDescriptor(redisClusterNode)).collect(Collectors.toList()));
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
