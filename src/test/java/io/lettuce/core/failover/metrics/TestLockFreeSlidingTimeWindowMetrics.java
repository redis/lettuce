package io.lettuce.core.failover.metrics;

public class TestLockFreeSlidingTimeWindowMetrics extends LockFreeSlidingTimeWindowMetrics {

    public TestLockFreeSlidingTimeWindowMetrics(int windowSize, TestClock clock) {
        super(windowSize, clock);
    }

}
