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

/**
 * A listener for Redis command events.
 *
 * @author Mikhael Sokolov
 * @since 6.1
 */
public interface CommandListener {

    /**
     * Listener for command started events.
     *
     * @param event the event.
     */
    default void commandStarted(CommandStartedEvent event) {
    }

    /**
     * Listener for command completed events.
     *
     * @param event the event.
     */
    default void commandSucceeded(CommandSucceededEvent event) {
    }

    /**
     * Listener for command failure events.
     *
     * @param event the event.
     */
    default void commandFailed(CommandFailedEvent event) {
    }

}
