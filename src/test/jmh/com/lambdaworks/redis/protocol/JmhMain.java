package com.lambdaworks.redis.protocol;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @soundtrack E-Rotic - Willy Use A Billy Boy (Extended Version)
 */
public class JmhMain {

    public static void main(String... args) throws IOException, RunnerException {
        
        // run selectively
        // runCommandHandlerBenchmark();
        runCommandBenchmark();
    }

    private static void runCommandBenchmark() throws RunnerException {
        
        new Runner(
                prepareOptions().mode(Mode.AverageTime).timeUnit(TimeUnit.NANOSECONDS).include(".*CommandBenchmark.*").build())
                        .run();

        new Runner(prepareOptions().mode(Mode.Throughput).timeUnit(TimeUnit.SECONDS).include(".*CommandBenchmark.*").build())
                .run();
    }

    private static void runCommandHandlerBenchmark() throws RunnerException {

        new Runner(prepareOptions().mode(Mode.AverageTime).timeUnit(TimeUnit.NANOSECONDS).include(".*CommandHandlerBenchmark.userWrite.*")
                .build()).run();
        // new
        // Runner(prepareOptions().mode(Mode.Throughput).timeUnit(TimeUnit.SECONDS).include(".*CommandHandlerBenchmark.*").build()).run();
    }

    private static ChainedOptionsBuilder prepareOptions() {
        return new OptionsBuilder().forks(1).warmupIterations(5).threads(1).measurementIterations(5)
                .timeout(TimeValue.seconds(2));
    }
}
