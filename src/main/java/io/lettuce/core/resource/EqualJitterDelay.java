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
