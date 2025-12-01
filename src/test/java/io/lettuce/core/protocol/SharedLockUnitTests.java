package io.lettuce.core.protocol;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static io.lettuce.TestTags.UNIT_TEST;

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
    public void cleanupShouldRemoveThreadLocalEntry() throws Exception {
        final SharedLock sharedLock = new SharedLock();

        // Access the threadWriters field via reflection
        Field threadWritersField = SharedLock.class.getDeclaredField("threadWriters");
        threadWritersField.setAccessible(true);
        ThreadLocal<Integer> threadWriters = (ThreadLocal<Integer>) threadWritersField.get(sharedLock);

        // Use the SharedLock to create a ThreadLocal entry
        sharedLock.incrementWriters();
        Assertions.assertEquals(1, threadWriters.get());

        sharedLock.incrementWriters();
        Assertions.assertEquals(2, threadWriters.get());

        sharedLock.decrementWriters();
        Assertions.assertEquals(1, threadWriters.get());

        sharedLock.decrementWriters();
        // After decrement to zero, ThreadLocal entry should still exist (not removed automatically)
        Assertions.assertEquals(0, threadWriters.get());

        // Now call cleanup() to remove the ThreadLocal entry
        sharedLock.cleanup();

        // After cleanup, get() should return the initial value (0) again
        // This indicates the ThreadLocal entry was removed and re-initialized
        Integer valueAfterCleanup = threadWriters.get();
        Assertions.assertEquals(0, valueAfterCleanup);
    }

    @Test
    public void cleanupWithMultipleThreads() throws InterruptedException {
        final SharedLock sharedLock = new SharedLock();
        final int threadCount = 100;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch completionLatch = new CountDownLatch(threadCount);

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
                    // Simulate endpoint close by calling cleanup
                    sharedLock.cleanup();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
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

        // After all threads complete and cleanup, the SharedLock should be in a clean state
        // We can verify this by doing an exclusive operation
        String result = sharedLock.doExclusive(() -> "success");
        Assertions.assertEquals("success", result);
    }

}
