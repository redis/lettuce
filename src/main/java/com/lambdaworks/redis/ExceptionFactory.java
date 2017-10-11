/*
 * Copyright 2017 the original author or authors.
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
package com.lambdaworks.redis;

import java.util.concurrent.TimeUnit;

/**
 * Factory for Redis exceptions.
 * 
 * @author Mark Paluch
 * @since 4.5
 */
public abstract class ExceptionFactory {

    private ExceptionFactory() {
    }

    /**
     * Create a {@link RedisCommandTimeoutException} with a detail message given the timeout.
     * 
     * @param timeout the timeout value.
     * @param unit the {@link TimeUnit}.
     * @return the {@link RedisCommandTimeoutException}.
     */
    public static RedisCommandTimeoutException createTimeoutException(long timeout, TimeUnit unit) {
        return new RedisCommandTimeoutException(String.format("Command timed out after %d %s", timeout, unit));
    }

    /**
     * Create a {@link RedisCommandTimeoutException} with a detail message given the message and timeout.
     * 
     * @param message the detail message.
     * @param timeout the timeout value.
     * @param unit the {@link TimeUnit}.
     * @return the {@link RedisCommandTimeoutException}.
     */
    public static RedisCommandTimeoutException createTimeoutException(String message, long timeout, TimeUnit unit) {
        return new RedisCommandTimeoutException(String.format("%s. Command timed out after %d %s", message, timeout, unit));
    }

    /**
     * Create a {@link RedisCommandExecutionException} with a detail message. Specific Redis error messages may create subtypes
     * of {@link RedisCommandExecutionException}.
     * 
     * @param message the detail message.
     * @return the {@link RedisCommandExecutionException}.
     */
    public static RedisCommandExecutionException createExecutionException(String message) {
        return createExecutionException(message, null);
    }

    /**
     * Create a {@link RedisCommandExecutionException} with a detail message and optionally a {@link Throwable cause}. Specific
     * Redis error messages may create subtypes of {@link RedisCommandExecutionException}.
     * 
     * @param message the detail message.
     * @param cause the nested exception, may be {@literal null}.
     * @return the {@link RedisCommandExecutionException}.
     */
    public static RedisCommandExecutionException createExecutionException(String message, Throwable cause) {

        if (message != null) {

            if (message.startsWith("BUSY")) {
                return cause != null ? new RedisBusyException(message, cause) : new RedisBusyException(message);
            }

            if (message.startsWith("NOSCRIPT")) {
                return cause != null ? new RedisNoScriptException(message, cause) : new RedisNoScriptException(message);
            }

            return cause != null ? new RedisCommandExecutionException(message, cause) : new RedisCommandExecutionException(
                    message);
        }

        return new RedisCommandExecutionException(cause);
    }
}
