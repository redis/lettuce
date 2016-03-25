package com.lambdaworks.redis.cluster;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.lambdaworks.redis.RedisFuture;

/**
 * Pipelining for commands that are executed on multiple cluster nodes. Merges results and emits one composite result.
 * 
 * @author Mark Paluch
 */
class PipelinedRedisFuture<V> extends CompletableFuture<V> implements RedisFuture<V> {

    private CountDownLatch latch = new CountDownLatch(1);

    public PipelinedRedisFuture(CompletionStage<V> completionStage, Function<V, V> converter) {
        completionStage.thenAccept(v -> complete(converter.apply(v)))
                .exceptionally(throwable -> {
                    completeExceptionally(throwable);
                    return null;
                });
    }

    public PipelinedRedisFuture(Map<?, ? extends RedisFuture<?>> executions, Function<PipelinedRedisFuture<V>, V> converter) {

        CompletableFuture.allOf(executions.values().toArray(new CompletableFuture<?>[executions.size()]))
                .thenRun(() -> complete(converter.apply(this))).exceptionally(throwable -> {
                    completeExceptionally(throwable);
                    return null;
                });
    }

    @Override
    public boolean complete(V value) {
        boolean result = super.complete(value);
        latch.countDown();
        return result;
    }

    @Override
    public boolean completeExceptionally(Throwable ex) {

        boolean value = super.completeExceptionally(ex);
        latch.countDown();
        return value;
    }

    @Override
    public String getError() {
        return null;
    }

    @Override
    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        return latch.await(timeout, unit);
    }

}
