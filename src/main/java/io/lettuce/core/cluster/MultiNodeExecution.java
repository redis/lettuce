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

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

import io.lettuce.core.RedisCommandInterruptedException;
import io.lettuce.core.RedisException;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.cluster.api.NodeSelectionSupport;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.sync.NodeSelection;
import io.lettuce.core.cluster.api.sync.NodeSelectionCommands;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;

/**
 * Utility to perform and synchronize command executions on multiple cluster nodes.
 *
 * @author Mark Paluch
 */
class MultiNodeExecution {

    static <T> T execute(Callable<T> function) {
        try {
            return function.call();
        } catch (Exception e) {
            throw new RedisException(e);
        }
    }

    /**
     * Aggregate (sum) results of the {@link RedisFuture}s.
     *
     * @param executions mapping of a key to the future
     * @return future producing an aggregation result
     */
    protected static RedisFuture<Long> aggregateAsync(Map<?, RedisFuture<Long>> executions) {
        return new PipelinedRedisFuture<>(executions, objectPipelinedRedisFuture -> {
            AtomicLong result = new AtomicLong();
            for (RedisFuture<Long> future : executions.values()) {
                Long value = execute(() -> future.get());
                if (value != null) {
                    result.getAndAdd(value);
                }
            }

            return result.get();
        });
    }

    /**
     * Returns the result of the first {@link RedisFuture} and guarantee that all futures are finished.
     *
     * @param executions mapping of a key to the future
     * @param <T> result type
     * @return future returning the first result.
     */
    protected static <T> RedisFuture<T> firstOfAsync(Map<?, RedisFuture<T>> executions) {
        return new PipelinedRedisFuture<>(executions, objectPipelinedRedisFuture -> {
            // make sure, that all futures are executed before returning the result.
                for (RedisFuture<T> future : executions.values()) {
                    execute(() -> future.get());
                }
                for (RedisFuture<T> future : executions.values()) {
                    return execute(() -> future.get());
                }
                return null;
            });
    }

    /**
     * Returns always {@literal OK} and guarantee that all futures are finished.
     *
     * @param executions mapping of a key to the future
     * @return future returning the first result.
     */
    protected static RedisFuture<String> alwaysOkOfAsync(Map<?, RedisFuture<String>> executions) {
        return new PipelinedRedisFuture<>(executions, objectPipelinedRedisFuture -> {
            // make sure, that all futures are executed before returning the result.
                for (RedisFuture<String> future : executions.values()) {
                    try {
                        future.get();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RedisCommandInterruptedException(e);
                    } catch (ExecutionException e) {
                        // swallow exceptions
            }
        }
        return "OK";
    }   );
    }
}
