package io.lettuce.core.event;

import java.time.Duration;

/**
 * Configuration interface for command latency collection.
 *
 * @author Mark Paluch
 */
public interface EventPublisherOptions {

    /**
     * Returns the interval for emit metrics.
     *
     * @return the interval for emit metrics
     */
    Duration eventEmitInterval();

}
