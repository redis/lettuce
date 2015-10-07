package com.lambdaworks.redis.event.metrics;

import com.lambdaworks.redis.event.Event;

/**
 * Event publisher which publishes metrics by the use of {@link Event events}.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @since 3.4
 */
public interface MetricEventPublisher {

    /**
     * Emit immediately a metrics event.
     */
    void emitMetricsEvent();

    /**
     * Returns {@literal true} if the metric collector is enabled.
     * 
     * @return {@literal true} if the metric collector is enabled
     */
    boolean isEnabled();

    /**
     * Shut down the event publisher.
     */
    void shutdown();
}
