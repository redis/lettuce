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

import io.lettuce.core.RedisException;

/**
 * Exception thrown if a command cannot be constructed from a {@link CommandMethod}.
 *
 * @author Mark Paluch
 * @since 5.0
 */
@SuppressWarnings("serial")
public class CommandCreationException extends RedisException {

    private final CommandMethod commandMethod;

    /**
     * Create a new {@link CommandCreationException} given {@link CommandMethod} and a message.
     *
     * @param commandMethod must not be {@code null}.
     * @param msg must not be {@code null}.
     */
    public CommandCreationException(CommandMethod commandMethod, String msg) {

        super(String.format("%s Offending method: %s", msg, commandMethod));
        this.commandMethod = commandMethod;
    }

    /**
     *
     * @return the offending {@link CommandMethod}.
     */
    public CommandMethod getCommandMethod() {
        return commandMethod;
    }

}
