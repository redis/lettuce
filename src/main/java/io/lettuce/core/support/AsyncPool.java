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

import java.io.Closeable;
import java.util.concurrent.CompletableFuture;

import io.lettuce.core.internal.AsyncCloseable;

/**
 * Interface declaring non-blocking object pool methods allowing to {@link #acquire()} and {@link #release(Object)} objects. All
 * activity of a pool task outcome is communicated through the returned {@link CompletableFuture}.
 *
 * @author Mark Paluch
 * @since 5.1
 */
public interface AsyncPool<T> extends Closeable, AsyncCloseable {

    /**
     * Acquire an object from this {@link AsyncPool}. The returned {@link CompletableFuture} is notified once the acquire is
     * successful and failed otherwise. Behavior upon acquiring objects from an exhausted pool depends on the actual pool
     * implementation whether requests are rejected immediately (exceptional completion with
     * {@link java.util.NoSuchElementException}) or delayed after exceeding a particular timeout (
     * {@link java.util.concurrent.TimeoutException}).
     *
     * <strong>It's required that an acquired object is always released to the pool again once the object is no longer in
     * use.</strong>.
     */
    CompletableFuture<T> acquire();

    /**
     * Release an object back to this {@link AsyncPool}. The returned {@link CompletableFuture} is notified once the release is
     * successful and failed otherwise. When failed the object will automatically disposed.
     *
     * @param object the object to be released. The object must have been acquired from this pool.
     */
    CompletableFuture<Void> release(T object);

    /**
     * Clear the pool.
     */
    void clear();

    /**
     * Clear the pool.
     */
    CompletableFuture<Void> clearAsync();

    @Override
    void close();

    @Override
    CompletableFuture<Void> closeAsync();

}
