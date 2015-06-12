package com.lambdaworks.redis.cluster.api.async;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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

    /**
     *
     * @return a {@code Spliterator} over the CompletionStages in this collection
     * @since 4.0
     */
    @Override
    default Spliterator<CompletionStage<T>> spliterator() {
        return Spliterators.spliterator(iterator(), nodes().size(), 0);
    }

    /**
     * @return a sequential {@code Stream} over the elements in this collection
     * @since 4.0
     */
    default Stream<CompletionStage<T>> stream() {
        return StreamSupport.stream(spliterator(), false);
    }
}
