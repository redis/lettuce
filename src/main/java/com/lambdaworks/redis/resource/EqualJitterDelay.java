package com.lambdaworks.redis.resource;

import java.util.concurrent.TimeUnit;

/**
 * Delay that increases using equal jitter strategy.
 *
 * <p>
 * Considering retry attempts start at 1, attempt 0 would be the initial call and will always yield 0 (or the lower). Then, each
 * retry step will by default yield {@code randomBetween(0, base * 2 ^ (attempt - 1))}.
 *
 * This strategy is based on <a href="https://www.awsarchitectureblog.com/2015/03/backoff.html">Exponential Backoff and
 * Jitter</a>.
 * </p>
 *
 * @author Jongyeol Choi
 * @since 4.2
 */
class EqualJitterDelay extends ExponentialDelay {

    private final long base;

    EqualJitterDelay(long lower, long upper, long base, TimeUnit unit) {
        super(lower, upper, unit, 2);
        this.base = base;
    }

    @Override
    public long createDelay(long attempt) {
        long value = randomBetween(0, base * calculatePowerOfTwo(attempt));
        return applyBounds(value);
    }
}
