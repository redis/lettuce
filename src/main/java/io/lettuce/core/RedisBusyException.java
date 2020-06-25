/*
 * Copyright 2017-2020 the original author or authors.
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

/**
 * Exception that gets thrown when Redis is busy executing a Lua script with a {@code BUSY} error response.
 *
 * @author Mark Paluch
 * @since 4.5
 */
@SuppressWarnings("serial")
public class RedisBusyException extends RedisCommandExecutionException {

    /**
     * Create a {@code RedisBusyException} with the specified detail message.
     *
     * @param msg the detail message.
     */
    public RedisBusyException(String msg) {
        super(msg);
    }

    /**
     * Create a {@code RedisNoScriptException} with the specified detail message and nested exception.
     *
     * @param msg the detail message.
     * @param cause the nested exception.
     */
    public RedisBusyException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
