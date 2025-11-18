package io.lettuce.core.failover.metrics;

public interface MetricsSnapshot {

    long getSuccessCount();

    long getFailureCount();

    long getTotalCount();

    double getFailureRate();

    double getSuccessRate();

}
