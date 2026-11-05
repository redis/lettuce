package io.lettuce.core.protocol;

import java.time.Duration;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import io.lettuce.core.RedisException;
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
 * <p>
 * <b>Memory Management:</b> This implementation uses a static {@link ThreadLocal} containing a {@link WeakHashMap} to track
 * per-thread writer counts across all {@code SharedLock} instances. This design:
 * <ul>
 * <li>Creates only ONE ThreadLocal entry per thread (not per SharedLock instance)</li>
 * <li>Uses WeakHashMap so entries are automatically removed when SharedLock instances are garbage collected</li>
 * <li>Explicitly removes entries when writer count reaches zero for immediate cleanup</li>
 * <li>Eliminates the memory leak that occurred with per-instance ThreadLocal in connection pooling scenarios</li>
 * </ul>
 *
 * @author Mark Paluch
 */
class SharedLock {

    private static final AtomicLongFieldUpdater<SharedLock> WRITERS = AtomicLongFieldUpdater.newUpdater(SharedLock.class,
            "writers");

    private final Lock lock = new ReentrantLock();

    private static final ThreadLocal<WeakHashMap<SharedLock, Integer>> THREAD_WRITERS = ThreadLocal
            .withInitial(WeakHashMap::new);

    private volatile long writers = 0;

    private volatile Thread exclusiveLockOwner;

    private static final Duration DEFAULT_EXCLUSIVE_LOCK_TIMEOUT = Duration.ofSeconds(30);

    private final long exclusiveLockTimeoutNanos;

    /**
     * Create a {@link SharedLock} with the {@link #DEFAULT_EXCLUSIVE_LOCK_TIMEOUT default} exclusive-lock timeout.
     */
    SharedLock() {
        this(DEFAULT_EXCLUSIVE_LOCK_TIMEOUT);
    }

    /**
     * Create a {@link SharedLock} with a configurable exclusive-lock acquisition timeout.
     *
     * @param exclusiveLockTimeout the maximum time an exclusive acquisition waits before failing fast with a
     *        {@link RedisException}; must not be {@code null}.
     */
    SharedLock(Duration exclusiveLockTimeout) {
        LettuceAssert.notNull(exclusiveLockTimeout, "Exclusive lock timeout must not be null");
        this.exclusiveLockTimeoutNanos = exclusiveLockTimeout.toNanos();
    }

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
                    WeakHashMap<SharedLock, Integer> map = THREAD_WRITERS.get();
                    map.merge(this, 1, Integer::sum);
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
        WeakHashMap<SharedLock, Integer> map = THREAD_WRITERS.get();
        map.computeIfPresent(this, (lock, count) -> count <= 1 ? null : count - 1);
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

        long deadline = System.nanoTime() + exclusiveLockTimeoutNanos;
        acquireExclusiveGuard(deadline);
        try {

            try {

                lockWritersExclusive(deadline);
                return supplier.get();
            } finally {
                unlockWritersExclusive();
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Acquire the guarding {@link #lock} for an exclusive operation, bounded by {@code deadline}. If the lock was leaked by a
     * thread that died inside the guarded region (issue #3804) a plain {@code lock.lock()} would park the caller - frequently a
     * Netty event-loop thread - forever. Fail fast with a {@link RedisException} instead so the endpoint can be rebuilt. The
     * acquisition remains reentrant, so a thread that already holds the lock re-acquires immediately.
     * <p>
     * Acquisition continues in a loop bounded by {@code deadline} while catching {@link InterruptedException} and restoring the
     * interrupt flag afterwards, so callers with their interrupt flag already set (such as during
     * {@code close()}/{@code closeAsync()}) do not fail an otherwise successful exclusive operation solely due to the interrupt
     * under brief contention.
     *
     * @param deadline the {@link System#nanoTime()} timestamp by which the acquisition must complete.
     */
    private void acquireExclusiveGuard(long deadline) {

        boolean interrupted = false;
        boolean acquired = lock.tryLock();
        try {
            while (!acquired) {
                long timeoutNanos = deadline - System.nanoTime();
                if (timeoutNanos <= 0) {
                    break;
                }
                try {
                    acquired = lock.tryLock(timeoutNanos, TimeUnit.NANOSECONDS);
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }

        if (!acquired) {
            throw new RedisException("Timed out after " + TimeUnit.NANOSECONDS.toMillis(exclusiveLockTimeoutNanos)
                    + "ms acquiring the exclusive SharedLock; the lock holder likely died inside the guarded region. "
                    + "The endpoint must be rebuilt to recover.");
        }
    }

    /**
     * Wait for stateLock and no writers, bounded by {@code deadline}. Must be used in an outer {@code synchronized} block to
     * prevent interleaving with other methods using writers. Sets writers to a negative value to create a lock for
     * {@link #incrementWriters()}.
     *
     * @param deadline the {@link System#nanoTime()} timestamp by which writer draining must complete.
     */
    private void lockWritersExclusive(long deadline) {

        if (exclusiveLockOwner == Thread.currentThread()) {
            WRITERS.decrementAndGet(this);
            return;
        }

        lock.lock();
        try {
            for (;;) {

                // allow reentrant exclusive lock by comparing writers count and threadWriters
                // count
                int threadWriterCount = getThreadWriterCount();
                if (WRITERS.compareAndSet(this, threadWriterCount, -1)) {
                    exclusiveLockOwner = Thread.currentThread();
                    return;
                }

                // A leaked shared writer (a thread that died before decrementWriters()) keeps
                // the writer count above this thread's own count, so the CAS above can never
                // succeed.
                // Or if exclusive mode was abandoned (writers is negative, e.g. a previous
                // exclusive holder died),
                // the CAS also cannot succeed. Bound the spin and fail fast instead of burning
                // a CPU core forever
                // (issues #3804, #3880).
                if (System.nanoTime() - deadline >= 0) {
                    long currentWriters = WRITERS.get(this);
                    if (currentWriters < 0) {
                        throw new RedisException("Timed out after " + TimeUnit.NANOSECONDS.toMillis(exclusiveLockTimeoutNanos)
                                + "ms acquiring the exclusive SharedLock; the exclusive lock was likely abandoned by a previous holder. "
                                + "The endpoint must be rebuilt to recover.");
                    }

                    throw new RedisException("Timed out after " + TimeUnit.NANOSECONDS.toMillis(exclusiveLockTimeoutNanos)
                            + "ms waiting for " + currentWriters + " shared writer(s) to drain while acquiring the "
                            + "exclusive SharedLock; a shared writer was likely leaked. The endpoint must be rebuilt to recover.");
                }

                Thread.yield();
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
            int threadWriterCount = getThreadWriterCount();

            // check exclusive look not reentrant first
            if (WRITERS.compareAndSet(this, -1, threadWriterCount)) {
                exclusiveLockOwner = null;
                return;
            }
            // otherwise unlock until no more reentrant left
            WRITERS.incrementAndGet(this);
        }
    }

    private int getThreadWriterCount() {
        return THREAD_WRITERS.get().getOrDefault(this, 0);
    }

}
