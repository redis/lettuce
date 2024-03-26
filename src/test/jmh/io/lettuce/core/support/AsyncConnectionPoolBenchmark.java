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
