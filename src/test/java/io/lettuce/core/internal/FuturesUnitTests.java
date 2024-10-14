package io.lettuce.core.internal;

import static io.lettuce.TestTags.UNIT_TEST;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.RedisCommandInterruptedException;

/**
 * Unit tests for {@link Futures}.
 *
 * @author Mark Paluch
 * @author Tihomir Mateev
 */
@Tag(UNIT_TEST)
class FuturesUnitTests {

    @BeforeEach
    void setUp() {
        Thread.interrupted();
    }

    @Test
    void awaitAllShouldThrowRedisCommandExecutionException() {

        CompletableFuture<String> f = new CompletableFuture<>();
        f.completeExceptionally(new RedisCommandExecutionException("error"));

        assertThatThrownBy(() -> Futures.await(1, SECONDS, f)).isInstanceOf(RedisCommandExecutionException.class);
    }

    @Test
    void awaitAllShouldThrowRedisCommandInterruptedException() {

        CompletableFuture<String> f = new CompletableFuture<>();
        Thread.currentThread().interrupt();

        assertThatThrownBy(() -> Futures.await(1, SECONDS, f)).isInstanceOf(RedisCommandInterruptedException.class);
    }

    @Test
    void awaitAllShouldSetInterruptedBit() {

        CompletableFuture<String> f = new CompletableFuture<>();
        Thread.currentThread().interrupt();

        try {
            Futures.await(1, SECONDS, f);
        } catch (Exception e) {
        }

        assertThat(Thread.currentThread().isInterrupted()).isTrue();
    }

    @Test
    void allOfShouldNotThrow() throws InterruptedException {
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        List<Throwable> issues = new ArrayList<>();
        List<CompletableFuture<Void>> futures = Collections.synchronizedList(new ArrayList<>());
        // Submit multiple threads to perform concurrent operations
        CountDownLatch latch = new CountDownLatch(threadCount);
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    for (int y = 0; y < 1000; y++) {
                        futures.add(new CompletableFuture<>());
                    }

                    Futures.allOf(futures);
                } catch (Exception e) {
                    issues.add(e);
                } finally {
                    latch.countDown();
                }
            });
        }

        // wait for all threads to complete
        latch.await();
        assertThat(issues).doesNotHaveAnyElementsOfTypes(ArrayIndexOutOfBoundsException.class);
    }

}
