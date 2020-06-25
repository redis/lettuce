/*
 * Copyright 2016-2020 the original author or authors.
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
package io.lettuce.core.dynamic;

import io.lettuce.core.protocol.RedisCommand;

/**
 * Strategy interface to create {@link RedisCommand}s.
 * <p>
 * Implementing classes are required to construct {@link RedisCommand}s given an array of parameters for command execution.
 *
 * @author Mark Paluch
 * @since 5.0
 */
@FunctionalInterface
interface CommandFactory {

    /**
     * Create a new {@link RedisCommand} given {@code parameters}.
     *
     * @param parameters must not be {@code null}.
     * @return the {@link RedisCommand}.
     */
    RedisCommand<Object, Object, Object> createCommand(Object[] parameters);

}
