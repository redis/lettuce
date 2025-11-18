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
        runFailoverMetricsBenchmark();
    }

    private static void runFailoverMetricsBenchmark() throws RunnerException {

        // measure time-per-op
        new Runner(prepareOptions().mode(Mode.AverageTime)
                .threads(1)
                .timeUnit(TimeUnit.NANOSECONDS)
                .operationsPerInvocation(1_000_000)
                .include(".*FailoverMetricsBenchmark.*")
                //.addProfiler("gc")
                .build()).run();

        // measure thrpt (ops/sec)
        new Runner(prepareOptions().mode(Mode.Throughput)
                .threads(1)
                .timeUnit(TimeUnit.SECONDS)
                //.addProfiler("gc")
                .build()).run();

        new Runner(prepareOptions().mode(Mode.AverageTime)
                .threads(4)
                .timeUnit(TimeUnit.NANOSECONDS)
                //.addProfiler("gc")
                .build()).run();

        // measure thrpt (ops/sec)
        new Runner(prepareOptions().mode(Mode.Throughput)
                .threads(4)
                .timeUnit(TimeUnit.SECONDS)
                //.addProfiler("gc")
                .build()).run();
    }

    private static ChainedOptionsBuilder prepareOptions() {

        return new OptionsBuilder()//
                .forks(1) //
                .warmupIterations(1)//
                .warmupTime(TimeValue.seconds(1))
                .operationsPerInvocation(1_000_000)
                .include(".*FailoverMetricsBenchmark.*")
                .measurementIterations(1) //
                .timeout(TimeValue.seconds(2));
    }
}
