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

import java.util.concurrent.CompletableFuture;

import org.openjdk.jmh.annotations.*;

import io.lettuce.core.EmptyRedisChannelWriter;
import io.lettuce.core.EmptyStatefulRedisConnection;
import io.lettuce.core.api.StatefulRedisConnection;

/**
 * @author Mark Paluch
 */
@State(Scope.Benchmark)
public class AsyncConnectionPoolBenchmark {

    private AsyncPool<StatefulRedisConnection<String, String>> pool;
    private StatefulRedisConnection[] holder = new StatefulRedisConnection[20];

    @Setup
    public void setup() {

        BoundedPoolConfig config = BoundedPoolConfig.builder().minIdle(0).maxIdle(20).maxTotal(20).build();

        pool = AsyncConnectionPoolSupport.createBoundedObjectPool(
                () -> CompletableFuture.completedFuture(new EmptyStatefulRedisConnection(EmptyRedisChannelWriter.INSTANCE)),
                config);
    }

    @TearDown(Level.Iteration)
    public void tearDown() {
        pool.clear();
    }

    @Benchmark
    public void singleConnection() {
        pool.release(pool.acquire().join()).join();
    }

    @Benchmark
    public void twentyConnections() {

        for (int i = 0; i < holder.length; i++) {
            holder[i] = pool.acquire().join();
        }

        for (int i = 0; i < holder.length; i++) {
            pool.release(holder[i]).join();
        }
    }
}
