package io.lettuce.core.multidb;

import io.lettuce.core.RedisException;
import io.lettuce.core.RedisURI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link RedisEndpoints}.
 *
 * @author Ivo Gaydazhiev
 */
class RedisEndpointsUnitTests {

    private RedisURI endpoint1;

    private RedisURI endpoint2;

    private RedisURI endpoint3;

    @BeforeEach
    void setUp() {
        endpoint1 = RedisURI.create("redis://localhost:6379");
        endpoint2 = RedisURI.create("redis://localhost:6380");
        endpoint3 = RedisURI.create("redis://localhost:6381");
    }

    @Test
    void shouldCreateEmptyEndpoints() {
        RedisEndpoints endpoints = new RedisEndpoints();

        assertThat(endpoints.isEmpty()).isTrue();
        assertThat(endpoints.size()).isEqualTo(0);
        assertThat(endpoints.hasActive()).isFalse();
    }

    @Test
    void shouldCreateWithSingleEndpoint() {
        RedisEndpoints endpoints = RedisEndpoints.create(endpoint1);

        assertThat(endpoints.isEmpty()).isFalse();
        assertThat(endpoints.size()).isEqualTo(1);
        assertThat(endpoints.hasActive()).isTrue();
        assertThat(endpoints.getActive()).isEqualTo(endpoint1);
        assertThat(endpoints.contains(endpoint1)).isTrue();
    }

    @Test
    void shouldCreateWithMultipleEndpoints() {
        RedisEndpoints endpoints = RedisEndpoints.create(Arrays.asList(endpoint1, endpoint2, endpoint3));

        assertThat(endpoints.size()).isEqualTo(3);
        assertThat(endpoints.hasActive()).isTrue();
        assertThat(endpoints.getActive()).isEqualTo(endpoint1);
        assertThat(endpoints.contains(endpoint1)).isTrue();
        assertThat(endpoints.contains(endpoint2)).isTrue();
        assertThat(endpoints.contains(endpoint3)).isTrue();
    }

    @Test
    void shouldRejectNullEndpointInCreate() {
        assertThatThrownBy(() -> RedisEndpoints.create((RedisURI) null)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Initial endpoint must not be null");
    }

    @Test
    void shouldRejectNullCollectionInCreate() {
        assertThatThrownBy(() -> RedisEndpoints.create((Collection<RedisURI>) null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Initial endpoints must not be null");
    }

    @Test
    void shouldRejectEmptyCollectionInCreate() {
        assertThatThrownBy(() -> RedisEndpoints.create(Collections.emptyList())).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Initial endpoints must not be empty");
    }

    @Test
    void shouldAddEndpoint() {
        RedisEndpoints endpoints = RedisEndpoints.create(endpoint1);

        boolean added = endpoints.add(endpoint2);

        assertThat(added).isTrue();
        assertThat(endpoints.size()).isEqualTo(2);
        assertThat(endpoints.contains(endpoint2)).isTrue();
    }

    @Test
    void shouldNotAddDuplicateEndpoint() {
        RedisEndpoints endpoints = RedisEndpoints.create(endpoint1);

        boolean added = endpoints.add(endpoint1);

        assertThat(added).isFalse();
        assertThat(endpoints.size()).isEqualTo(1);
    }

    @Test
    void shouldRejectNullEndpointInAdd() {
        RedisEndpoints endpoints = new RedisEndpoints();

        assertThatThrownBy(() -> endpoints.add(null)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Endpoint must not be null");
    }

    @Test
    void shouldRemoveEndpoint() {
        RedisEndpoints endpoints = RedisEndpoints.create(Arrays.asList(endpoint1, endpoint2));

        boolean removed = endpoints.remove(endpoint2);

        assertThat(removed).isTrue();
        assertThat(endpoints.size()).isEqualTo(1);
        assertThat(endpoints.contains(endpoint2)).isFalse();
    }

    @Test
    void shouldNotRemoveNonExistentEndpoint() {
        RedisEndpoints endpoints = RedisEndpoints.create(endpoint1);

        boolean removed = endpoints.remove(endpoint2);

        assertThat(removed).isFalse();
        assertThat(endpoints.size()).isEqualTo(1);
    }

    @Test
    void shouldClearActiveWhenRemovingActiveEndpoint() {
        RedisEndpoints endpoints = RedisEndpoints.create(Arrays.asList(endpoint1, endpoint2));
        endpoints.setActive(endpoint1);

        endpoints.remove(endpoint1);

        assertThat(endpoints.hasActive()).isFalse();
        assertThatThrownBy(endpoints::getActive).isInstanceOf(RedisException.class)
                .hasMessageContaining("No active endpoint is set");
    }

    @Test
    void shouldNotRemoveLastEndpoint() {
        RedisEndpoints endpoints = RedisEndpoints.create(endpoint1);

        assertThatThrownBy(() -> endpoints.remove(endpoint1)).isInstanceOf(RedisException.class)
                .hasMessageContaining("Cannot remove the last endpoint");
    }

    @Test
    void shouldRejectNullEndpointInRemove() {
        RedisEndpoints endpoints = RedisEndpoints.create(endpoint1);

        assertThatThrownBy(() -> endpoints.remove(null)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Endpoint must not be null");
    }

    @Test
    void shouldSetActiveEndpoint() {
        RedisEndpoints endpoints = RedisEndpoints.create(Arrays.asList(endpoint1, endpoint2));

        endpoints.setActive(endpoint2);

        assertThat(endpoints.getActive()).isEqualTo(endpoint2);
        assertThat(endpoints.isActive(endpoint2)).isTrue();
        assertThat(endpoints.isActive(endpoint1)).isFalse();
    }

    @Test
    void shouldRejectSettingNonExistentEndpointAsActive() {
        RedisEndpoints endpoints = RedisEndpoints.create(endpoint1);

        assertThatThrownBy(() -> endpoints.setActive(endpoint2)).isInstanceOf(RedisException.class)
                .hasMessageContaining("Endpoint").hasMessageContaining("is not available");
    }

    @Test
    void shouldRejectNullEndpointInSetActive() {
        RedisEndpoints endpoints = RedisEndpoints.create(endpoint1);

        assertThatThrownBy(() -> endpoints.setActive(null)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Endpoint must not be null");
    }

    @Test
    void shouldGetActiveEndpoint() {
        RedisEndpoints endpoints = RedisEndpoints.create(endpoint1);

        RedisURI active = endpoints.getActive();

        assertThat(active).isEqualTo(endpoint1);
    }

    @Test
    void shouldThrowWhenGettingActiveWithoutActiveSet() {
        RedisEndpoints endpoints = new RedisEndpoints();
        endpoints.add(endpoint1);

        assertThatThrownBy(endpoints::getActive).isInstanceOf(RedisException.class)
                .hasMessageContaining("No active endpoint is set");
    }

    @Test
    void shouldCheckIfEndpointIsActive() {
        RedisEndpoints endpoints = RedisEndpoints.create(Arrays.asList(endpoint1, endpoint2));
        endpoints.setActive(endpoint1);

        assertThat(endpoints.isActive(endpoint1)).isTrue();
        assertThat(endpoints.isActive(endpoint2)).isFalse();
    }

    @Test
    void shouldRejectNullEndpointInIsActive() {
        RedisEndpoints endpoints = RedisEndpoints.create(endpoint1);

        assertThatThrownBy(() -> endpoints.isActive(null)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Endpoint must not be null");
    }

    @Test
    void shouldCheckIfEndpointExists() {
        RedisEndpoints endpoints = RedisEndpoints.create(endpoint1);

        assertThat(endpoints.contains(endpoint1)).isTrue();
        assertThat(endpoints.contains(endpoint2)).isFalse();
    }

    @Test
    void shouldRejectNullEndpointInContains() {
        RedisEndpoints endpoints = RedisEndpoints.create(endpoint1);

        assertThatThrownBy(() -> endpoints.contains(null)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Endpoint must not be null");
    }

    @Test
    void shouldGetAllEndpoints() {
        RedisEndpoints endpoints = RedisEndpoints.create(Arrays.asList(endpoint1, endpoint2, endpoint3));

        Set<RedisURI> allEndpoints = endpoints.getEndpoints();

        assertThat(allEndpoints).containsExactlyInAnyOrder(endpoint1, endpoint2, endpoint3);
    }

    @Test
    void shouldReturnUnmodifiableSetFromGetEndpoints() {
        RedisEndpoints endpoints = RedisEndpoints.create(endpoint1);

        Set<RedisURI> allEndpoints = endpoints.getEndpoints();

        assertThatThrownBy(() -> allEndpoints.add(endpoint2)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldGetSize() {
        RedisEndpoints endpoints = RedisEndpoints.create(Arrays.asList(endpoint1, endpoint2));

        assertThat(endpoints.size()).isEqualTo(2);
    }

    @Test
    void shouldCheckIfEmpty() {
        RedisEndpoints endpoints = new RedisEndpoints();

        assertThat(endpoints.isEmpty()).isTrue();

        endpoints.add(endpoint1);

        assertThat(endpoints.isEmpty()).isFalse();
    }

    @Test
    void shouldCheckIfHasActive() {
        RedisEndpoints endpoints = new RedisEndpoints();

        assertThat(endpoints.hasActive()).isFalse();

        endpoints.add(endpoint1);
        endpoints.setActive(endpoint1);

        assertThat(endpoints.hasActive()).isTrue();
    }

    @Test
    void shouldGenerateToString() {
        RedisEndpoints endpoints = RedisEndpoints.create(endpoint1);

        String toString = endpoints.toString();

        assertThat(toString).contains("RedisEndpoints").contains("endpoints=").contains("activeEndpoint=");
    }

    @Test
    void shouldBeThreadSafeForConcurrentReads() throws InterruptedException {
        RedisEndpoints endpoints = RedisEndpoints.create(Arrays.asList(endpoint1, endpoint2, endpoint3));
        endpoints.setActive(endpoint1);

        int threadCount = 10;
        int iterationsPerThread = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < iterationsPerThread; j++) {
                        endpoints.getActive();
                        endpoints.contains(endpoint1);
                        endpoints.isActive(endpoint1);
                        endpoints.getEndpoints();
                        endpoints.size();
                    }
                    successCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(successCount.get()).isEqualTo(threadCount);
        executor.shutdown();
    }

    @Test
    void shouldBeThreadSafeForConcurrentWrites() throws InterruptedException {
        RedisEndpoints endpoints = RedisEndpoints.create(endpoint1);

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        RedisURI[] additionalEndpoints = new RedisURI[threadCount];
        for (int i = 0; i < threadCount; i++) {
            additionalEndpoints[i] = RedisURI.create("redis://localhost:" + (6380 + i));
        }

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    endpoints.add(additionalEndpoints[index]);
                } finally {
                    latch.countDown();
                }
            });
        }

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(endpoints.size()).isEqualTo(threadCount + 1); // initial + added
        executor.shutdown();
    }

    @Test
    void shouldBeThreadSafeForConcurrentReadWriteOperations() throws InterruptedException {
        RedisEndpoints endpoints = RedisEndpoints.create(Arrays.asList(endpoint1, endpoint2, endpoint3));
        endpoints.setActive(endpoint1);

        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger errorCount = new AtomicInteger(0);

        // Half threads do reads, half do writes
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    if (index % 2 == 0) {
                        // Read operations
                        for (int j = 0; j < 100; j++) {
                            endpoints.getActive();
                            endpoints.getEndpoints();
                            endpoints.size();
                        }
                    } else {
                        // Write operations
                        for (int j = 0; j < 100; j++) {
                            if (j % 3 == 0) {
                                endpoints.setActive(endpoint2);
                            } else if (j % 3 == 1) {
                                endpoints.setActive(endpoint3);
                            } else {
                                endpoints.setActive(endpoint1);
                            }
                        }
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(errorCount.get()).isEqualTo(0);
        assertThat(endpoints.hasActive()).isTrue();
        executor.shutdown();
    }

}
