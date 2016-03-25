package com.lambdaworks.redis.event;

import java.util.concurrent.TimeUnit;

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
    long eventEmitInterval();

    /**
     * Returns the {@link TimeUnit} for the event emit interval.
     *
     * @return the {@link TimeUnit} for the event emit interval
     */
    TimeUnit eventEmitIntervalUnit();
}
