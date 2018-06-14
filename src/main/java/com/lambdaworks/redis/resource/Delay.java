package com.lambdaworks.redis.resource;

import java.util.concurrent.TimeUnit;

import com.lambdaworks.redis.internal.LettuceAssert;

/**
 * Base class for delays and factory class to create particular instances. {@link Delay} can be subclassed to create custom
 * delay implementations based on attempts. Attempts start with {@value 1}.
 * 
 * @author Mark Paluch
 * @since 4.2
 */
public abstract class Delay {

    /**
     * The time unit of the delay.
     */
    private final TimeUnit timeUnit;

    /**
     * Creates a new {@link Delay}.
     *
     * @param timeUnit the time unit.
     */
    Delay(TimeUnit timeUnit) {

        LettuceAssert.notNull(timeUnit, "TimeUnit must not be null");

        this.timeUnit = timeUnit;
    }

    /**
     * Returns the {@link TimeUnit} associated with this {@link Delay}.
     * 
     * @return the {@link TimeUnit} associated with this {@link Delay}.
     */
    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    /**
     * Calculate a specific delay based on the attempt.
     *
     * This method is to be implemented by the implementations and depending on the params that were set during construction
     * time.
     *
     * @param attempt the attempt to calculate the delay from.
     * @return the calculated delay.
     */
    public abstract long createDelay(long attempt);

    /**
     * Creates a new {@link ConstantDelay}.
     *
     * @param delay the delay, must be greater or equal to 0
     * @param timeUnit the unit of the delay.
     * @return a created {@link ExponentialDelay}.
     */
    public static Delay constant(int delay, TimeUnit timeUnit) {

        LettuceAssert.isTrue(delay >= 0, "Delay must be greater or equal to 0");

        return new ConstantDelay(delay, timeUnit);
    }

    /**
     * Creates a new {@link ExponentialDelay} with default boundaries and factor (1, 2, 4, 8, 16, 32...). The delay begins with
     * 1 and is capped at 30 seconds after reaching the 16th attempt.
     *
     * @return a created {@link ExponentialDelay}.
     */
    public static Delay exponential() {
        return exponential(0, TimeUnit.SECONDS.toMillis(30), TimeUnit.MILLISECONDS, 2);
    }

    /**
     * Creates a new {@link ExponentialDelay} on with custom boundaries and factor (eg. with upper 9000, lower 0, powerOf 10: 1,
     * 10, 100, 1000, 9000, 9000, 9000, ...).
     *
     * @param lower the lower boundary, must be non-negative
     * @param upper the upper boundary, must be greater than the lower boundary
     * @param unit the unit of the delay.
     * @param powersOf the base for exponential growth (eg. powers of 2, powers of 10, etc...), must be non-negative and greater
     *        than 1
     * @return a created {@link ExponentialDelay}.
     */
    public static Delay exponential(long lower, long upper, TimeUnit unit, int powersOf) {

        LettuceAssert.isTrue(lower >= 0, "Lower boundary must be greater or equal to 0");
        LettuceAssert.isTrue(upper > lower, "Upper boundary must be greater than the lower boundary");
        LettuceAssert.isTrue(powersOf > 1, "PowersOf must be greater than 1");

        return new ExponentialDelay(lower, upper, unit, powersOf);
    }
}
