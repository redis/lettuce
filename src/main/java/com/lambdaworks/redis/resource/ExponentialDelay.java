package com.lambdaworks.redis.resource;

import java.util.concurrent.TimeUnit;

/**
 * Delay that increases exponentially on every attempt.
 *
 * <p>
 * Considering retry attempts start at 1, attempt 0 would be the initial call and will always yield 0 (or the lower bound). Then
 * each retry step will by default yield {@code 2 ^ (attemptNumber-1)}. By default this gives us 0 (initial attempt), 1, 2, 4,
 * 8, 16, 32, ...
 * <p>
 * {@link ExponentialDelay} can also apply different {@code powersBy}, such as power of 10 that would apply
 * {@code 10 ^ (attemptNumber-1)} which would give 0, 10, 100, 1000, ...
 * <p>
 * Each of the resulting values that is below the {@code lowerBound} will be replaced by the lower bound, and each value over
 * the {@code upperBound} will be replaced by the upper bound.
 *
 * @author Mark Paluch
 * @author Jongyeol Choi
 * @since 4.1
 */
class ExponentialDelay extends Delay {

    private final long lower;
    private final long upper;
    private final int powersOf;

    ExponentialDelay(long lower, long upper, TimeUnit unit, int powersOf) {

        super(unit);
        this.lower = lower;
        this.upper = upper;
        this.powersOf = powersOf;
    }

    @Override
    public long createDelay(long attempt) {

        long delay;
        if (attempt <= 0) { // safeguard against underflow
            delay = 0;
        } else if (powersOf == 2) {
            delay = calculatePowerOfTwo(attempt);
        } else {
            delay = calculateAlternatePower(attempt);
        }

        return applyBounds(delay);
    }

    /**
     * Apply bounds to the given {@code delay}.
     * 
     * @param delay the delay
     * @return the delay normalized to its lower and upper bounds.
     */
    protected long applyBounds(long delay) {
        return applyBounds(delay, lower, upper);
    }

    private long calculateAlternatePower(long attempt) {

        // round will cap at Long.MAX_VALUE and pow should prevent overflows
        double step = Math.pow(powersOf, attempt - 1); // attempt > 0
        return Math.round(step);
    }

    // fastpath with bitwise operator
    protected static long calculatePowerOfTwo(long attempt) {

        if (attempt <= 0) { // safeguard against underflow
            return 0L;
        } else if (attempt >= 64) { // safeguard against overflow in the bitshift operation
            return Long.MAX_VALUE;
        } else {
            return 1L << (attempt - 1);
        }
    }
}
