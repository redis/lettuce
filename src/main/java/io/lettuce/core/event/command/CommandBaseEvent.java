/*
 * Copyright 2020 the original author or authors.
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
package io.lettuce.core.event.command;

import java.util.Map;

import io.lettuce.core.protocol.RedisCommand;

/**
 * Base class for Redis command events.
 *
 * @author Mikhael Sokolov
 * @since 6.1
 */
public abstract class CommandBaseEvent {

    private final RedisCommand<Object, Object, Object> command;

    private final Map<String, Object> context;

    protected CommandBaseEvent(RedisCommand<Object, Object, Object> command, Map<String, Object> context) {
        this.command = command;
        this.context = context;
    }

    /**
     * @return the actual command.
     */
    public RedisCommand<Object, Object, Object> getCommand() {
        return command;
    }

    /**
     * @return shared context.
     */
    public Map<String, Object> getContext() {
        synchronized (this) {
            return context;
        }
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(getClass().getSimpleName());
        sb.append(" [command=").append(command);
        sb.append(", context=").append(context);
        sb.append(']');
        return sb.toString();
    }

}
