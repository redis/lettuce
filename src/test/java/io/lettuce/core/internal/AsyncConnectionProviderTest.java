package io.lettuce.core.internal;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class AsyncConnectionProviderTest {

    @Test
    public void testFutureListLength() throws InterruptedException, ExecutionException, TimeoutException {

        CountDownLatch slowCreate = new CountDownLatch(1);
        CountDownLatch slowShutdown = new CountDownLatch(1);

        // create a provider with a slow connection creation
        AsyncConnectionProvider<String, AsyncCloseable, CompletableFuture<AsyncCloseable>> provider = new AsyncConnectionProvider<>(
                key -> {
                    return countDownFuture(slowCreate, new io.lettuce.core.api.AsyncCloseable() {

                        @Override
                        public CompletableFuture<Void> closeAsync() {
                            return CompletableFuture.completedFuture(null);
                        }

                    });
                });

        // add slow shutdown connection first
        SlowCloseFuture slowCloseFuture = new SlowCloseFuture(slowShutdown);
        provider.register("slowShutdown", new io.lettuce.core.api.AsyncCloseable() {

            @Override
            public CompletableFuture<Void> closeAsync() {
                return slowCloseFuture;
            }

        });

        // add slow creation connection
        CompletableFuture<AsyncCloseable> createFuture = provider.getConnection("slowCreate");

        // close the connection.
        CompletableFuture<Void> closeFuture = provider.close();

        // the connection has not been created yet, so the close futures array always has 1 element
        // we block the iterator on the slowCloseFuture
        // then we count down the creation, the close future will be added to the list
        slowCreate.countDown();

        // the close future is added to the list, we unblock the iterator
        slowShutdown.countDown();

        // assert close future is completed, and no exceptions are thrown
        closeFuture.get(10, TimeUnit.SECONDS);
        Assert.assertTrue(createFuture.isDone());
    }

    private <T> CompletableFuture<T> countDownFuture(CountDownLatch countDownLatch, T value) {
        return CompletableFuture.runAsync(() -> {
            try {
                countDownLatch.await(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).thenApply(v -> value);
    }

    static class SlowCloseFuture extends CompletableFuture<Void> {

        private final CountDownLatch countDownLatch;

        SlowCloseFuture(CountDownLatch countDownLatch) {
            this.countDownLatch = countDownLatch;
        }

        @Override
        public CompletableFuture<Void> toCompletableFuture() {
            // we block the iterator on here
            try {
                countDownLatch.await(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return super.toCompletableFuture();
        }

        @Override
        public Void get() {
            return null;
        }

    }

}
