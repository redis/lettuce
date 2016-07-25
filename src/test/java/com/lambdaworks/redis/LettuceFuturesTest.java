package com.lambdaworks.redis;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Paluch
 */
public class LettuceFuturesTest {

    @Before
    public void setUp() throws Exception {
        Thread.interrupted();
    }

    @Test(expected = RedisCommandExecutionException.class)
    public void awaitAllShouldThrowRedisCommandExecutionException() throws Exception {

        CompletableFuture<String> f = new CompletableFuture<>();
        f.completeExceptionally(new RedisCommandExecutionException("error"));

        LettuceFutures.awaitAll(1, TimeUnit.SECONDS, f);
    }

    @Test(expected = RedisCommandInterruptedException.class)
    public void awaitAllShouldThrowRedisCommandInterruptedException() throws Exception {

        CompletableFuture<String> f = new CompletableFuture<>();
        Thread.currentThread().interrupt();

        LettuceFutures.awaitAll(1, TimeUnit.SECONDS, f);
    }

    @Test
    public void awaitAllShouldSetInterruptedBit() throws Exception {

        CompletableFuture<String> f = new CompletableFuture<>();
        Thread.currentThread().interrupt();

        try {
            LettuceFutures.awaitAll(1, TimeUnit.SECONDS, f);
        } catch (Exception e) {
        }

        assertThat(Thread.currentThread().isInterrupted()).isTrue();
    }
}
