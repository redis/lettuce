package com.lambdaworks.redis.resource;

import java.util.concurrent.TimeUnit;

/**
 * Delay that increases using full jitter strategy.
 *
 * <p>
 * Considering retry attempts start at 1, attempt 0 would be the initial call and will always yield 0 (or the lower). Then, each
 * retry step will by default yield {@code temp / 2 + random_between(0, temp / 2)} and temp is
 * {@code temp = min(upper, base * 2 ** attempt)}.
 *
 * This strategy is based on <a href="https://www.awsarchitectureblog.com/2015/03/backoff.html">Exponential Backoff and
 * Jitter</a>.
 * </p>
 *
 * @author Jongyeol Choi
 * @since 4.2
 */
class FullJitterDelay extends ExponentialDelay {

    private final long base;
    private final long upper;

    FullJitterDelay(long lower, long upper, long base, TimeUnit unit) {
        super(lower, upper, unit, 2);
        this.upper = upper;
        this.base = base;
    }

    @Override
    public long createDelay(long attempt) {
        long temp = Math.min(upper, base * calculatePowerOfTwo(attempt));
        long delay = temp / 2 + randomBetween(0, temp / 2);
        return applyBounds(delay);
    }
}
