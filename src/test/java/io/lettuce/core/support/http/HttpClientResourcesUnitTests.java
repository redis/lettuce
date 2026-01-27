/*
 * Copyright 2024-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.support.http;

import io.lettuce.test.ReflectionTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link HttpClientResources}.
 *
 * @author Ivo Gaydazhiev
 */
class HttpClientResourcesUnitTests {

    @BeforeEach
    void setUp() throws Exception {
        // Reset static state before each test
        resetHttpClientResources();
    }

    @AfterEach
    void tearDown() throws Exception {
        // Clean up after each test
        resetHttpClientResources();
    }

    @Test
    void shouldAcquireHttpClient() {
        HttpClient client = HttpClientResources.acquire();

        assertThat(client).isNotNull();
        assertThat(client).isInstanceOf(NettyHttpClient.class);

        HttpClientResources.release(client);
    }

    @Test
    void shouldReturnSameInstanceOnMultipleAcquires() {
        HttpClient client1 = HttpClientResources.acquire();
        HttpClient client2 = HttpClientResources.acquire();

        assertThat(client1).isSameAs(client2);

        HttpClientResources.release(client1);
        HttpClientResources.release(client2);
    }

    @Test
    void shouldIncrementRefCountOnAcquire() throws Exception {
        HttpClient client1 = HttpClientResources.acquire();
        long refCount1 = getRefCount();
        assertThat(refCount1).isEqualTo(1);

        HttpClient client2 = HttpClientResources.acquire();
        long refCount2 = getRefCount();
        assertThat(refCount2).isEqualTo(2);

        HttpClientResources.release(client1);
        HttpClientResources.release(client2);
    }

    @Test
    void shouldDecrementRefCountOnRelease() throws Exception {
        HttpClient client1 = HttpClientResources.acquire();
        HttpClient client2 = HttpClientResources.acquire();

        long refCount1 = getRefCount();
        assertThat(refCount1).isEqualTo(2);

        HttpClientResources.release(client1);
        long refCount2 = getRefCount();
        assertThat(refCount2).isEqualTo(1);

        HttpClientResources.release(client2);
        long refCount3 = getRefCount();
        assertThat(refCount3).isEqualTo(0);
    }

    @Test
    void shouldCloseClientWhenLastReferenceIsReleased() throws Exception {
        HttpClient client1 = HttpClientResources.acquire();
        HttpClient client2 = HttpClientResources.acquire();

        HttpClientResources.release(client1);
        HttpClient sharedClient = getSharedClient();
        assertThat(sharedClient).isNotNull();

        HttpClientResources.release(client2);
        sharedClient = getSharedClient();
        assertThat(sharedClient).isNull();
    }

    @Test
    void shouldCreateNewClientAfterAllReferencesReleased() throws Exception {
        HttpClient client1 = HttpClientResources.acquire();
        HttpClientResources.release(client1);

        HttpClient client2 = HttpClientResources.acquire();
        assertThat(client2).isNotNull();
        assertThat(client2).isNotSameAs(client1);

        HttpClientResources.release(client2);
    }

    @Test
    void shouldHandleNullRelease() {
        // Should not throw exception
        assertThatCode(() -> HttpClientResources.release(null)).doesNotThrowAnyException();
    }

    @Test
    void shouldHandleReleaseOfNonSharedClient() {
        HttpClient sharedClient = HttpClientResources.acquire();
        HttpClient otherClient = new NettyHttpClient();

        // Should not affect ref count
        HttpClientResources.release(otherClient);

        try {
            long refCount = getRefCount();
            assertThat(refCount).isEqualTo(1);
        } catch (Exception e) {
            fail("Failed to get ref count", e);
        } finally {
            HttpClientResources.release(sharedClient);
            otherClient.close();
        }
    }

    @Test
    void shouldBeThreadSafe() throws Exception {
        int threadCount = 10;
        int acquiresPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < acquiresPerThread; j++) {
                        HttpClient client = HttpClientResources.acquire();
                        assertThat(client).isNotNull();
                        HttpClientResources.release(client);
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
        assertThat(getRefCount()).isEqualTo(0);
        assertThat(getSharedClient()).isNull();
    }

    @Test
    void shouldHandleReleaseWhenRefCountIsZero() throws Exception {
        HttpClient client = HttpClientResources.acquire();
        HttpClientResources.release(client);

        // Try to release again when ref count is already 0
        HttpClientResources.release(client);

        // Should not throw exception and ref count should remain 0
        assertThat(getRefCount()).isEqualTo(0);
    }

    // Helper methods to access private static fields via reflection

    private void resetHttpClientResources() throws Exception {
        Field sharedClientField = HttpClientResources.class.getDeclaredField("sharedClient");
        sharedClientField.setAccessible(true);
        HttpClient client = (HttpClient) sharedClientField.get(null);
        if (client != null) {
            try {
                client.close();
            } catch (Exception e) {
                // Ignore
            }
        }
        sharedClientField.set(null, null);

        Field refCountField = HttpClientResources.class.getDeclaredField("refCount");
        refCountField.setAccessible(true);
        refCountField.set(null, 0L);

        Field cachedProviderField = HttpClientResources.class.getDeclaredField("cachedProvider");
        cachedProviderField.setAccessible(true);
        cachedProviderField.set(null, null);
    }

    private long getRefCount() {
        return ReflectionTestUtils.getField(HttpClientResources.class, "refCount");
    }

    private HttpClient getSharedClient() {
        return ReflectionTestUtils.getField(HttpClientResources.class, "sharedClient");
    }

}
