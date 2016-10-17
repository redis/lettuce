package com.lambdaworks.redis.dynamic.domain;

import java.util.concurrent.TimeUnit;

import com.lambdaworks.redis.dynamic.annotation.Command;
import com.lambdaworks.redis.internal.LettuceAssert;

/**
 * Timeout value object to represent a timeout value with its {@link TimeUnit}.
 * 
 * @author Mark Paluch
 * @since 5.0
 * @see Command
 */
public class Timeout {

    private final long timeout;
    private final TimeUnit timeUnit;

    private Timeout(long timeout, TimeUnit timeUnit) {

        LettuceAssert.isTrue(timeout >= 0, "Timeout must be greater or equal to zero");
        LettuceAssert.notNull(timeUnit, "TimeUnit must not be null");

        this.timeout = timeout;
        this.timeUnit = timeUnit;
    }

    /**
     * Create a {@link Timeout}.
     * 
     * @param timeout the timeout value, must be non-negative.
     * @param timeUnit the associated {@link TimeUnit}, must not be {@literal null}.
     * @return the {@link Timeout}.
     */
    public static Timeout create(long timeout, TimeUnit timeUnit) {
        return new Timeout(timeout, timeUnit);
    }

    /**
     *
     * @return the timeout value.
     */
    public long getTimeout() {
        return timeout;
    }

    /**
     *
     * @return the {@link TimeUnit}.
     */
    public TimeUnit getTimeUnit() {
        return timeUnit;
    }
}
