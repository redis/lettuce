package com.lambdaworks.redis.pubsub;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.lambdaworks.redis.RedisFuture;
import com.lambdaworks.redis.protocol.RedisCommand;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
@SuppressWarnings({ "raw", "unchecked" })
class VoidFuture implements RedisFuture<Void> {
    private RedisCommand<?, ?, ?> redisFuture;

    public VoidFuture(RedisCommand<?, ?, ?> redisFuture) {
        this.redisFuture = redisFuture;
    }

    @Override
    public void addListener(Runnable listener, Executor executor) {
        redisFuture.addListener(listener, executor);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return redisFuture.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return redisFuture.isCancelled();
    }

    @Override
    public boolean isDone() {
        return redisFuture.isDone();
    }

    @Override
    public Void get() throws InterruptedException, ExecutionException {
        redisFuture.get();
        return null;
    }

    @Override
    public Void get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        redisFuture.get(timeout, unit);
        return null;
    }

    @Override
    public String getError() {
        return redisFuture.getError();
    }

    @Override
    public boolean await(long timeout, TimeUnit unit) {
        return redisFuture.await(timeout, unit);
    }
}
