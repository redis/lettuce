package com.lambdaworks.redis.cluster;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;

/**
 * Completes
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public interface AsyncExecutions<T> extends Iterable<CompletionStage<T>> {

    Map<RedisClusterNode, CompletionStage<T>> asMap();

    Collection<RedisClusterNode> nodes();

    CompletionStage<T> get(RedisClusterNode redisClusterNode);

    CompletableFuture<T>[] futures();

    @Override
    default Iterator<CompletionStage<T>> iterator() {
        return asMap().values().iterator();
    }
}
