package com.lambdaworks.redis.metrics;

/**
 * Generic metrics collector interface. A metrics collector collects metrics and emits metric events.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @param <T> data type of the metrics
 * @since 3.4
 *
 */
public interface MetricCollector<T> {

    /**
     * Shut down the metrics collector.
     */
    void shutdown();

    /**
     * Returns the collected/aggregated metrics.
     * 
     * @return the the collected/aggregated metrics
     */
    T retrieveMetrics();

    /**
     * Returns {@literal true} if the metric collector is enabled.
     * 
     * @return {@literal true} if the metric collector is enabled
     */
    boolean isEnabled();
}
