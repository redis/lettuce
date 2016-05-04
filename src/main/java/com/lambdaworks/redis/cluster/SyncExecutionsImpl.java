package com.lambdaworks.redis.cluster;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import com.lambdaworks.redis.cluster.api.sync.Executions;
import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;

/**
 * @author Mark Paluch
 */
class SyncExecutionsImpl<T> implements Executions<T> {

    private Map<RedisClusterNode, T> executions;

    public SyncExecutionsImpl(Map<RedisClusterNode, CompletionStage<T>> executions) throws ExecutionException,
            InterruptedException {

        Map<RedisClusterNode, T> result = new HashMap<>();
        for (Map.Entry<RedisClusterNode, CompletionStage<T>> entry : executions.entrySet()) {
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
