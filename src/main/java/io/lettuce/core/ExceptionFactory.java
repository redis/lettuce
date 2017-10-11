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
package io.lettuce.core;

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
     * Create a {@link RedisCommandExecutionException} with a detail message. Specific Redis error messages may create subtypes
     * of {@link RedisCommandExecutionException}.
     * 
     * @param message the detail message.
     * @param cause the nested exception.
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
