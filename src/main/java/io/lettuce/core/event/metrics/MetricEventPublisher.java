package io.lettuce.core.event.metrics;

import io.lettuce.core.event.Event;

/**
 * Event publisher which publishes metrics by the use of {@link Event events}.
 *
 * @author Mark Paluch
 * @since 3.4
 */
public interface MetricEventPublisher {

    /**
     * Emit immediately a metrics event.
     */
    void emitMetricsEvent();

    /**
     * Returns {@code true} if the metric collector is enabled.
     *
     * @return {@code true} if the metric collector is enabled
     */
    boolean isEnabled();

    /**
     * Shut down the event publisher.
     */
    void shutdown();

}
