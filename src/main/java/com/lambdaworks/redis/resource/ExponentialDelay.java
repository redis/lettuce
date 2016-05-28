package com.lambdaworks.redis.resource;

import java.util.concurrent.TimeUnit;

/**
 * Delay that increases exponentially on every attempt.
 *
 * <p>
 * Considering retry attempts start at 1, attempt 0 would be the initial call and will always yield 0 (or the lower bound). Then
 * each retry step will by default yield <code>1 * 2 ^ (attemptNumber-1)</code>. Actually each step can be based on a different
 * number than 1 unit of time using the <code>growBy</code> parameter: <code>growBy * 2 ^ (attemptNumber-1)</code>.
 * <p>
 * By default with growBy = 1 this gives us 0 (initial attempt), 1, 2, 4, 8, 16, 32...
 *
 * Each of the resulting values that is below the <code>lowerBound</code> will be replaced by the lower bound, and each value
 * over the <code>upperBound</code> will be replaced by the upper bound.
 *
 * @author Mark Paluch
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

    private long calculateAlternatePower(long attempt) {

        // round will cap at Long.MAX_VALUE and pow should prevent overflows
        double step = Math.pow(this.powersOf, attempt - 1); // attempt > 0
        return Math.round(step);
    }

    // fastpath with bitwise operator
    private long calculatePowerOfTwo(long attempt) {

        long step;
        if (attempt >= 64) { // safeguard against overflow in the bitshift operation
            step = Long.MAX_VALUE;
        } else {
            step = (1L << (attempt - 1));
        }
        // round will cap at Long.MAX_VALUE
        return Math.round(step);
    }

    private long applyBounds(long calculatedValue) {

        if (calculatedValue < lower) {
            return lower;
        }
        if (calculatedValue > upper) {
            return upper;
        }
        return calculatedValue;
    }
}
