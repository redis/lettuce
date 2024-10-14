package io.lettuce.core.protocol;

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

}
