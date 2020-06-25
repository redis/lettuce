/*
 * Copyright 2011-2020 the original author or authors.
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
package io.lettuce.core.protocol;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import io.lettuce.core.internal.LettuceAssert;

/**
 * Shared locking facade that supports shared and exclusive locking.
 * <p>
 * Multiple shared locks (writers) are allowed concurrently to process their work. If an exclusive lock is requested, the
 * exclusive lock requestor will wait until all shared locks are released and the exclusive worker is permitted.
 * <p>
 * Exclusive locking is reentrant. An exclusive lock owner is permitted to acquire and release shared locks. Shared/exclusive
 * lock requests by other threads than the thread which holds the exclusive lock, are forced to wait until the exclusive lock is
 * released.
 *
 * @author Mark Paluch
 */
class SharedLock {

    private final AtomicLong writers = new AtomicLong();

    private volatile Thread exclusiveLockOwner;

    /**
     * Wait for stateLock and increment writers. Will wait if stateLock is locked and if writer counter is negative.
     */
    void incrementWriters() {

        if (exclusiveLockOwner == Thread.currentThread()) {
            return;
        }

        synchronized (this) {
            for (;;) {

                if (writers.get() >= 0) {
                    writers.incrementAndGet();
                    return;
                }
            }
        }
    }

    /**
     * Decrement writers without any wait.
     */
    void decrementWriters() {

        if (exclusiveLockOwner == Thread.currentThread()) {
            return;
        }

        writers.decrementAndGet();
    }

    /**
     * Execute a {@link Runnable} guarded by an exclusive lock.
     *
     * @param runnable the runnable, must not be {@code null}.
     */
    void doExclusive(Runnable runnable) {

        LettuceAssert.notNull(runnable, "Runnable must not be null");

        doExclusive(() -> {
            runnable.run();
            return null;
        });
    }

    /**
     * Retrieve a value produced by a {@link Supplier} guarded by an exclusive lock.
     *
     * @param supplier the {@link Supplier}, must not be {@code null}.
     * @param <T> the return type.
     * @return the return value.
     */
    <T> T doExclusive(Supplier<T> supplier) {

        LettuceAssert.notNull(supplier, "Supplier must not be null");

        synchronized (this) {

            try {

                lockWritersExclusive();
                return supplier.get();
            } finally {
                unlockWritersExclusive();
            }
        }
    }

    /**
     * Wait for stateLock and no writers. Must be used in an outer {@code synchronized} block to prevent interleaving with other
     * methods using writers. Sets writers to a negative value to create a lock for {@link #incrementWriters()}.
     */
    private void lockWritersExclusive() {

        if (exclusiveLockOwner == Thread.currentThread()) {
            writers.decrementAndGet();
            return;
        }

        synchronized (this) {
            for (;;) {

                if (writers.compareAndSet(0, -1)) {
                    exclusiveLockOwner = Thread.currentThread();
                    return;
                }
            }
        }
    }

    /**
     * Unlock writers.
     */
    private void unlockWritersExclusive() {

        if (exclusiveLockOwner == Thread.currentThread()) {
            if (writers.incrementAndGet() == 0) {
                exclusiveLockOwner = null;
            }
        }
    }

}
