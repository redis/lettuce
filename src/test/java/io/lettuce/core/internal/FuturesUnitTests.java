/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
