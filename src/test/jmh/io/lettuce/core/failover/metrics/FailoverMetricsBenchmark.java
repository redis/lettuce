package io.lettuce.core.failover.metrics;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;


@State(Scope.Benchmark)
@Fork(1)
public class FailoverMetricsBenchmark {

    @State(Scope.Benchmark)  // shared across all threads
    public static class FailoverMetricsBenchmarkState {
        public SlidingWindowMetrics metrics;

        @Param({"LockFreeSlidingWindowMetrics"})
        public String implementation;

        // Window size in seconds
        // Note: SlidingTimeWindowMetrics requires windowSize >= 2
        @Param({ "2", "10", "60","180", "300", "600" })
        private int windowSize;

        @Setup(Level.Iteration)
        public void setup() {
            switch (implementation) {
                case "SlidingTimeWindowMetrics":
                    metrics = new LockFreeSlidingTimeWindowMetrics(windowSize);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown impl: " + implementation);
            }
        }
    }


    private static final int ONE_MILLION = 1_000_000;


    // Benchmark: Record 1M events (SingleShot mode)
    @Benchmark
    public void record1MillionEvents(FailoverMetricsBenchmarkState state) {
        for (int i = 0; i < ONE_MILLION; i++) {
            if (i % 2 == 0) {
                state.metrics.recordSuccess();
            } else {
                state.metrics.recordFailure();
            }
        }
    }


    // Benchmark: Mixed workload (SingleShot mode)
    @Benchmark
    public void mixedWorkload1Million(FailoverMetricsBenchmarkState state, Blackhole bh) {
        for (int i = 0; i < ONE_MILLION; i++) {
            if (i % 3 == 0) {
                state.metrics.recordSuccess();
            } else if (i % 3 == 1) {
                state.metrics.recordFailure();
            } else {
                bh.consume(state.metrics.getSnapshot());
            }
        }
    }
}