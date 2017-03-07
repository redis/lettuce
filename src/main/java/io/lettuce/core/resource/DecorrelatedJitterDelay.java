/*
 * Copyright 2011-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.resource;

import java.util.concurrent.TimeUnit;

import io.lettuce.core.resource.Delay.StatefulDelay;

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
