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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import io.lettuce.core.cluster.api.async.AsyncExecutions;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;

/**
 * @author Mark Paluch
 */
class AsyncExecutionsImpl<T> implements AsyncExecutions<T> {

    private Map<RedisClusterNode, CompletionStage<T>> executions;

    public AsyncExecutionsImpl(Map<RedisClusterNode, CompletionStage<T>> executions) {
        this.executions = Collections.unmodifiableMap(new HashMap<>(executions));
    }

    @Override
    public Map<RedisClusterNode, CompletionStage<T>> asMap() {
        return executions;
    }

    @Override
    public Collection<RedisClusterNode> nodes() {
        return executions.keySet();
    }

    @Override
    public CompletionStage<T> get(RedisClusterNode redisClusterNode) {
        return executions.get(redisClusterNode);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public CompletableFuture<T>[] futures() {
        return executions.values().toArray(new CompletableFuture[executions.size()]);
    }
}
