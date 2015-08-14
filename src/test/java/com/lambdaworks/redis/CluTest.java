package com.lambdaworks.redis;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.google.code.tempusfugit.temporal.Duration;
import com.google.code.tempusfugit.temporal.RealClock;
import com.google.code.tempusfugit.temporal.StopWatch;
import com.google.common.collect.Lists;
import com.lambdaworks.redis.cluster.RedisClusterClient;
import com.lambdaworks.redis.codec.ByteArrayCodec;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class CluTest {

    public static void main(String[] args) throws Exception {

        RedisClusterClient client = new RedisClusterClient(RedisURI.create("redis://localhost:7379"));

        RedisClient client2 = new RedisClient(RedisURI.create("redis://localhost:6479"));

        final RedisClusterAsyncConnection<byte[], byte[]> connection = client2.connectAsync(new ByteArrayCodec());

        // final RedisClusterAsyncConnection<byte[], byte[]> connection = client.connectClusterAsync(new ByteArrayCodec());

        for (int i = 0; i < 100; i++) {
            connection.get(("" + i).getBytes());
        }

        int threads = 3;
        final int opsPerThread = 10000;
        ThreadPoolExecutor executor = new ThreadPoolExecutor(threads, threads, 10, TimeUnit.MINUTES,
                new LinkedBlockingDeque<Runnable>(Integer.MAX_VALUE));

        testRun(connection, threads, opsPerThread, executor);
        testRun(connection, threads, opsPerThread, executor);

        double testruns[] = new double[10];
        double mw = 0;
        for (int i = 0; i < testruns.length; i++) {
            testruns[i] = testRun(connection, threads, opsPerThread, executor);
            mw += testruns[i];
        }

        System.out.println(String.format("MW: %.2f ops/sec ", mw / testruns.length));

        client.shutdown();
        client2.shutdown();
        executor.shutdown();

    }

    protected static double testRun(final RedisClusterAsyncConnection<byte[], byte[]> connection, int threads,
            final int opsPerThread, ThreadPoolExecutor executor) throws InterruptedException,
            java.util.concurrent.ExecutionException {
        final AtomicLong al = new AtomicLong();
        connection.setAutoFlushCommands(false);

        final byte[] zed = "Z".getBytes();
        final byte[] cee = "c".getBytes();
        final byte[] ah = "r3".getBytes();
        final byte[] ef = "H".getBytes();

        final CountDownLatch cdl = new CountDownLatch(1);
        List<Future<?>> futures = Lists.newArrayListWithCapacity(threads);
        for (int i = 0; i < threads; i++) {

            futures.add(executor.submit(new Callable<Object>() {
                int i;

                @Override
                public Object call() throws Exception {
                    List<Future<?>> locals = Lists.newArrayListWithCapacity(opsPerThread);
                    cdl.await();

                    for (int j = 0; j < opsPerThread; j++) {
                        // new ClusterCommand<String, String, String>(new Command<String, String, String>(null, new
                        // ValueOutput(null), new CommandArgs(null)), null, 0);

                        i++;
                        byte[] b = zed;
                        if (i % 2 == 0) {
                            b = cee;
                        }

                        if (i % 3 == 0) {
                            b = ah;
                        }

                        if (i % 4 == 0) {
                            b = ef;
                        }
                        if (i % 500 == 0 && i != 0) {
                            connection.flushCommands();
                        }
                        locals.add(connection.get(b));
                    }

                    connection.flushCommands();

                    for (Future<?> future : locals) {
                        future.get();
                    }

                    return null;
                }
            }));
        }

        StopWatch sw = StopWatch.start(RealClock.now());
        cdl.countDown();

        for (Future<?> future : futures) {
            future.get();
        }

        Duration duration = sw.markAndGetTotalElapsedTime();
        double durationSeconds = duration.inMillis() / 1000d;
        double opsPerSecond = (opsPerThread * threads) / durationSeconds;
        System.out.println(String.format("Duration: %d ms (%.2f sec), operations: %d, %.2f ops/sec ", duration.inMillis(),
                durationSeconds, (opsPerThread * threads), opsPerSecond));

        return opsPerSecond;
    }
}
