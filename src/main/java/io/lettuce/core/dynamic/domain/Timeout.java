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
package io.lettuce.core.dynamic.domain;

import java.time.Duration;
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

    private final Duration timeout;

    private Timeout(Duration timeout) {

        LettuceAssert.notNull(timeout, "Timeout must not be null");
        LettuceAssert.isTrue(!timeout.isNegative(), "Timeout must be greater or equal to zero");

        this.timeout = timeout;
    }

    /**
     * Create a {@link Timeout}.
     *
     * @param timeout the timeout value, must be non-negative.
     * @return the {@link Timeout}.
     */
    public static Timeout create(Duration timeout) {
        return new Timeout(timeout);
    }

    /**
     * Create a {@link Timeout}.
     *
     * @param timeout the timeout value, must be non-negative.
     * @param timeUnit the associated {@link TimeUnit}, must not be {@code null}.
     * @return the {@link Timeout}.
     */
    public static Timeout create(long timeout, TimeUnit timeUnit) {

        LettuceAssert.notNull(timeUnit, "TimeUnit must not be null");

        return new Timeout(Duration.ofNanos(timeUnit.toNanos(timeout)));
    }

    /**
     * @return the timeout value.
     */
    public Duration getTimeout() {
        return timeout;
    }

}
