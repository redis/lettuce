/*
 * Copyright 2011-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.resource;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import io.lettuce.core.internal.LettuceAssert;

/**
 * Base class for delays and factory class to create particular instances. {@link Delay} can be subclassed to create custom
 * delay implementations based on attempts. Attempts start with {@code 1}.
 * <p>
 * Delays are usually stateless instances that can be shared amongst multiple users (such as connections). Stateful
 * {@link Delay} implementations must implement {@link StatefulDelay} to reset their internal state after the delay is not
 * required anymore.
 *
 * @author Mark Paluch
 * @author Jongyeol Choi
 * @since 4.2
 * @see StatefulDelay
 */
public abstract class Delay {

    private static Duration DEFAULT_LOWER_BOUND = Duration.ZERO;

    private static Duration DEFAULT_UPPER_BOUND = Duration.ofSeconds(30);

    private static int DEFAULT_POWER_OF = 2;

    private static TimeUnit DEFAULT_TIMEUNIT = TimeUnit.MILLISECONDS;

    /**
     * Interface to be implemented by stateful {@link Delay}s. Stateful delays can get reset once a condition (such as
     * successful reconnect) is met. Stateful delays should not be shared by multiple connections but each connection should use
     * its own instance.
     *
     * @see Supplier
     * @see io.lettuce.core.resource.DefaultClientResources.Builder#reconnectDelay(Supplier)
     */
    public interface StatefulDelay {

        /**
         * Reset this delay state. Resetting prepares a stateful delay for its next usage.
         */
        void reset();

    }

    /**
     * Creates a new {@link Delay}.
     */
    protected Delay() {
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
    public abstract Duration createDelay(long attempt);

    /**
     * Creates a new {@link ConstantDelay}.
     *
     * @param delay the delay, must be greater or equal to {@literal 0}.
     * @return a created {@link ConstantDelay}.
     */
    public static Delay constant(Duration delay) {

        LettuceAssert.notNull(delay, "Delay must not be null");
        LettuceAssert.isTrue(delay.toNanos() >= 0, "Delay must be greater or equal to 0");

        return new ConstantDelay(delay);
    }

    /**
     * Creates a new {@link ConstantDelay}.
     *
     * @param delay the delay, must be greater or equal to 0.
     * @param timeUnit the unit of the delay.
     * @return a created {@link ConstantDelay}.
     * @deprecated since 5.0, use {@link #constant(Duration)}.
     */
    @Deprecated
    public static Delay constant(int delay, TimeUnit timeUnit) {

        LettuceAssert.notNull(timeUnit, "TimeUnit must not be null");

        return constant(Duration.ofNanos(timeUnit.toNanos(delay)));
    }

    /**
     * Creates a new {@link ExponentialDelay} with default boundaries and factor (1, 2, 4, 8, 16, 32...). The delay begins with
     * 1 and is capped at 30 {@link TimeUnit#SECONDS} after reaching the 16th attempt.
     *
     * @return a created {@link ExponentialDelay}.
     */
    public static Delay exponential() {
        return exponential(DEFAULT_LOWER_BOUND, DEFAULT_UPPER_BOUND, DEFAULT_POWER_OF, DEFAULT_TIMEUNIT);
    }

    /**
     * Creates a new {@link ExponentialDelay} on with custom boundaries and factor (eg. with upper 9000, lower 0, powerOf 10: 1,
     * 10, 100, 1000, 9000, 9000, 9000, ...).
     *
     * @param lower the lower boundary, must be non-negative.
     * @param upper the upper boundary, must be greater than the lower boundary.
     * @param powersOf the base for exponential growth (eg. powers of 2, powers of 10, etc...), must be non-negative and greater
     *        than 1.
     * @param targetTimeUnit the unit of the delay.
     * @return a created {@link ExponentialDelay}.
     * @since 5.0
     */
    public static Delay exponential(Duration lower, Duration upper, int powersOf, TimeUnit targetTimeUnit) {

        LettuceAssert.notNull(lower, "Lower boundary must not be null");
        LettuceAssert.isTrue(lower.toNanos() >= 0, "Lower boundary must be greater or equal to 0");
        LettuceAssert.notNull(upper, "Upper boundary must not be null");
        LettuceAssert.isTrue(upper.toNanos() > lower.toNanos(), "Upper boundary must be greater than the lower boundary");
        LettuceAssert.isTrue(powersOf > 1, "PowersOf must be greater than 1");
        LettuceAssert.notNull(targetTimeUnit, "Target TimeUnit must not be null");

        return new ExponentialDelay(lower, upper, powersOf, targetTimeUnit);
    }

    /**
     * Creates a new {@link ExponentialDelay} on with custom boundaries and factor (eg. with upper 9000, lower 0, powerOf 10: 1,
     * 10, 100, 1000, 9000, 9000, 9000, ...).
     *
     * @param lower the lower boundary, must be non-negative.
     * @param upper the upper boundary, must be greater than the lower boundary.
     * @param unit the unit of the delay.
     * @param powersOf the base for exponential growth (eg. powers of 2, powers of 10, etc...), must be non-negative and greater
     *        than 1.
     * @return a created {@link ExponentialDelay}.
     */
    public static Delay exponential(long lower, long upper, TimeUnit unit, int powersOf) {

        LettuceAssert.isTrue(lower >= 0, "Lower boundary must be greater or equal to 0");
        LettuceAssert.isTrue(upper > lower, "Upper boundary must be greater than the lower boundary");
        LettuceAssert.isTrue(powersOf > 1, "PowersOf must be greater than 1");
        LettuceAssert.notNull(unit, "TimeUnit must not be null");

        return exponential(Duration.ofNanos(unit.toNanos(lower)), Duration.ofNanos(unit.toNanos(upper)), powersOf, unit);
    }

    /**
     * Creates a new {@link EqualJitterDelay} with default boundaries.
     *
     * @return a created {@link EqualJitterDelay}.
     */
    public static Delay equalJitter() {
        return equalJitter(DEFAULT_LOWER_BOUND, DEFAULT_UPPER_BOUND, 1L, DEFAULT_TIMEUNIT);
    }

    /**
     * Creates a new {@link EqualJitterDelay}.
     *
     * @param lower the lower boundary, must be non-negative.
     * @param upper the upper boundary, must be greater than the lower boundary.
     * @param base the base, must be greater or equal to 1.
     * @param targetTimeUnit the unit of the delay.
     * @return a created {@link EqualJitterDelay}.
     * @since 5.0
     */
    public static Delay equalJitter(Duration lower, Duration upper, long base, TimeUnit targetTimeUnit) {

        LettuceAssert.notNull(lower, "Lower boundary must not be null");
        LettuceAssert.isTrue(lower.toNanos() >= 0, "Lower boundary must be greater or equal to 0");
        LettuceAssert.notNull(upper, "Upper boundary must not be null");
        LettuceAssert.isTrue(upper.toNanos() > lower.toNanos(), "Upper boundary must be greater than the lower boundary");
        LettuceAssert.isTrue(base >= 1, "Base must be greater or equal to 1");
        LettuceAssert.notNull(targetTimeUnit, "Target TimeUnit must not be null");

        return new EqualJitterDelay(lower, upper, base, targetTimeUnit);
    }

    /**
     * Creates a new {@link EqualJitterDelay}.
     *
     * @param lower the lower boundary, must be non-negative.
     * @param upper the upper boundary, must be greater than the lower boundary.
     * @param base the base, must be greater or equal to 1.
     * @param unit the unit of the delay.
     * @return a created {@link EqualJitterDelay}.
     */
    public static Delay equalJitter(long lower, long upper, long base, TimeUnit unit) {

        LettuceAssert.isTrue(lower >= 0, "Lower boundary must be greater or equal to 0");
        LettuceAssert.isTrue(upper > lower, "Upper boundary must be greater than the lower boundary");
        LettuceAssert.isTrue(base >= 1, "Base must be greater or equal to 1");
        LettuceAssert.notNull(unit, "TimeUnit must not be null");

        return equalJitter(Duration.ofNanos(unit.toNanos(lower)), Duration.ofNanos(unit.toNanos(upper)), base, unit);
    }

    /**
     * Creates a new {@link FullJitterDelay} with default boundaries.
     *
     * @return a created {@link FullJitterDelay}.
     */
    public static Delay fullJitter() {
        return fullJitter(DEFAULT_LOWER_BOUND, DEFAULT_UPPER_BOUND, 1L, DEFAULT_TIMEUNIT);
    }

    /**
     * Creates a new {@link FullJitterDelay}.
     *
     * @param lower the lower boundary, must be non-negative.
     * @param upper the upper boundary, must be greater than the lower boundary.
     * @param base the base, must be greater or equal to 1.
     * @param targetTimeUnit the unit of the delay.
     * @return a created {@link FullJitterDelay}.
     * @since 5.0
     */
    public static Delay fullJitter(Duration lower, Duration upper, long base, TimeUnit targetTimeUnit) {

        LettuceAssert.notNull(lower, "Lower boundary must not be null");
        LettuceAssert.isTrue(lower.toNanos() >= 0, "Lower boundary must be greater or equal to 0");
        LettuceAssert.notNull(upper, "Upper boundary must not be null");
        LettuceAssert.isTrue(upper.toNanos() > lower.toNanos(), "Upper boundary must be greater than the lower boundary");
        LettuceAssert.isTrue(base >= 1, "Base must be greater or equal to 1");
        LettuceAssert.notNull(targetTimeUnit, "Target TimeUnit must not be null");

        return new FullJitterDelay(lower, upper, base, targetTimeUnit);
    }

    /**
     * Creates a new {@link FullJitterDelay}.
     *
     * @param lower the lower boundary, must be non-negative.
     * @param upper the upper boundary, must be greater than the lower boundary.
     * @param base the base, must be greater or equal to 1.
     * @param unit the unit of the delay.
     * @return a created {@link FullJitterDelay}.
     */
    public static Delay fullJitter(long lower, long upper, long base, TimeUnit unit) {

        LettuceAssert.isTrue(lower >= 0, "Lower boundary must be greater or equal to 0");
        LettuceAssert.isTrue(upper > lower, "Upper boundary must be greater than the lower boundary");
        LettuceAssert.isTrue(base >= 1, "Base must be greater or equal to 1");
        LettuceAssert.notNull(unit, "TimeUnit must not be null");

        return fullJitter(Duration.ofNanos(unit.toNanos(lower)), Duration.ofNanos(unit.toNanos(upper)), base, unit);
    }

    /**
     * Creates a {@link Supplier} that constructs new {@link DecorrelatedJitterDelay} instances with default boundaries.
     *
     * @return a {@link Supplier} of {@link DecorrelatedJitterDelay}.
     */
    public static Supplier<Delay> decorrelatedJitter() {
        return decorrelatedJitter(DEFAULT_LOWER_BOUND, DEFAULT_UPPER_BOUND, 0L, DEFAULT_TIMEUNIT);
    }

    /**
     * Creates a {@link Supplier} that constructs new {@link DecorrelatedJitterDelay} instances.
     *
     * @param lower the lower boundary, must be non-negative.
     * @param upper the upper boundary, must be greater than the lower boundary.
     * @param base the base, must be greater or equal to 0.
     * @param targetTimeUnit the unit of the delay.
     * @return a new {@link Supplier} of {@link DecorrelatedJitterDelay}.
     * @since 5.0
     */
    public static Supplier<Delay> decorrelatedJitter(Duration lower, Duration upper, long base, TimeUnit targetTimeUnit) {

        LettuceAssert.notNull(lower, "Lower boundary must not be null");
        LettuceAssert.isTrue(lower.toNanos() >= 0, "Lower boundary must be greater or equal to 0");
        LettuceAssert.notNull(upper, "Upper boundary must not be null");
        LettuceAssert.isTrue(upper.toNanos() > lower.toNanos(), "Upper boundary must be greater than the lower boundary");
        LettuceAssert.isTrue(base >= 0, "Base must be greater or equal to 1");
        LettuceAssert.notNull(targetTimeUnit, "Target TimeUnit must not be null");

        // Create new Delay because it has state.
        return () -> new DecorrelatedJitterDelay(lower, upper, base, targetTimeUnit);
    }

    /**
     * Creates a {@link Supplier} that constructs new {@link DecorrelatedJitterDelay} instances.
     *
     * @param lower the lower boundary, must be non-negative.
     * @param upper the upper boundary, must be greater than the lower boundary.
     * @param base the base, must be greater or equal to 0.
     * @param unit the unit of the delay.
     * @return a new {@link Supplier} of {@link DecorrelatedJitterDelay}.
     */
    public static Supplier<Delay> decorrelatedJitter(long lower, long upper, long base, TimeUnit unit) {

        LettuceAssert.isTrue(lower >= 0, "Lower boundary must be greater or equal to 0");
        LettuceAssert.isTrue(upper > lower, "Upper boundary must be greater than the lower boundary");
        LettuceAssert.isTrue(base >= 0, "Base must be greater or equal to 0");
        LettuceAssert.notNull(unit, "TimeUnit must not be null");

        // Create new Delay because it has state.
        return decorrelatedJitter(Duration.ofNanos(unit.toNanos(lower)), Duration.ofNanos(unit.toNanos(upper)), base, unit);
    }

    /**
     * Generates a random long value within {@code min} and {@code max} boundaries.
     *
     * @param min
     * @param max
     * @return a random value.
     * @see ThreadLocalRandom#nextLong(long, long)
     */
    protected static long randomBetween(long min, long max) {
        if (min == max) {
            return min;
        }
        return ThreadLocalRandom.current().nextLong(min, max);
    }

    protected static Duration applyBounds(Duration calculatedValue, Duration lower, Duration upper) {

        if (calculatedValue.compareTo(lower) < 0) {
            return lower;
        }

        if (calculatedValue.compareTo(upper) > 0) {
            return upper;
        }

        return calculatedValue;
    }

}
