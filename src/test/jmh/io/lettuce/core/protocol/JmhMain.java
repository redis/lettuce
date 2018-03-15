/*
 * Copyright 2011-2018 the original author or authors.
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
package io.lettuce.core.protocol;

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

    public static void main(String... args) throws RunnerException {

        // run selectively
        // runCommandBenchmark();
        runCommandHandlerBenchmark();
        // runRedisEndpointBenchmark();
        // runRedisStateMachineBenchmark();
        // runCommandEncoderBenchmark();

        // or all
        // runBenchmarks();
    }

    private static void runBenchmarks() throws RunnerException {
        new Runner(prepareOptions().mode(Mode.AverageTime).timeUnit(TimeUnit.NANOSECONDS).build()).run();
    }

    private static void runCommandBenchmark() throws RunnerException {

        new Runner(prepareOptions().mode(Mode.AverageTime).timeUnit(TimeUnit.NANOSECONDS).include(".*CommandBenchmark.*")
                .build()).run();

        new Runner(prepareOptions().mode(Mode.Throughput).timeUnit(TimeUnit.SECONDS).include(".*CommandBenchmark.*").build())
                .run();
    }

    private static void runCommandHandlerBenchmark() throws RunnerException {

        new Runner(prepareOptions().mode(Mode.AverageTime).timeUnit(TimeUnit.NANOSECONDS)
                .include(".*CommandHandlerBenchmark.*").build()).run();
        // new
        // Runner(prepareOptions().mode(Mode.Throughput).timeUnit(TimeUnit.SECONDS).include(".*CommandHandlerBenchmark.*").build()).run();
    }

    private static void runRedisEndpointBenchmark() throws RunnerException {

        new Runner(prepareOptions().mode(Mode.AverageTime).timeUnit(TimeUnit.NANOSECONDS).include(".*RedisEndpointBenchmark.*")
                .build()).run();
        // new
        // Runner(prepareOptions().mode(Mode.Throughput).timeUnit(TimeUnit.SECONDS).include(".*CommandHandlerBenchmark.*").build()).run();
    }

    private static void runCommandEncoderBenchmark() throws RunnerException {

        new Runner(prepareOptions().mode(Mode.AverageTime).timeUnit(TimeUnit.NANOSECONDS)
                .include(".*CommandEncoderBenchmark.*").build()).run();
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
