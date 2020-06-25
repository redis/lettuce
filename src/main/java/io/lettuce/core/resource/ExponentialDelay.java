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

    private final Duration lower;

    private final Duration upper;

    private final int powersOf;

    private final TimeUnit targetTimeUnit;

    ExponentialDelay(Duration lower, Duration upper, int powersOf, TimeUnit targetTimeUnit) {

        this.lower = lower;
        this.upper = upper;
        this.powersOf = powersOf;
        this.targetTimeUnit = targetTimeUnit;
    }

    @Override
    public Duration createDelay(long attempt) {

        long delay;
        if (attempt <= 0) { // safeguard against underflow
            delay = 0;
        } else if (powersOf == 2) {
            delay = calculatePowerOfTwo(attempt);
        } else {
            delay = calculateAlternatePower(attempt);
        }

        return applyBounds(Duration.ofNanos(targetTimeUnit.toNanos(delay)));
    }

    /**
     * Apply bounds to the given {@code delay}.
     *
     * @param delay the delay
     * @return the delay normalized to its lower and upper bounds.
     */
    protected Duration applyBounds(Duration delay) {
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
