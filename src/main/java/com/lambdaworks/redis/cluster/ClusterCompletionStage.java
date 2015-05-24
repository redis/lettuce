package com.lambdaworks.redis.cluster;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

import com.lambdaworks.redis.cluster.models.partitions.RedisClusterNode;

/**
 * Completes
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public interface ClusterCompletionStage<T> {

    Map<RedisClusterNode, CompletionStage<T>> asMap();

    Collection<RedisClusterNode> nodes();

    Collection<CompletionStage<T>> stages();

    CompletionStage<T> get(RedisClusterNode redisClusterNode);

    CompletionStage<T> any();

    CompletionStage<List<T>> all();

}
