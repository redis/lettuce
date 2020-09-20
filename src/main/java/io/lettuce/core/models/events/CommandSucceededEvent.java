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

package io.lettuce.core.models.events;

import io.lettuce.core.protocol.RedisCommand;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Event for succeeded command.
 *
 * @author Mikhael Sokolov
 */
public class CommandSucceededEvent<K, V, T> extends CommandBaseEvent<K, V, T> {

    private final long startedAt;
    private final long succeededAt;

    public CommandSucceededEvent(RedisCommand<K, V, T> command, Map<String, ?> context, long startedAt, long succeededAt) {
        super(command, context);
        this.startedAt = startedAt;
        this.succeededAt = succeededAt;
    }

    /**
     * @return execution duration
     */
    public Duration getExecuteDuration() {
        return Duration.ofMillis(succeededAt - startedAt);
    }

    /**
     * @return datetime when the command was started
     */
    public Instant getStartedAt() {
        return Instant.ofEpochMilli(startedAt);
    }

    /**
     * @return datetime in millis when the command was succeeded
     */
    public Instant getSucceededAt() {
        return Instant.ofEpochMilli(succeededAt);
    }
}
