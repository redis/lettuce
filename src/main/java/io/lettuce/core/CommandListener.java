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

package io.lettuce.core;

import io.lettuce.core.models.events.CommandFailedEvent;
import io.lettuce.core.models.events.CommandStartedEvent;
import io.lettuce.core.models.events.CommandSucceededEvent;

/**
 * @author Mikhael Sokolov
 */
public interface CommandListener {

    /**
     * Listener for command started events.
     *
     * @param event the event
     */
    default <K, V, T> void commandStarted(CommandStartedEvent<K, V, T> event) {
    }

    /**
     * Listener for command completed events
     *
     * @param event the event
     */
    default <K, V, T> void commandSucceeded(CommandSucceededEvent<K, V, T> event) {
    }

    /**
     * Listener for command failure events
     *
     * @param event the event
     */
    default <K, V, T> void commandFailed(CommandFailedEvent<K, V, T> event) {
    }

}
