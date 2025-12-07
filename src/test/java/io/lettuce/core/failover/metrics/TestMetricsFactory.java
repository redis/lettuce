package io.lettuce.core.failover.metrics;

public class TestMetricsFactory extends MetricsFactory {

    private final TestClock clock;

    public TestMetricsFactory(TestClock clock) {
        this.clock = clock;
    }

    @Override
    public CircuitBreakerMetrics createDefaultMetrics(int windowSize) {
        return new TestLockFreeSlidingTimeWindowMetrics(windowSize, clock);
    }

}
