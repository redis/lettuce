/*
 * Copyright 2026-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.support.http;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for {@link HttpClientResources}.
 *
 * @author Ivo Gaydazhiev
 */
@Tag(INTEGRATION_TEST)
class HttpClientResourcesIntegrationTests {

    @Test
    void shouldGetHttpClient() {
        HttpClient client = HttpClientResources.get();

        assertThat(client).isNotNull();
        assertThat(client).isInstanceOf(NettyHttpClient.class);
    }

    @Test
    void shouldReturnSameInstanceOnMultipleGets() {
        HttpClient client1 = HttpClientResources.get();
        HttpClient client2 = HttpClientResources.get();

        assertThat(client1).isSameAs(client2);
    }

    @Test
    void shouldBeThreadSafe() throws Exception {
        int threadCount = 10;
        int getsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicReference<HttpClient> firstClient = new AtomicReference<>();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < getsPerThread; j++) {
                        HttpClient client = HttpClientResources.get();
                        assertThat(client).isNotNull();
                        // All gets should return the same instance
                        firstClient.compareAndSet(null, client);
                        assertThat(client).isSameAs(firstClient.get());
                    }
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertThat(doneLatch.await(30, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();

        assertThat(successCount.get()).isEqualTo(threadCount);
    }

}
