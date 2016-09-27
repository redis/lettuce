package com.lambdaworks.redis.resource;

import java.util.concurrent.TimeUnit;

import com.lambdaworks.redis.resource.Delay.StatefulDelay;

/**
 * Stateful delay that increases using decorrelated jitter strategy.
 *
 * <p>
 * Considering retry attempts start at 1, attempt 0 would be the initial call and will always yield 0 (or the lower). Then, each
 * retry step will by default yield {@code min(cap, randomBetween(base, prevDelay * 3))}.
 *
 * This strategy is based on <a href="https://www.awsarchitectureblog.com/2015/03/backoff.html">Exponential Backoff and
 * Jitter</a>.
 * </p>
 * 
 * @author Jongyeol Choi
 * @since 4.2
 * @see StatefulDelay
 */
class DecorrelatedJitterDelay extends Delay implements StatefulDelay {

    private final long lower;
    private final long upper;
    private final long base;

    /*
     * Delays may be used by different threads, this one is volatile to prevent visibility issues
     */
    private volatile long prevDelay;

    DecorrelatedJitterDelay(long lower, long upper, long base, TimeUnit unit) {
        super(unit);
        this.lower = lower;
        this.upper = upper;
        this.base = base;
        reset();
    }

    @Override
    public long createDelay(long attempt) {
        long value = randomBetween(base, Math.max(base, prevDelay * 3));
        long delay = applyBounds(value, lower, upper);
        prevDelay = delay;
        return delay;
    }

    @Override
    public void reset() {
        prevDelay = 0L;
    }
}
