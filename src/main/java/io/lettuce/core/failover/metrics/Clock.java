package io.lettuce.core.failover.metrics;

/**
 * Clock abstraction for obtaining the current time in milliseconds.
 * <p>
 * This interface allows for testable time-dependent code by enabling injection of custom clock implementations.
 * </p>
 *
 * @since 7.1
 */
public interface Clock {

    /**
     * System clock implementation using {@link System#nanoTime()}.
     */
    Clock SYSTEM = System::nanoTime;

    /**
     * Get the current time in nanoseconds.
     *
     * @return the current time in nanoseconds
     */
    long monotonicTime();

}
