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
package io.lettuce.core.dynamic.domain;

import java.util.concurrent.TimeUnit;

import io.lettuce.core.dynamic.annotation.Command;
import io.lettuce.core.internal.LettuceAssert;

/**
 * Timeout value object to represent a timeout value with its {@link TimeUnit}.
 *
 * @author Mark Paluch
 * @since 5.0
 * @see Command
 */
public class Timeout {

    private final long timeout;
    private final TimeUnit timeUnit;

    private Timeout(long timeout, TimeUnit timeUnit) {

        LettuceAssert.isTrue(timeout >= 0, "Timeout must be greater or equal to zero");
        LettuceAssert.notNull(timeUnit, "TimeUnit must not be null");

        this.timeout = timeout;
        this.timeUnit = timeUnit;
    }

    /**
     * Create a {@link Timeout}.
     *
     * @param timeout the timeout value, must be non-negative.
     * @param timeUnit the associated {@link TimeUnit}, must not be {@literal null}.
     * @return the {@link Timeout}.
     */
    public static Timeout create(long timeout, TimeUnit timeUnit) {
        return new Timeout(timeout, timeUnit);
    }

    /**
     *
     * @return the timeout value.
     */
    public long getTimeout() {
        return timeout;
    }

    /**
     *
     * @return the {@link TimeUnit}.
     */
    public TimeUnit getTimeUnit() {
        return timeUnit;
    }
}
