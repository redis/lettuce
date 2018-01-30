/*
 * Copyright 2011-2018 the original author or authors.
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
package io.lettuce.core.cluster.api.async;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import io.lettuce.core.cluster.models.partitions.RedisClusterNode;

/**
 * Result holder for a command that was executed asynchronously on multiple nodes.
 *
 * @author Mark Paluch
 * @since 4.0
 */
public interface AsyncExecutions<T> extends Iterable<CompletableFuture<T>> {

    /**
     *
     * @return map between {@link RedisClusterNode} and the {@link CompletionStage}
     */
    Map<RedisClusterNode, CompletableFuture<T>> asMap();

    /**
     *
     * @return collection of nodes on which the command was executed.
     */
    Collection<RedisClusterNode> nodes();

    /**
     *
     * @param redisClusterNode the node
     * @return the completion stage for this node
     */
    CompletionStage<T> get(RedisClusterNode redisClusterNode);

    /**
     *
     * @return array of futures.
     */
    CompletableFuture<T>[] futures();

    /**
     * @return a sequential {@code Stream} over the {@link CompletionStage CompletionStages} in this collection
     */
    default Stream<CompletableFuture<T>> stream() {
        return StreamSupport.stream(spliterator(), false);
    }
}
