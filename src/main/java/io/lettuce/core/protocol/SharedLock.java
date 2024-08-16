package io.lettuce.core.protocol;

import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
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

    private static final AtomicLongFieldUpdater<SharedLock> WRITERS = AtomicLongFieldUpdater.newUpdater(SharedLock.class,
            "writers");

    private final Lock lock = new ReentrantLock();

    private final ThreadLocal<Integer> sharedCnt = ThreadLocal.withInitial(() -> 0);

    private volatile long writers = 0;

    private volatile Thread exclusiveLockOwner;

    /**
     * Wait for stateLock and increment writers. Will wait if stateLock is locked and if writer counter is negative.
     */
    void incrementWriters() {

        if (exclusiveLockOwner == Thread.currentThread()) {
            return;
        }

        lock.lock();
        try {
            for (;;) {

                if (WRITERS.get(this) >= 0) {
                    WRITERS.incrementAndGet(this);
                    sharedCnt.set(sharedCnt.get() + 1);
                    return;
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Decrement writers without any wait.
     */
    void decrementWriters() {

        if (exclusiveLockOwner == Thread.currentThread()) {
            return;
        }

        WRITERS.decrementAndGet(this);
        sharedCnt.set(sharedCnt.get() - 1);
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
     * @param <T> the return type
     * @return the return value
     */
    <T> T doExclusive(Supplier<T> supplier) {

        LettuceAssert.notNull(supplier, "Supplier must not be null");

        lock.lock();
        try {

            try {

                lockWritersExclusive();
                return supplier.get();
            } finally {
                unlockWritersExclusive();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Wait for stateLock and no writers. Must be used in an outer {@code synchronized} block to prevent interleaving with other
     * methods using writers. Sets writers to a negative value to create a lock for {@link #incrementWriters()}.
     */
    private void lockWritersExclusive() {

        if (exclusiveLockOwner == Thread.currentThread()) {
            WRITERS.decrementAndGet(this);
            return;
        }

        lock.lock();
        try {
            for (;;) {

                if (WRITERS.compareAndSet(this, 0, -1)) {
                    exclusiveLockOwner = Thread.currentThread();
                    return;
                }
                // reentrant exclusive lock
                if (WRITERS.compareAndSet(this, sharedCnt.get(), -1)) {
                    exclusiveLockOwner = Thread.currentThread();
                    return;
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Unlock writers.
     */
    private void unlockWritersExclusive() {

        if (exclusiveLockOwner == Thread.currentThread()) {
            // check exclusive look not reentrant first
            if (WRITERS.compareAndSet(this, -1, sharedCnt.get())) {
                exclusiveLockOwner = null;
                return;
            }
            // otherwise unlock until no more reentrant left
            WRITERS.incrementAndGet(this);
        }
    }

}
