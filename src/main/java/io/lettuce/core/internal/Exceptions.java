/*
 * Copyright 2020 the original author or authors.
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
package io.lettuce.core.internal;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.RedisCommandInterruptedException;
import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.RedisException;

/**
 * Exception handling and utils to operate on.
 *
 * @author Mark Paluch
 * @since 6.0
 */
public class Exceptions {

    /**
     * Unwrap the exception if the given {@link Throwable} is a {@link ExecutionException} or {@link CompletionException}.
     *
     * @param t the root cause
     * @return the unwrapped {@link Throwable#getCause() cause} or the actual {@link Throwable}.
     */
    public static Throwable unwrap(Throwable t) {

        if (t instanceof ExecutionException || t instanceof CompletionException) {
            return t.getCause();
        }

        return t;
    }

    /**
     * Prepare an unchecked {@link RuntimeException} that will bubble upstream if thrown by an operator.
     *
     * @param t the root cause
     * @return an unchecked exception that should choose bubbling up over error callback path.
     */
    public static RuntimeException bubble(Throwable t) {

        Throwable throwableToUse = unwrap(t);

        if (throwableToUse instanceof TimeoutException) {
            return new RedisCommandTimeoutException(throwableToUse);
        }

        if (throwableToUse instanceof InterruptedException) {

            Thread.currentThread().interrupt();
            return new RedisCommandInterruptedException(throwableToUse);
        }

        if (throwableToUse instanceof RedisCommandExecutionException) {
            return ExceptionFactory.createExecutionException(throwableToUse.getMessage(), throwableToUse);
        }

        if (throwableToUse instanceof RedisException) {
            return (RedisException) throwableToUse;
        }

        if (throwableToUse instanceof RuntimeException) {
            return (RuntimeException) throwableToUse;
        }

        return new RedisException(throwableToUse);
    }

    /**
     * Prepare an unchecked {@link RuntimeException} that will bubble upstream for synchronization usage (i.e. on calling
     * {@link Future#get()}).
     *
     * @param t the root cause
     * @return an unchecked exception that should choose bubbling up over error callback path.
     */
    public static RuntimeException fromSynchronization(Throwable t) {

        Throwable throwableToUse = unwrap(t);

        if (throwableToUse instanceof RedisCommandTimeoutException) {
            return new RedisCommandTimeoutException(throwableToUse);
        }

        if (throwableToUse instanceof RedisCommandExecutionException) {
            return bubble(throwableToUse);
        }

        if (throwableToUse instanceof RuntimeException) {
            return new RedisException(throwableToUse);
        }

        return bubble(throwableToUse);
    }

}
