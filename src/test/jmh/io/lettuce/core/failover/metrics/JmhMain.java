package io.lettuce.core.failover.metrics;

import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.util.concurrent.TimeUnit;

/**
 * Manual JMH Test Launcher.
 *
 * @author Mark Paluch
 */
public class JmhMain {

    public static void main(String... args) throws RunnerException {
        runSlidingTimeWindowMetricsBenchmark();
        runSlidingTimeWindowMetricsBenchmarkThrpt();
    }


    private static void runSlidingTimeWindowMetricsBenchmark() throws RunnerException {

        // measure time-per-op
        new Runner(prepareOptions().mode(Mode.AverageTime)
                .timeUnit(TimeUnit.NANOSECONDS)
                .addProfiler("gc")
                .build()).run();
    }


    private static void runSlidingTimeWindowMetricsBenchmarkThrpt() throws RunnerException {
        // measure thrpt (ops/sec)
        new Runner(prepareOptions().mode(Mode.Throughput)
                .timeUnit(TimeUnit.SECONDS)
                .addProfiler("gc")
                .build()).run();
    }

    private static ChainedOptionsBuilder prepareOptions() {

        return new OptionsBuilder()//
                .forks(1) //
                .warmupIterations(5)//
                .warmupTime(TimeValue.seconds(5))
                .include(".*SlidingTimeWindowMetricsBenchmark.*")
                .measurementIterations(3) //
                .timeout(TimeValue.seconds(5));
    }
}
