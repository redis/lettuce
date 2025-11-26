package io.lettuce.core.failover.metrics;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.time.Duration;

/**
 * JMH benchmark for sliding time window metrics.
 *
 * Measure record and query time comparing different implementations, window sizes, and thread counts.
 *
 * @author Ivo Gaydajiev
 * @since 7.1
 */
@State(Scope.Benchmark)
@Fork(1)
public class SlidingTimeWindowMetricsBenchmark {

    @State(Scope.Benchmark)  // shared across all threads
    public static class FailoverMetricsBenchmarkState {
        public CircuitBreakerMetrics metrics;
        public TestClock testClock = new TestClock();

        @Param({"LockFreeSlidingTimeWindowMetrics"})
        public String implementation;

        // Window size in seconds
        @Param({ "2", "30", "180" })
        private int windowSize;

        @Setup(Level.Iteration)
        public void setup() {
            switch (implementation) {
                case "LockFreeSlidingTimeWindowMetrics":
                    metrics = new LockFreeSlidingTimeWindowMetrics(windowSize, testClock);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown impl: " + implementation);
            }
        }
    }

    // simulate total of 1M ops in 5min timeframe
    private static final int RECORDED_EVENTS = 1_000_000;
    private static final long opsPerSecond = RECORDED_EVENTS / Duration.ofMinutes(5).getSeconds();
    private static final long delayBetweenOps = 1_000_000_000 / opsPerSecond; // nanos


    // Benchmark: Record 1M events
    @Benchmark
    @Threads(1)
    @OperationsPerInvocation(RECORDED_EVENTS)
    public void record1MillionEvents1Thread(FailoverMetricsBenchmarkState state) {
        for (int i = 0; i < RECORDED_EVENTS; i++) {
            if (i % 2 == 0) {
                state.metrics.recordSuccess();
            } else {
                state.metrics.recordFailure();
            }
            state.testClock.advance(Duration.ofNanos(delayBetweenOps));
        }
    }

    // Benchmark: Record 1M events
    @Benchmark
    @Threads(4)
    @OperationsPerInvocation(RECORDED_EVENTS)
    public void record1MillionEvents4Threads(FailoverMetricsBenchmarkState state) {
        for (int i = 0; i < RECORDED_EVENTS; i++) {
            if (i % 2 == 0) {
                state.metrics.recordSuccess();
            } else {
                state.metrics.recordFailure();
            }
            state.testClock.advance(Duration.ofNanos(delayBetweenOps));
        }
    }

    // Benchmark: Mixed workload
    @Benchmark
    @Threads(1)
    @OperationsPerInvocation(RECORDED_EVENTS)
    public void recordAndQuery1MillionOps1Thread(FailoverMetricsBenchmarkState state, Blackhole bh) {
        for (int i = 0; i < RECORDED_EVENTS; i++) {
            if (i % 3 == 0) {
                state.metrics.recordSuccess();
            } else if (i % 3 == 1) {
                state.metrics.recordFailure();
            } else {
                bh.consume(state.metrics.getSnapshot());
            }
            state.testClock.advance(Duration.ofNanos(delayBetweenOps));
        }
    }

    // Benchmark: Mixed workload
    @Benchmark
    @Threads(4)
    @OperationsPerInvocation(RECORDED_EVENTS)
    public void recordAndQuery1MillionOps4Threads(FailoverMetricsBenchmarkState state, Blackhole bh) {
        for (int i = 0; i < RECORDED_EVENTS; i++) {
            if (i % 3 == 0) {
                state.metrics.recordSuccess();
            } else if (i % 3 == 1) {
                state.metrics.recordFailure();
            } else {
                bh.consume(state.metrics.getSnapshot());
            }
            state.testClock.advance(Duration.ofNanos(delayBetweenOps));
        }
    }

}