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
 * Manual JMH Test Launcher.
 * 
 * @author Mark Paluch
 */
public class JmhMain {

    public static void main(String... args) throws IOException, RunnerException {

        // run selectively
        // runCommandBenchmark();
        runCommandHandlerBenchmark();
        // runRedisStateMachineBenchmark();

        // or all
        //runBenchmarks();
    }

    private static void runBenchmarks() throws RunnerException {

        new Runner(prepareOptions().mode(Mode.AverageTime).timeUnit(TimeUnit.NANOSECONDS).build()).run();
    }

    private static void runCommandBenchmark() throws RunnerException {

        new Runner(
                prepareOptions().mode(Mode.AverageTime).timeUnit(TimeUnit.NANOSECONDS).include(".*CommandBenchmark.*").build())
                        .run();

        new Runner(prepareOptions().mode(Mode.Throughput).timeUnit(TimeUnit.SECONDS).include(".*CommandBenchmark.*").build())
                .run();
    }

    private static void runCommandHandlerBenchmark() throws RunnerException {

        new Runner(prepareOptions().mode(Mode.AverageTime).timeUnit(TimeUnit.NANOSECONDS).include(".*CommandHandlerBenchmark.*")
                .build()).run();
        // new
        // Runner(prepareOptions().mode(Mode.Throughput).timeUnit(TimeUnit.SECONDS).include(".*CommandHandlerBenchmark.*").build()).run();
    }

    private static void runCommandEncoderBenchmark() throws RunnerException {

        new Runner(prepareOptions().mode(Mode.AverageTime).timeUnit(TimeUnit.NANOSECONDS).include(".*CommandEncoderBenchmark.*")
                .build()).run();
        // new
        // Runner(prepareOptions().mode(Mode.Throughput).timeUnit(TimeUnit.SECONDS).include(".*CommandHandlerBenchmark.*").build()).run();
    }

    private static void runRedisStateMachineBenchmark() throws RunnerException {

        new Runner(prepareOptions().mode(Mode.AverageTime).timeUnit(TimeUnit.NANOSECONDS)
                .include(".*RedisStateMachineBenchmark.*").build()).run();
        // new
        // Runner(prepareOptions().mode(Mode.Throughput).timeUnit(TimeUnit.SECONDS).include(".*CommandHandlerBenchmark.*").build()).run();
    }

    private static ChainedOptionsBuilder prepareOptions() {
        return new OptionsBuilder().forks(1).warmupIterations(5).threads(1).measurementIterations(5)
                .timeout(TimeValue.seconds(2));
    }
}
