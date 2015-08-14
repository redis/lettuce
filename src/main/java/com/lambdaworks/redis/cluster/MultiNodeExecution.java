package com.lambdaworks.redis.cluster;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

import com.lambdaworks.redis.RedisCommandInterruptedException;
import com.lambdaworks.redis.RedisException;
import com.lambdaworks.redis.RedisFuture;

/**
 * Utility to perform and synchronize command executions on multiple cluster nodes.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
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
                        throw new RedisCommandInterruptedException(e);
                    } catch (ExecutionException e) {
                        // swallow exceptions
            }
        }
        return "OK";
    }   );
    }
}
