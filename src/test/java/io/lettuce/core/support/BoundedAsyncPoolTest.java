/*
 * Copyright 2017-2018 the original author or authors.
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
package io.lettuce.core.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

/**
 * @author Mark Paluch
 */
public class BoundedAsyncPoolTest {

    AtomicInteger counter = new AtomicInteger();
    List<String> destroyed = new ArrayList<>();

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
    public void shouldCreateObject() {

        BoundedAsyncPool<String> pool = new BoundedAsyncPool<>(STRING_OBJECT_FACTORY, BoundedPoolConfig.create());

        String object = pool.acquire().join();

        assertThat(pool.getIdle()).isEqualTo(0);
        assertThat(object).isEqualTo("1");
    }

    @Test
    public void shouldCreateMinIdleObject() {

        BoundedAsyncPool<String> pool = new BoundedAsyncPool<>(STRING_OBJECT_FACTORY, BoundedPoolConfig.builder().minIdle(2)
                .build());

        assertThat(pool.getIdle()).isEqualTo(2);
        assertThat(pool.getObjectCount()).isEqualTo(2);
    }

    @Test
    public void shouldCreateMaintainMinIdleObject() {

        BoundedAsyncPool<String> pool = new BoundedAsyncPool<>(STRING_OBJECT_FACTORY, BoundedPoolConfig.builder().minIdle(2)
                .build());

        pool.acquire().join();

        assertThat(pool.getIdle()).isEqualTo(2);
        assertThat(pool.getObjectCount()).isEqualTo(3);
    }

    @Test
    public void shouldCreateMaintainMinMaxIdleObject() {

        BoundedAsyncPool<String> pool = new BoundedAsyncPool<>(STRING_OBJECT_FACTORY, BoundedPoolConfig.builder().minIdle(2)
                .maxTotal(2).build());

        pool.acquire().join();

        assertThat(pool.getIdle()).isEqualTo(1);
        assertThat(pool.getObjectCount()).isEqualTo(2);
    }

    @Test
    public void shouldReturnObject() {

        BoundedAsyncPool<String> pool = new BoundedAsyncPool<>(STRING_OBJECT_FACTORY, BoundedPoolConfig.create());

        String object = pool.acquire().join();
        assertThat(pool.getObjectCount()).isEqualTo(1);
        pool.release(object);

        assertThat(pool.getIdle()).isEqualTo(1);
    }

    @Test
    public void shouldReuseObjects() {

        BoundedAsyncPool<String> pool = new BoundedAsyncPool<>(STRING_OBJECT_FACTORY, BoundedPoolConfig.create());

        pool.release(pool.acquire().join());

        assertThat(pool.acquire().join()).isEqualTo("1");
        assertThat(pool.getIdle()).isEqualTo(0);
    }

    @Test
    public void shouldDestroyIdle() {

        BoundedAsyncPool<String> pool = new BoundedAsyncPool<>(STRING_OBJECT_FACTORY, BoundedPoolConfig.builder().maxIdle(2)
                .maxTotal(5).build());

        List<String> objects = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            objects.add(pool.acquire().join());
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
    public void shouldExhaustPool() {

        BoundedAsyncPool<String> pool = new BoundedAsyncPool<>(STRING_OBJECT_FACTORY, BoundedPoolConfig.builder().maxTotal(4)
                .build());

        String object1 = pool.acquire().join();
        String object2 = pool.acquire().join();
        String object3 = pool.acquire().join();
        String object4 = pool.acquire().join();

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
    public void shouldClearPool() {

        BoundedAsyncPool<String> pool = new BoundedAsyncPool<>(STRING_OBJECT_FACTORY, BoundedPoolConfig.builder().maxTotal(4)
                .build());

        for (int i = 0; i < 20; i++) {

            String object1 = pool.acquire().join();
            String object2 = pool.acquire().join();
            String object3 = pool.acquire().join();
            String object4 = pool.acquire().join();

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
    public void shouldExhaustPoolConcurrent() {

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
    public void shouldConcurrentlyFail() {

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
