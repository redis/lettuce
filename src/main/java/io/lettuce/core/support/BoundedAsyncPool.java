/*
 * Copyright 2017-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.support;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.internal.Futures;
import io.lettuce.core.internal.LettuceAssert;

/**
 * Bounded asynchronous object pool. This object pool allows pre-warming with {@link BoundedPoolConfig#getMinIdle() idle}
 * objects upon construction. The pool is stateful and requires {@link #closeAsync() cleanup} once it's no longer in use.
 * <p>
 * Object pool bounds are maintained on a best-effort basis as bounds are maintained upon object request whereas the actual
 * object creation might finish at a later time. You might see temporarily slight differences in object usage vs. pool count due
 * to asynchronous processing vs. protecting the pool from exceed its bounds.
 *
 * @author Mark Paluch
 * @since 5.1
 * @see BoundedPoolConfig
 * @see AsyncObjectFactory
 */
public class BoundedAsyncPool<T> extends BasePool implements AsyncPool<T> {

    private static final CompletableFuture<Void> COMPLETED = CompletableFuture.completedFuture(null);

    private static final IllegalStateException POOL_SHUTDOWN = unknownStackTrace(
            new IllegalStateException("AsyncPool is closed"), BoundedAsyncPool.class, "acquire()");

    private static final NoSuchElementException POOL_EXHAUSTED = unknownStackTrace(new NoSuchElementException("Pool exhausted"),
            BoundedAsyncPool.class, "acquire()");

    private static final IllegalStateException NOT_PART_OF_POOL = unknownStackTrace(
            new IllegalStateException("Returned object not currently part of this pool"), BoundedAsyncPool.class, "release()");

    private final int maxTotal;

    private final int maxIdle;

    private final int minIdle;

    private final AsyncObjectFactory<T> factory;

    private final Queue<T> cache;

    private final Queue<T> all;

    private final AtomicInteger objectCount = new AtomicInteger();

    private final AtomicInteger objectsInCreationCount = new AtomicInteger();

    private final AtomicInteger idleCount = new AtomicInteger();

    private final CompletableFuture<Void> closeFuture = new CompletableFuture<>();

    private volatile State state = State.ACTIVE;

    /**
     * Create a new {@link BoundedAsyncPool} given {@link BasePoolConfig} and {@link AsyncObjectFactory}. The factory creates
     * idle objects upon construction and requires {@link #closeAsync() termination} once it's no longer in use.
     * <p>
     * Please note that pre-initialization cannot be awaited when using this constructor. Please use
     * {@link #create(AsyncObjectFactory, BoundedPoolConfig)} instead.
     *
     * @param factory must not be {@code null}.
     * @param poolConfig must not be {@code null}.
     */
    public BoundedAsyncPool(AsyncObjectFactory<T> factory, BoundedPoolConfig poolConfig) {
        this(factory, poolConfig, true);
    }

    /**
     * Create a new {@link BoundedAsyncPool} given {@link BasePoolConfig} and {@link AsyncObjectFactory}.
     *
     * @param factory must not be {@code null}.
     * @param poolConfig must not be {@code null}.
     * @param createIdle whether to pre-initialize the pool.
     * @since 5.3.2
     */
    BoundedAsyncPool(AsyncObjectFactory<T> factory, BoundedPoolConfig poolConfig, boolean createIdle) {

        super(poolConfig);

        LettuceAssert.notNull(factory, "AsyncObjectFactory must not be null");

        this.maxTotal = poolConfig.getMaxTotal();
        this.maxIdle = poolConfig.getMaxIdle();
        this.minIdle = poolConfig.getMinIdle();

        this.factory = factory;

        this.cache = new ConcurrentLinkedQueue<>();
        this.all = new ConcurrentLinkedQueue<>();

        if (createIdle) {
            createIdle();
        }
    }

    /**
     * Create and initialize {@link BoundedAsyncPool} asynchronously.
     *
     * @param factory must not be {@code null}.
     * @param poolConfig must not be {@code null}.
     * @param <T> object type that is managed by the pool.
     * @return a {@link CompletionStage} that completes with the {@link BoundedAsyncPool} when created and pre-initialized
     *         successfully. Completes exceptionally if the pool initialization failed.
     * @since 5.3.3
     */
    public static <T> CompletionStage<BoundedAsyncPool<T>> create(AsyncObjectFactory<T> factory, BoundedPoolConfig poolConfig) {

        BoundedAsyncPool<T> pool = new BoundedAsyncPool<>(factory, poolConfig, false);

        CompletableFuture<BoundedAsyncPool<T>> future = new CompletableFuture<>();

        pool.createIdle().whenComplete((v, throwable) -> {

            if (throwable == null) {
                future.complete(pool);
            } else {
                pool.closeAsync().whenComplete((v1, throwable1) -> {
                    future.completeExceptionally(new RedisConnectionException("Could not create pool", throwable));
                });
            }
        });

        return future;
    }

    @SuppressWarnings("rawtypes")
    CompletableFuture<Void> createIdle() {

        int potentialIdle = getMinIdle() - getIdle();
        if (potentialIdle <= 0 || !isPoolActive()) {
            return CompletableFuture.completedFuture(null);
        }

        int totalLimit = getAvailableCapacity();
        int toCreate = Math.min(Math.max(0, totalLimit), potentialIdle);

        CompletableFuture[] futures = new CompletableFuture[toCreate];
        for (int i = 0; i < toCreate; i++) {

            if (getAvailableCapacity() <= 0) {
                break;
            }

            CompletableFuture<T> future = new CompletableFuture<>();
            futures[i] = future;
            makeObject0(future);

            future.thenAccept(it -> {

                if (isPoolActive()) {
                    idleCount.incrementAndGet();
                    cache.add(it);
                } else {
                    factory.destroy(it);
                }
            });
        }

        return CompletableFuture.allOf(futures);
    }

    private int getAvailableCapacity() {
        return getMaxTotal() - (getCreationInProgress() + getObjectCount());
    }

    @Override
    public CompletableFuture<T> acquire() {

        T object = cache.poll();

        CompletableFuture<T> res = new CompletableFuture<>();
        acquire0(object, res);

        return res;
    }

    private void acquire0(T object, CompletableFuture<T> res) {

        if (object != null) {

            idleCount.decrementAndGet();

            if (isTestOnAcquire()) {

                factory.validate(object).whenComplete((state, throwable) -> {

                    if (!isPoolActive()) {
                        res.completeExceptionally(POOL_SHUTDOWN);
                        return;
                    }

                    if (state != null && state) {

                        completeAcquire(res, object);

                        return;
                    }

                    destroy0(object).whenComplete((aVoid, th) -> makeObject0(res));
                });

                return;
            }

            if (isPoolActive()) {
                completeAcquire(res, object);
            } else {
                res.completeExceptionally(POOL_SHUTDOWN);
            }

            createIdle();
            return;
        }

        long objects = (long) (getObjectCount() + getCreationInProgress());

        if ((long) getMaxTotal() >= (objects + 1)) {
            makeObject0(res);
            return;
        }

        res.completeExceptionally(POOL_EXHAUSTED);
    }

    private void makeObject0(CompletableFuture<T> res) {

        long total = getObjectCount();
        long creations = objectsInCreationCount.incrementAndGet();

        if (((long) getMaxTotal()) < total + creations) {

            res.completeExceptionally(POOL_EXHAUSTED);
            objectsInCreationCount.decrementAndGet();
            return;
        }

        factory.create().whenComplete((o, t) -> {

            if (t != null) {
                objectsInCreationCount.decrementAndGet();
                res.completeExceptionally(new IllegalStateException("Cannot allocate object", t));
                return;
            }

            if (isTestOnCreate()) {

                factory.validate(o).whenComplete((state, throwable) -> {

                    try {

                        if (isPoolActive() && state != null && state) {

                            objectCount.incrementAndGet();
                            all.add(o);

                            completeAcquire(res, o);
                            return;
                        }

                        if (!isPoolActive()) {
                            rejectPoolClosed(res, o);
                            return;
                        }

                        factory.destroy(o).whenComplete((v, th) -> res.completeExceptionally(
                                new IllegalStateException("Cannot allocate object: Validation failed", throwable)));
                    } catch (Exception e) {
                        factory.destroy(o).whenComplete((v, th) -> res.completeExceptionally(
                                new IllegalStateException("Cannot allocate object: Validation failed", throwable)));
                    } finally {
                        objectsInCreationCount.decrementAndGet();
                    }
                });

                return;
            }

            try {

                if (isPoolActive()) {
                    objectCount.incrementAndGet();
                    all.add(o);

                    completeAcquire(res, o);
                } else {
                    rejectPoolClosed(res, o);
                }

            } catch (Exception e) {

                objectCount.decrementAndGet();
                all.remove(o);

                factory.destroy(o).whenComplete((v, th) -> res.completeExceptionally(e));
            } finally {
                objectsInCreationCount.decrementAndGet();
            }
        });
    }

    private void completeAcquire(CompletableFuture<T> res, T o) {

        if (res.isCancelled()) {
            return0(o);
        } else {
            res.complete(o);
        }
    }

    private void rejectPoolClosed(CompletableFuture<T> res, T o) {

        factory.destroy(o);
        res.completeExceptionally(POOL_SHUTDOWN);
    }

    @Override
    public CompletableFuture<Void> release(T object) {

        if (!all.contains(object)) {
            return Futures.failed(NOT_PART_OF_POOL);
        }

        if (idleCount.get() >= getMaxIdle()) {
            return destroy0(object);
        }

        if (isTestOnRelease()) {

            CompletableFuture<Boolean> valid = factory.validate(object);
            CompletableFuture<Void> res = new CompletableFuture<>();

            valid.whenComplete((state1, throwable) -> {

                if (state1 != null && state1) {
                    return0(object).whenComplete((x, y) -> res.complete(null));
                } else {
                    destroy0(object).whenComplete((x, y) -> res.complete(null));
                }
            });

            return res;
        }

        return return0(object);
    }

    private CompletableFuture<Void> return0(T object) {

        int idleCount = this.idleCount.incrementAndGet();

        if (idleCount > getMaxIdle()) {

            this.idleCount.decrementAndGet();
            return destroy0(object);
        }

        cache.add(object);

        return COMPLETED;
    }

    private CompletableFuture<Void> destroy0(T object) {

        objectCount.decrementAndGet();
        all.remove(object);
        return factory.destroy(object);
    }

    @Override
    public void clear() {
        clearAsync().join();
    }

    @Override
    public CompletableFuture<Void> clearAsync() {

        List<CompletableFuture<Void>> futures = new ArrayList<>(all.size());

        T cached;
        while ((cached = cache.poll()) != null) {
            idleCount.decrementAndGet();
            objectCount.decrementAndGet();
            all.remove(cached);
            futures.add(factory.destroy(cached));
        }

        return Futures.allOf(futures);
    }

    @Override
    public void close() {
        closeAsync().join();
    }

    @Override
    public CompletableFuture<Void> closeAsync() {

        if (!isPoolActive()) {
            return closeFuture;
        }

        state = State.TERMINATING;

        CompletableFuture<Void> clear = clearAsync();

        state = State.TERMINATED;

        clear.whenComplete((aVoid, throwable) -> {

            if (throwable != null) {
                closeFuture.completeExceptionally(throwable);
            } else {
                closeFuture.complete(aVoid);
            }
        });

        return closeFuture;
    }

    /**
     * Returns the maximum number of objects that can be allocated by the pool (checked out to clients, or idle awaiting
     * checkout) at a given time. When negative, there is no limit to the number of objects that can be managed by the pool at
     * one time.
     *
     * @return the cap on the total number of object instances managed by the pool.
     * @see BoundedPoolConfig#getMaxTotal()
     */
    public int getMaxTotal() {
        return maxTotal;
    }

    /**
     * Returns the cap on the number of "idle" instances in the pool. If {@code maxIdle} is set too low on heavily loaded
     * systems it is possible you will see objects being destroyed and almost immediately new objects being created. This is a
     * result of the active threads momentarily returning objects faster than they are requesting them them, causing the number
     * of idle objects to rise above maxIdle. The best value for maxIdle for heavily loaded system will vary but the default is
     * a good starting point.
     *
     * @return the maximum number of "idle" instances that can be held in the pool.
     * @see BoundedPoolConfig#getMaxIdle()
     */
    public int getMaxIdle() {
        return maxIdle;
    }

    /**
     * Returns the target for the minimum number of idle objects to maintain in the pool. If this is the case, an attempt is
     * made to ensure that the pool has the required minimum number of instances during idle object eviction runs.
     * <p>
     * If the configured value of minIdle is greater than the configured value for {@code maxIdle} then the value of
     * {@code maxIdle} will be used instead.
     *
     * @return The minimum number of objects.
     * @see BoundedPoolConfig#getMinIdle()
     */
    public int getMinIdle() {

        int maxIdleSave = getMaxIdle();
        if (this.minIdle > maxIdleSave) {
            return maxIdleSave;
        } else {
            return minIdle;
        }
    }

    public int getIdle() {
        return idleCount.get();
    }

    public int getObjectCount() {
        return objectCount.get();
    }

    public int getCreationInProgress() {
        return objectsInCreationCount.get();
    }

    private boolean isPoolActive() {
        return this.state == State.ACTIVE;
    }

    enum State {
        ACTIVE, TERMINATING, TERMINATED;
    }

}
