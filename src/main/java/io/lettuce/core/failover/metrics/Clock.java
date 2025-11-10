package io.lettuce.core.failover.metrics;

/**
 * Clock abstraction for obtaining the current time in milliseconds.
 * <p>
 * This interface allows for testable time-dependent code by enabling injection of custom clock implementations.
 * </p>
 *
 * @since 7.1
 */
@FunctionalInterface
public interface Clock {

    /**
     * System clock implementation using {@link System#currentTimeMillis()}.
     */
    Clock SYSTEM = System::currentTimeMillis;

    /**
     * Get the current time in milliseconds.
     *
     * @return the current time in milliseconds
     */
    long currentTimeMillis();

}

