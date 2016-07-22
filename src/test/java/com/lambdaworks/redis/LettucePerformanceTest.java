package com.lambdaworks.redis;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.PropertyConfigurator;
import org.junit.*;

import com.google.common.io.Resources;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.api.rx.RedisReactiveCommands;

import rx.Observable;

/**
 * @author Mark Paluch
 */
@Ignore
public class LettucePerformanceTest {

    private static RedisClient redisClient = new RedisClient(TestSettings.host(), TestSettings.port());
    private ExecutorService executor;
    private CountDownLatch latch = new CountDownLatch(1);

    @Before
    public void before() throws Exception {
        LogManager.resetConfiguration();
        LogManager.getRootLogger().setLevel(Level.WARN);
    }

    @After
    public void after() throws Exception {
        PropertyConfigurator.configure(Resources.getResource("log4j.properties"));
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        redisClient.shutdown();
    }

    /**
     * Multi-threaded performance test.
     *
     * Uses a {@link ThreadPoolExecutor} with thread and connection preheating. Execution tasks are submitted and synchronized
     * with a {@link CountDownLatch}
     *
     * @throws Exception
     */
    @Test
    public void testSyncAsyncPerformance() throws Exception {

        // TWEAK ME
        int threads = 4;
        int totalCalls = 250000;
        boolean waitForFutureCompletion = true;
        boolean connectionPerThread = false;
        // Keep in mind, that the size of the event loop threads is CPU count * 4 unless you
        // set -Dio.netty.eventLoopThreads=...
        // END OF TWEAK ME

        executor = new ThreadPoolExecutor(threads, threads, 1, TimeUnit.MINUTES, new ArrayBlockingQueue<Runnable>(totalCalls));

        List<Future<List<CompletableFuture<String>>>> futurama = new ArrayList<>();

        preheat(threads);

        final int callsPerThread = totalCalls / threads;

        submitExecutionTasks(threads, futurama, callsPerThread, connectionPerThread);
        Thread.sleep(800);

        long start = System.currentTimeMillis();
        latch.countDown();

        for (Future<List<CompletableFuture<String>>> listFuture : futurama) {
            for (CompletableFuture<String> future : listFuture.get()) {
                if (waitForFutureCompletion) {
                    future.get();
                }
            }
        }

        long end = System.currentTimeMillis();

        long duration = end - start;
        double durationSeconds = duration / 1000d;
        double opsPerSecond = totalCalls / durationSeconds;
        System.out.println(String.format("Sync/Async: Duration: %d ms (%.2f sec), operations: %d, %.2f ops/sec ", duration,
                durationSeconds, totalCalls, opsPerSecond));

        for (Future<List<CompletableFuture<String>>> listFuture : futurama) {
            for (CompletableFuture<String> future : listFuture.get()) {
                future.get();
            }
        }

    }

    protected void submitExecutionTasks(int threads, List<Future<List<CompletableFuture<String>>>> futurama,
            final int callsPerThread, final boolean connectionPerThread) {
        final RedisAsyncConnection<String, String> sharedConnection;
        if (!connectionPerThread) {
            sharedConnection = redisClient.connectAsync();
        } else {
            sharedConnection = null;
        }

        for (int i = 0; i < threads; i++) {
            Future<List<CompletableFuture<String>>> submit = executor.submit(() -> {

                RedisAsyncConnection<String, String> connection = sharedConnection;
                if (connectionPerThread) {
                    connection = redisClient.connectAsync();
                }
                connection.ping().get();

                List<CompletableFuture<String>> futures = new ArrayList<>(callsPerThread);
                latch.await();
                for (int i1 = 0; i1 < callsPerThread; i1++) {
                    futures.add(connection.ping().toCompletableFuture());
                }

                return futures;
            });

            futurama.add(submit);
        }
    }

    /**
     * Multi-threaded performance using reactive commands.
     *
     * Uses a {@link ThreadPoolExecutor} with thread and connection preheating. Execution tasks are submitted and synchronized
     * with a {@link CountDownLatch}
     *
     * @throws Exception
     */
    @Test
    public void testObservablePerformance() throws Exception {

        // TWEAK ME
        int threads = 4;
        int totalCalls = 25000;
        boolean waitForCompletion = true;
        boolean connectionPerThread = false;
        // Keep in mind, that the size of the event loop threads is CPU count * 4 unless you
        // set -Dio.netty.eventLoopThreads=...
        // END OF TWEAK ME

        executor = new ThreadPoolExecutor(threads, threads, 1, TimeUnit.MINUTES, new ArrayBlockingQueue<Runnable>(totalCalls));

        List<Future<List<Observable<String>>>> futurama = new ArrayList<>();

        preheat(threads);
        final int callsPerThread = totalCalls / threads;

        submitObservableTasks(threads, futurama, callsPerThread, connectionPerThread);
        Thread.sleep(800);

        long start = System.currentTimeMillis();
        latch.countDown();

        for (Future<List<Observable<String>>> listFuture : futurama) {
            for (Observable<String> future : listFuture.get()) {
                if (waitForCompletion) {
                    future.toBlocking().last();
                } else {
                    future.subscribe();
                }
            }
        }

        long end = System.currentTimeMillis();

        long duration = end - start;
        double durationSeconds = duration / 1000d;
        double opsPerSecond = totalCalls / durationSeconds;
        System.out.println(String.format("Reactive Duration: %d ms (%.2f sec), operations: %d, %.2f ops/sec ", duration,
                durationSeconds, totalCalls, opsPerSecond));

    }

    protected void submitObservableTasks(int threads, List<Future<List<Observable<String>>>> futurama, final int callsPerThread,
            final boolean connectionPerThread) {
        final StatefulRedisConnection<String, String> sharedConnection;
        if (!connectionPerThread) {
            sharedConnection = redisClient.connectAsync().getStatefulConnection();
        } else {
            sharedConnection = null;
        }

        for (int i = 0; i < threads; i++) {
            Future<List<Observable<String>>> submit = executor.submit(() -> {

                StatefulRedisConnection<String, String> connection = sharedConnection;
                if (connectionPerThread) {
                    connection = redisClient.connectAsync().getStatefulConnection();
                }
                RedisReactiveCommands<String, String> reactive = connection.reactive();

                connection.sync().ping();

                List<Observable<String>> observables = new ArrayList<>(callsPerThread);
                latch.await();
                for (int i1 = 0; i1 < callsPerThread; i1++) {
                    observables.add(reactive.ping());
                }

                return observables;
            });

            futurama.add(submit);
        }
    }

    protected void preheat(int threads) throws Exception {

        List<Future<?>> futures = new ArrayList<>();

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
