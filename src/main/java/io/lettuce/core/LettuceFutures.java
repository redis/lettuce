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
package io.lettuce.core;

import java.time.Duration;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import io.lettuce.core.internal.Futures;

/**
 * Utility to {@link #awaitAll(long, TimeUnit, Future[])} futures until they are done and to synchronize future execution using
 * {@link #awaitOrCancel(RedisFuture, long, TimeUnit)}.
 *
 * @author Mark Paluch
 * @since 3.0
 */
public class LettuceFutures {

    private LettuceFutures() {
    }

    /**
     * Wait until futures are complete or the supplied timeout is reached. Commands are not canceled (in contrast to
     * {@link #awaitOrCancel(RedisFuture, long, TimeUnit)}) when the timeout expires.
     *
     * @param timeout Maximum time to wait for futures to complete.
     * @param futures Futures to wait for.
     * @return {@code true} if all futures complete in time, otherwise {@code false}
     * @since 5.0
     */
    public static boolean awaitAll(Duration timeout, Future<?>... futures) {
        return awaitAll(timeout.toNanos(), TimeUnit.NANOSECONDS, futures);
    }

    /**
     * Wait until futures are complete or the supplied timeout is reached. Commands are not canceled (in contrast to
     * {@link #awaitOrCancel(RedisFuture, long, TimeUnit)}) when the timeout expires.
     *
     * @param timeout Maximum time to wait for futures to complete.
     * @param unit Unit of time for the timeout.
     * @param futures Futures to wait for.
     * @return {@code true} if all futures complete in time, otherwise {@code false}
     */
    public static boolean awaitAll(long timeout, TimeUnit unit, Future<?>... futures) {
        return Futures.awaitAll(timeout, unit, futures);
    }

    /**
     * Wait until futures are complete or the supplied timeout is reached. Commands are canceled if the timeout is reached but
     * the command is not finished.
     *
     * @param cmd Command to wait for
     * @param timeout Maximum time to wait for futures to complete
     * @param unit Unit of time for the timeout
     * @param <T> Result type
     *
     * @return Result of the command.
     */
    public static <T> T awaitOrCancel(RedisFuture<T> cmd, long timeout, TimeUnit unit) {
        return Futures.awaitOrCancel(cmd, timeout, unit);
    }

}
