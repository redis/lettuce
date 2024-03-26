package io.lettuce.core.internal;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.RedisCommandInterruptedException;

/**
 * Unit tests for {@link Futures}.
 *
 * @author Mark Paluch
 */
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
}
