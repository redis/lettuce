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

import java.time.Instant;
import java.util.HashMap;

import io.lettuce.core.protocol.RedisCommand;

/**
 * Event for a started command.
 *
 * @author Mikhael Sokolov
 * @since 6.1
 */
public class CommandStartedEvent extends CommandBaseEvent {

    private final long startedAt;

    public CommandStartedEvent(RedisCommand<Object, Object, Object> command, long startedAt) {
        super(command, new HashMap<>());
        this.startedAt = startedAt;
    }

    /**
     * @return {@link Instant} when the command was started.
     */
    public Instant getStartedAt() {
        return Instant.ofEpochMilli(startedAt);
    }

}
