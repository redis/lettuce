package io.lettuce.core.cluster;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import io.lettuce.core.cluster.api.sync.Executions;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;

/**
 * @author Mark Paluch TODO: Add timeout handling
 */
class SyncExecutionsImpl<T> implements Executions<T> {

    private Map<RedisClusterNode, T> executions;

    public SyncExecutionsImpl(Map<RedisClusterNode, CompletionStage<? extends T>> executions)
            throws ExecutionException, InterruptedException {

        Map<RedisClusterNode, T> result = new HashMap<>(executions.size(), 1);
        for (Map.Entry<RedisClusterNode, CompletionStage<? extends T>> entry : executions.entrySet()) {
            result.put(entry.getKey(), entry.getValue().toCompletableFuture().get());
        }

        this.executions = result;
    }

    @Override
    public Map<RedisClusterNode, T> asMap() {
        return executions;
    }

    @Override
    public Collection<RedisClusterNode> nodes() {
        return executions.keySet();
    }

    @Override
    public T get(RedisClusterNode redisClusterNode) {
        return executions.get(redisClusterNode);
    }

}
