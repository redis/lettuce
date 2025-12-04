package io.lettuce.core.protocol;

import java.lang.reflect.Field;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

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
    public void removes_threadLocal_entry_when_writer_depth_reaches_zero() throws Exception {

        SharedLock sharedLock = new SharedLock();

        CountingThreadLocal countingThreadLocal = new CountingThreadLocal();
        Field field = SharedLock.class.getDeclaredField("threadWriters");
        field.setAccessible(true);
        field.set(sharedLock, countingThreadLocal);

        Assertions.assertEquals(0, countingThreadLocal.initialValueCalls);

        sharedLock.incrementWriters();
        Assertions.assertEquals(1, countingThreadLocal.initialValueCalls,
                "initialValue() should be called once on the first ThreadLocal#get()");

        sharedLock.decrementWriters();

        sharedLock.incrementWriters();
        Assertions.assertEquals(2, countingThreadLocal.initialValueCalls,
                "initialValue() should be called again after remove()");
    }

    // Test Helper Class
    static class CountingThreadLocal extends ThreadLocal<Integer> {

        int initialValueCalls = 0;

        @Override
        protected Integer initialValue() {
            initialValueCalls++;
            return 0;
        }

    }

}
