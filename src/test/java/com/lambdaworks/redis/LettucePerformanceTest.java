package com.lambdaworks.redis;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.Ignore;
import org.junit.Test;

import com.google.common.collect.Lists;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class LettucePerformanceTest {

    private RedisClient redisClient = new RedisClient(TestSettings.host(), TestSettings.port());
    private ExecutorService executor;
    private CountDownLatch latch = new CountDownLatch(1);

    /**
     * Multi-threaded performance test.
     *
     * Uses a {@link ThreadPoolExecutor} with thread and connection preheating. Execution tasks are submitted and synchronized
     * with a {@link CountDownLatch}
     *
     * @throws Exception
     */
    @Test
    @Ignore("Run me manually")
    public void testPerformance() throws Exception {

        // TWEAK ME
        int threads = 4;
        int totalCalls = 250000;
        boolean waitForFutureCompletion = true;
        boolean connectionPerThread = true;
        // Keep in mind, that the size of the event loop threads is CPU count * 4 unless you
        // set -Dio.netty.eventLoopThreads=...
        // END OF TWEAK ME

        executor = new ThreadPoolExecutor(threads, threads, 1, TimeUnit.MINUTES, new ArrayBlockingQueue<Runnable>(totalCalls));

        List<Future<List<RedisFuture<String>>>> futurama = Lists.newArrayList();

        preheat(threads);

        final int callsPerThread = totalCalls / threads;

        submitExecutionTasks(threads, futurama, callsPerThread, connectionPerThread);
        Thread.sleep(800);

        long start = System.currentTimeMillis();
        latch.countDown();

        for (Future<List<RedisFuture<String>>> listFuture : futurama) {
            for (RedisFuture<String> future : listFuture.get()) {
                if (waitForFutureCompletion) {
                    future.get();
                }
            }
        }

        long end = System.currentTimeMillis();

        long duration = end - start;
        double durationSeconds = duration / 1000d;
        double opsPerSecond = totalCalls / durationSeconds;
        System.out.println(String.format("Duration: %d ms (%.2f sec), operations: %d, %.2f ops/sec ", duration,
                durationSeconds, totalCalls, opsPerSecond));

        redisClient.shutdown();

    }

    protected void submitExecutionTasks(int threads, List<Future<List<RedisFuture<String>>>> futurama,
            final int callsPerThread, final boolean connectionPerThread) {
        final RedisAsyncConnection<String, String> sharedConnection;
        if (!connectionPerThread) {
            sharedConnection = redisClient.connectAsync();
        } else {
            sharedConnection = null;
        }

        for (int i = 0; i < threads; i++) {
            Future<List<RedisFuture<String>>> submit = executor.submit(new Callable<List<RedisFuture<String>>>() {
                @Override
                public List<RedisFuture<String>> call() throws Exception {

                    RedisAsyncConnection<String, String> connection = sharedConnection;
                    if (connectionPerThread) {
                        connection = redisClient.connectAsync();
                    }
                    connection.ping().get();

                    List<RedisFuture<String>> futures = Lists.newArrayListWithCapacity(callsPerThread);
                    latch.await();
                    for (int i = 0; i < callsPerThread; i++) {
                        futures.add(connection.ping());
                    }

                    return futures;
                }
            });

            futurama.add(submit);
        }
    }

    protected void preheat(int threads) throws Exception {

        List<Future<?>> futures = Lists.newArrayList();

        for (int i = 0; i < threads; i++) {

            futures.add(executor.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }));
        }

        for (Future<?> future : futures) {
            future.get();
        }

    }
}
