/*
 * Copyright 2017-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import io.lettuce.core.RedisException;
import io.lettuce.test.TestFutures;

/**
 * Unit tests for {@link BoundedAsyncPool}.
 *
 * @author Mark Paluch
 */
class BoundedAsyncPoolUnitTests {

    private AtomicInteger counter = new AtomicInteger();
    private List<String> destroyed = new ArrayList<>();

    private AsyncObjectFactory<String> STRING_OBJECT_FACTORY = new AsyncObjectFactory<String>() {
        @Override
        public CompletableFuture<String> create() {
            return CompletableFuture.completedFuture(counter.incrementAndGet() + "");
        }

        @Override
        public CompletableFuture<Void> destroy(String object) {
            destroyed.add(object);
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Boolean> validate(String object) {
            return CompletableFuture.completedFuture(true);
        }
    };

    @Test
    void shouldCreateObject() {

        BoundedAsyncPool<String> pool = new BoundedAsyncPool<>(STRING_OBJECT_FACTORY, BoundedPoolConfig.create());

        String object = TestFutures.getOrTimeout(pool.acquire());

        assertThat(pool.getIdle()).isEqualTo(0);
        assertThat(object).isEqualTo("1");
    }

    @Test
    void shouldCreatePoolAsync() {

        CompletionStage<BoundedAsyncPool<String>> pool = BoundedAsyncPool.create(STRING_OBJECT_FACTORY,
                BoundedPoolConfig.builder().minIdle(5).build());

        pool.toCompletableFuture().join();

        assertThat(counter).hasValue(5);
    }

    @Test
    void failedAsyncCreationShouldCleanUpResources() {

        AtomicInteger cleanups = new AtomicInteger();
        AtomicInteger creations = new AtomicInteger();
        CompletionStage<BoundedAsyncPool<String>> pool = BoundedAsyncPool.create(new AsyncObjectFactory<String>() {

            @Override
            public CompletableFuture<String> create() {

                if (creations.incrementAndGet() == 1) {
                    return io.lettuce.core.internal.Futures.failed(new IllegalStateException());
                }

                return CompletableFuture.completedFuture("ok");
            }

            @Override
            public CompletableFuture<Void> destroy(String object) {
                cleanups.incrementAndGet();
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public CompletableFuture<Boolean> validate(String object) {
                return null;
            }

        }, BoundedPoolConfig.builder().minIdle(5).build());

        assertThatExceptionOfType(CompletionException.class).isThrownBy(pool.toCompletableFuture()::join)
                .withCauseInstanceOf(RedisException.class).withRootCauseInstanceOf(IllegalStateException.class);

        assertThat(cleanups).hasValue(4);
    }

    @Test
    void shouldCreateMinIdleObject() {

        BoundedAsyncPool<String> pool = new BoundedAsyncPool<>(STRING_OBJECT_FACTORY, BoundedPoolConfig.builder().minIdle(2)
                .build());

        assertThat(pool.getIdle()).isEqualTo(2);
        assertThat(pool.getObjectCount()).isEqualTo(2);
    }

    @Test
    void shouldCreateMaintainMinIdleObject() {

        BoundedAsyncPool<String> pool = new BoundedAsyncPool<>(STRING_OBJECT_FACTORY, BoundedPoolConfig.builder().minIdle(2)
                .build());

        TestFutures.awaitOrTimeout(pool.acquire());

        assertThat(pool.getIdle()).isEqualTo(2);
        assertThat(pool.getObjectCount()).isEqualTo(3);
    }

    @Test
    void shouldCreateMaintainMinMaxIdleObject() {

        BoundedAsyncPool<String> pool = new BoundedAsyncPool<>(STRING_OBJECT_FACTORY, BoundedPoolConfig.builder().minIdle(2)
                .maxTotal(2).build());

        TestFutures.awaitOrTimeout(pool.acquire());

        assertThat(pool.getIdle()).isEqualTo(1);
        assertThat(pool.getObjectCount()).isEqualTo(2);
    }

    @Test
    void shouldReturnObject() {

        BoundedAsyncPool<String> pool = new BoundedAsyncPool<>(STRING_OBJECT_FACTORY, BoundedPoolConfig.create());

        String object = TestFutures.getOrTimeout(pool.acquire());
        assertThat(pool.getObjectCount()).isEqualTo(1);
        pool.release(object);

        assertThat(pool.getIdle()).isEqualTo(1);
    }

    @Test
    void shouldReuseObjects() {

        BoundedAsyncPool<String> pool = new BoundedAsyncPool<>(STRING_OBJECT_FACTORY, BoundedPoolConfig.create());

        pool.release(TestFutures.getOrTimeout(pool.acquire()));

        assertThat(TestFutures.getOrTimeout(pool.acquire())).isEqualTo("1");
        assertThat(pool.getIdle()).isEqualTo(0);
    }

    @Test
    void shouldDestroyIdle() {

        BoundedAsyncPool<String> pool = new BoundedAsyncPool<>(STRING_OBJECT_FACTORY, BoundedPoolConfig.builder().maxIdle(2)
                .maxTotal(5).build());

        List<String> objects = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            objects.add(TestFutures.getOrTimeout(pool.acquire()));
        }

        for (int i = 0; i < 2; i++) {
            pool.release(objects.get(i));
        }

        assertThat(pool.getIdle()).isEqualTo(2);

        pool.release(objects.get(2));

        assertThat(pool.getIdle()).isEqualTo(2);
        assertThat(pool.getObjectCount()).isEqualTo(2);
        assertThat(destroyed).containsOnly("3");
    }

    @Test
    void shouldExhaustPool() {

        BoundedAsyncPool<String> pool = new BoundedAsyncPool<>(STRING_OBJECT_FACTORY, BoundedPoolConfig.builder().maxTotal(4)
                .build());

        String object1 = TestFutures.getOrTimeout(pool.acquire());
        String object2 = TestFutures.getOrTimeout(pool.acquire());
        String object3 = TestFutures.getOrTimeout(pool.acquire());
        String object4 = TestFutures.getOrTimeout(pool.acquire());

        assertThat(pool.getIdle()).isZero();
        assertThat(pool.getObjectCount()).isEqualTo(4);

        assertThat(pool.acquire()).isCompletedExceptionally();

        assertThat(pool.getIdle()).isZero();
        assertThat(pool.getObjectCount()).isEqualTo(4);

        pool.release(object1);
        pool.release(object2);
        pool.release(object3);
        pool.release(object4);

        assertThat(pool.getIdle()).isEqualTo(4);
        assertThat(pool.getObjectCount()).isEqualTo(4);
    }

    @Test
    void shouldClearPool() {

        BoundedAsyncPool<String> pool = new BoundedAsyncPool<>(STRING_OBJECT_FACTORY, BoundedPoolConfig.builder().maxTotal(4)
                .build());

        for (int i = 0; i < 20; i++) {

            String object1 = TestFutures.getOrTimeout(pool.acquire());
            String object2 = TestFutures.getOrTimeout(pool.acquire());
            String object3 = TestFutures.getOrTimeout(pool.acquire());
            String object4 = TestFutures.getOrTimeout(pool.acquire());

            assertThat(pool.acquire()).isCompletedExceptionally();

            pool.release(object1);
            pool.release(object2);
            pool.release(object3);
            pool.release(object4);

            pool.clear();

            assertThat(pool.getObjectCount()).isZero();
            assertThat(pool.getIdle()).isZero();
        }
    }

    @Test
    void shouldExhaustPoolConcurrent() {

        List<CompletableFuture<String>> progress = new ArrayList<>();
        AsyncObjectFactory<String> IN_PROGRESS = new AsyncObjectFactory<String>() {
            @Override
            public CompletableFuture<String> create() {

                CompletableFuture<String> future = new CompletableFuture<>();
                progress.add(future);

                return future;
            }

            @Override
            public CompletableFuture<Void> destroy(String object) {
                destroyed.add(object);
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public CompletableFuture<Boolean> validate(String object) {
                return CompletableFuture.completedFuture(true);
            }
        };

        BoundedAsyncPool<String> pool = new BoundedAsyncPool<>(IN_PROGRESS, BoundedPoolConfig.builder().maxTotal(4).build());

        CompletableFuture<String> object1 = pool.acquire();
        CompletableFuture<String> object2 = pool.acquire();
        CompletableFuture<String> object3 = pool.acquire();
        CompletableFuture<String> object4 = pool.acquire();
        CompletableFuture<String> object5 = pool.acquire();

        assertThat(pool.getIdle()).isZero();
        assertThat(pool.getObjectCount()).isZero();
        assertThat(pool.getCreationInProgress()).isEqualTo(4);

        assertThat(object5).isCompletedExceptionally();

        progress.forEach(it -> it.complete("foo"));

        assertThat(pool.getIdle()).isZero();
        assertThat(pool.getObjectCount()).isEqualTo(4);
        assertThat(pool.getCreationInProgress()).isZero();
    }

    @Test
    void shouldConcurrentlyFail() {

        List<CompletableFuture<String>> progress = new ArrayList<>();
        AsyncObjectFactory<String> IN_PROGRESS = new AsyncObjectFactory<String>() {
            @Override
            public CompletableFuture<String> create() {

                CompletableFuture<String> future = new CompletableFuture<>();
                progress.add(future);

                return future;
            }

            @Override
            public CompletableFuture<Void> destroy(String object) {
                destroyed.add(object);
                return CompletableFuture.completedFuture(null);
            }

            @Override
            public CompletableFuture<Boolean> validate(String object) {
                return CompletableFuture.completedFuture(true);
            }
        };

        BoundedAsyncPool<String> pool = new BoundedAsyncPool<>(IN_PROGRESS, BoundedPoolConfig.builder().maxTotal(4).build());

        CompletableFuture<String> object1 = pool.acquire();
        CompletableFuture<String> object2 = pool.acquire();
        CompletableFuture<String> object3 = pool.acquire();
        CompletableFuture<String> object4 = pool.acquire();

        progress.forEach(it -> it.completeExceptionally(new IllegalStateException()));

        assertThat(object1).isCompletedExceptionally();
        assertThat(object2).isCompletedExceptionally();
        assertThat(object3).isCompletedExceptionally();
        assertThat(object4).isCompletedExceptionally();

        assertThat(pool.getIdle()).isZero();
        assertThat(pool.getObjectCount()).isZero();
        assertThat(pool.getCreationInProgress()).isZero();
    }
}
