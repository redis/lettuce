package com.lambdaworks.redis.metrics;

import java.util.concurrent.TimeUnit;

/**
 * Configuration interface for command latency collection.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public interface CommandLatencyCollectorOptions {

    /**
     * Returns the target {@link TimeUnit} for the emitted latencies.
     *
     * @return the target {@link TimeUnit} for the emitted latencies
     */
    TimeUnit targetUnit();

    /**
     * Returns the percentiles which should be exposed in the metric.
     *
     * @return the percentiles which should be exposed in the metric
     */
    double[] targetPercentiles();

    /**
     * Returns whether the latencies should be reset once an event is emitted.
     * 
     * @return {@literal true} if the latencies should be reset once an event is emitted.
     */
    boolean resetLatenciesAfterEvent();

    /**
     * Returns whether to distinct latencies on local level. If {@literal true}, multiple connections to the same
     * host/connection point will be recorded separately which allows to inspect every connection individually. If
     * {@literal false}, multiple connections to the same host/connection point will be recorded together. This allows a
     * consolidated view on one particular service.
     * 
     * @return {@literal true} if latencies are recorded distinct on local level (per connection)
     */
    boolean localDistinction();

    /**
     * Returns whether the latency collector is enabled.
     * 
     * @return {@literal true} if the latency collector is enabled
     */
    boolean isEnabled();
}
