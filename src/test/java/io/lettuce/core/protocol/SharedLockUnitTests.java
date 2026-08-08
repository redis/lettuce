package io.lettuce.core.protocol;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.WeakHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import io.lettuce.core.RedisException;

import static io.lettuce.TestTags.UNIT_TEST;
import java.util.concurrent.atomic.AtomicInteger;

@Tag(UNIT_TEST)
public class SharedLockUnitTests {

    @Test
    public void safety_on_reentrant_lock_exclusive_on_writers() throws InterruptedException {
        final SharedLock sharedLock = new SharedLock();
        CountDownLatch cnt = new CountDownLatch(1);
        try {
            sharedLock.incrementWriters();

            String result = sharedLock.doExclusive(() -> {
                return sharedLock.doExclusive(() -> {
                    return "ok";
                });
            });
            if ("ok".equals(result)) {
                cnt.countDown();
            }
        } finally {
            sharedLock.decrementWriters();
        }

        boolean await = cnt.await(1, TimeUnit.SECONDS);
        Assertions.assertTrue(await);

        // verify writers won't be negative after finally decrementWriters
        String result = sharedLock.doExclusive(() -> {
            return sharedLock.doExclusive(() -> {
                return "ok";
            });
        });

        Assertions.assertEquals("ok", result);

        // and other writers should be passed after exclusive lock released
        CountDownLatch cntOtherThread = new CountDownLatch(1);
        new Thread(() -> {
            try {
                sharedLock.incrementWriters();
                cntOtherThread.countDown();
            } finally {
                sharedLock.decrementWriters();
            }
        }).start();

        await = cntOtherThread.await(1, TimeUnit.SECONDS);
        Assertions.assertTrue(await);
    }

    @Test
    public void writerCountsAreIndependentPerSharedLockInstance() {
        final SharedLock lock1 = new SharedLock();
        final SharedLock lock2 = new SharedLock();

        // Increment writers on lock1
        lock1.incrementWriters();

        // lock2 should still be able to get exclusive lock (its writer count is 0)
        String result = lock2.doExclusive(() -> "exclusive-on-lock2");
        Assertions.assertEquals("exclusive-on-lock2", result);

        // Cleanup
        lock1.decrementWriters();

        // Now lock1 should also work
        result = lock1.doExclusive(() -> "exclusive-on-lock1");
        Assertions.assertEquals("exclusive-on-lock1", result);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void entryShouldBeRemovedWhenWriterCountReachesZero() throws Exception {
        final SharedLock sharedLock = new SharedLock();

        // Access the static THREAD_WRITERS field
        Field threadWritersField = SharedLock.class.getDeclaredField("THREAD_WRITERS");
        threadWritersField.setAccessible(true);
        ThreadLocal<WeakHashMap<SharedLock, Integer>> threadLocal = (ThreadLocal<WeakHashMap<SharedLock, Integer>>) threadWritersField
                .get(null);

        WeakHashMap<SharedLock, Integer> map = threadLocal.get();

        // Initially, the map should not contain this SharedLock
        Assertions.assertFalse(map.containsKey(sharedLock), "Map should not contain SharedLock initially");

        // Increment should add an entry
        sharedLock.incrementWriters();
        Assertions.assertTrue(map.containsKey(sharedLock), "Map should contain SharedLock after increment");
        Assertions.assertEquals(1, map.get(sharedLock), "Writer count should be 1");

        // Decrement to zero should remove the entry
        sharedLock.decrementWriters();
        Assertions.assertFalse(map.containsKey(sharedLock), "Map entry should be removed when count reaches zero");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void nestedWriterCountsShouldWorkCorrectly() throws Exception {
        final SharedLock sharedLock = new SharedLock();

        // Access the static THREAD_WRITERS field
        Field threadWritersField = SharedLock.class.getDeclaredField("THREAD_WRITERS");
        threadWritersField.setAccessible(true);
        ThreadLocal<WeakHashMap<SharedLock, Integer>> threadLocal = (ThreadLocal<WeakHashMap<SharedLock, Integer>>) threadWritersField
                .get(null);

        WeakHashMap<SharedLock, Integer> map = threadLocal.get();

        // Nested increments
        sharedLock.incrementWriters();
        Assertions.assertEquals(1, map.get(sharedLock));

        sharedLock.incrementWriters();
        Assertions.assertEquals(2, map.get(sharedLock));

        sharedLock.incrementWriters();
        Assertions.assertEquals(3, map.get(sharedLock));

        // Decrements - entry should NOT be removed until count reaches 0
        sharedLock.decrementWriters();
        Assertions.assertEquals(2, map.get(sharedLock));

        sharedLock.decrementWriters();
        Assertions.assertEquals(1, map.get(sharedLock));

        // Final decrement to zero - entry should be removed
        sharedLock.decrementWriters();
        Assertions.assertFalse(map.containsKey(sharedLock), "Entry should be removed when count reaches zero");
    }

    @Test
    public void multipleThreadsShouldNotInterfere() throws InterruptedException {
        final SharedLock sharedLock = new SharedLock();
        final int threadCount = 100;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch completionLatch = new CountDownLatch(threadCount);
        final AtomicInteger errorCount = new AtomicInteger(0);

        // Create multiple threads that will use the SharedLock
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();

                    // Each thread increments and decrements multiple times
                    for (int j = 0; j < 10; j++) {
                        sharedLock.incrementWriters();
                        sharedLock.decrementWriters();
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    completionLatch.countDown();
                }
            }).start();
        }

        // Start all threads at once
        startLatch.countDown();

        // Wait for all threads to complete
        boolean completed = completionLatch.await(10, TimeUnit.SECONDS);
        Assertions.assertTrue(completed, "All threads should complete within timeout");
        Assertions.assertEquals(0, errorCount.get(), "No errors should occur during concurrent operations");

        // After all threads complete, the SharedLock should be in a clean state
        String result = sharedLock.doExclusive(() -> "success");
        Assertions.assertEquals("success", result);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void singleThreadLocalEntryPerThread() throws Exception {
        // Access the static THREAD_WRITERS field
        Field threadWritersField = SharedLock.class.getDeclaredField("THREAD_WRITERS");
        threadWritersField.setAccessible(true);
        ThreadLocal<WeakHashMap<SharedLock, Integer>> threadLocal = (ThreadLocal<WeakHashMap<SharedLock, Integer>>) threadWritersField
                .get(null);

        // Get the map for this thread
        WeakHashMap<SharedLock, Integer> map1 = threadLocal.get();
        WeakHashMap<SharedLock, Integer> map2 = threadLocal.get();

        // Should be the SAME map instance
        Assertions.assertSame(map1, map2, "ThreadLocal should return the same map instance");

        // Create multiple SharedLocks
        SharedLock lock1 = new SharedLock();
        SharedLock lock2 = new SharedLock();
        SharedLock lock3 = new SharedLock();

        lock1.incrementWriters();
        lock2.incrementWriters();
        lock3.incrementWriters();

        // All should be in the SAME map
        WeakHashMap<SharedLock, Integer> map = threadLocal.get();
        Assertions.assertTrue(map.containsKey(lock1));
        Assertions.assertTrue(map.containsKey(lock2));
        Assertions.assertTrue(map.containsKey(lock3));

        // Cleanup
        lock1.decrementWriters();
        lock2.decrementWriters();
        lock3.decrementWriters();
    }

    /**
     * #3804 — a {@code ReentrantLock} leaked by a thread that died inside the guarded region must not park the next exclusive
     * caller forever. {@code doExclusive} should fail fast with a {@link RedisException} once the bounded acquire elapses.
     */
    @Test
    @Timeout(5)
    public void doExclusiveFailsFastWhenGuardLockIsLeaked() throws Exception {
        final SharedLock sharedLock = new SharedLock(Duration.ofMillis(200));

        Lock internalLock = extractInternalLock(sharedLock);
        CountDownLatch held = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        // Hold the internal lock without ever releasing it during the test window — simulates the leaked lock.
        Thread holder = new Thread(() -> {
            internalLock.lock();
            held.countDown();
            try {
                release.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                internalLock.unlock();
            }
        });
        holder.setDaemon(true);
        holder.start();

        Assertions.assertTrue(held.await(1, TimeUnit.SECONDS), "holder thread failed to acquire the internal lock");

        try {
            RedisException ex = Assertions.assertThrows(RedisException.class,
                    () -> sharedLock.doExclusive(() -> "unreachable"));
            Assertions.assertTrue(ex.getMessage().contains("Timed out"), "unexpected message: " + ex.getMessage());
        } finally {
            release.countDown();
            holder.join(TimeUnit.SECONDS.toMillis(1));
        }
    }

    /**
     * #3880 — a shared writer leaked by another thread (incremented, never decremented) keeps the writer count above the
     * exclusive caller's own count, so the CAS in {@code lockWritersExclusive} can never succeed. Instead of spinning RUNNABLE
     * at 100% CPU forever, {@code doExclusive} should bound the spin and fail fast with a {@link RedisException}.
     */
    @Test
    @Timeout(5)
    public void doExclusiveFailsFastWhenSharedWriterIsLeaked() throws Exception {
        final SharedLock sharedLock = new SharedLock(Duration.ofMillis(200));

        // Leak a shared writer from another thread: increment on a thread that finishes without decrementing.
        Thread leaker = new Thread(sharedLock::incrementWriters);
        leaker.start();
        leaker.join(TimeUnit.SECONDS.toMillis(1));

        RedisException ex = Assertions.assertThrows(RedisException.class, () -> sharedLock.doExclusive(() -> "unreachable"));
        Assertions.assertTrue(ex.getMessage().contains("shared writer"), "unexpected message: " + ex.getMessage());
    }

    /**
     * When exclusive mode was abandoned (for example, writers is left at -1 because a previous exclusive holder died),
     * {@code lockWritersExclusive} must fail fast and report an abandoned exclusive lock rather than claiming a negative shared
     * writer count or attributing the failure to a leaked shared writer (#3880).
     */
    @Test
    @Timeout(5)
    public void doExclusiveFailsFastWhenExclusiveModeIsAbandoned() throws Exception {
        final SharedLock sharedLock = new SharedLock(Duration.ofMillis(200));

        Field writersField = SharedLock.class.getDeclaredField("writers");
        writersField.setAccessible(true);
        writersField.set(sharedLock, -1L);

        RedisException ex = Assertions.assertThrows(RedisException.class, () -> sharedLock.doExclusive(() -> "unreachable"));
        Assertions.assertTrue(ex.getMessage().contains("abandoned"), "unexpected message: " + ex.getMessage());
        Assertions.assertFalse(ex.getMessage().contains("shared writer"),
                "message should not claim a shared writer was leaked: " + ex.getMessage());
    }

    /**
     * When the calling thread's interrupt flag is already set, an uncontended {@code doExclusive} must still succeed without
     * throwing {@link RedisException}, and must preserve the interrupted status on the thread.
     */
    @Test
    public void doExclusiveSucceedsWhenThreadIsInterruptedAndLockIsUncontended() {
        final SharedLock sharedLock = new SharedLock();

        Thread.currentThread().interrupt();
        try {
            String result = sharedLock.doExclusive(() -> "ok");
            Assertions.assertEquals("ok", result);
            Assertions.assertTrue(Thread.currentThread().isInterrupted(), "interrupt flag should be preserved");
        } finally {
            Thread.interrupted(); // clear interrupted status
        }
    }

    private static Lock extractInternalLock(SharedLock sharedLock) throws Exception {
        Field field = SharedLock.class.getDeclaredField("lock");
        field.setAccessible(true);
        return (Lock) field.get(sharedLock);
    }

}
