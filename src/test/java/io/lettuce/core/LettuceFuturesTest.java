/*
 * Copyright 2011-2016 the original author or authors.
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
package io.lettuce.core;

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
