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
 * @author Mark Paluch
 * @since 4.2
 * @see StatefulDelay
 */
class DecorrelatedJitterDelay extends Delay implements StatefulDelay {

    private final Duration lower;

    private final Duration upper;

    private final long base;

    private final TimeUnit targetTimeUnit;

    /*
     * Delays may be used by different threads, this one is volatile to prevent visibility issues
     */
    private volatile long prevDelay;

    DecorrelatedJitterDelay(Duration lower, Duration upper, long base, TimeUnit targetTimeUnit) {
        this.lower = lower;
        this.upper = upper;
        this.base = base;
        this.targetTimeUnit = targetTimeUnit;
        reset();
    }

    @Override
    public Duration createDelay(long attempt) {
        long value = randomBetween(base, Math.max(base, prevDelay * 3));
        Duration delay = applyBounds(Duration.ofNanos(targetTimeUnit.toNanos(value)), lower, upper);
        prevDelay = delay.toNanos();
        return delay;
    }

    @Override
    public void reset() {
        prevDelay = 0L;
    }

}
