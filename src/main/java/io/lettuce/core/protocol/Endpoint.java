/*
 * Copyright 2011-2018 the original author or authors.
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
package io.lettuce.core.protocol;

import io.netty.channel.Channel;

/**
 * Wraps a stateful {@link Endpoint} that abstracts the underlying channel. Endpoints may be connected, disconnected and in
 * closed states. Endpoints may feature reconnection capabilities with replaying queued commands.
 *
 * @author Mark Paluch
 */
public interface Endpoint {

    /**
     * Notify about channel activation.
     *
     * @param channel the channel
     */
    void notifyChannelActive(Channel channel);

    /**
     * Notify about channel deactivation.
     *
     * @param channel the channel
     */
    void notifyChannelInactive(Channel channel);

    /**
     * Notify about an exception occured in channel/command processing
     *
     * @param t the Exception
     */
    void notifyException(Throwable t);

    /**
     * Signal the endpoint to drain queued commands from the queue holder.
     *
     * @param queuedCommands the queue holder.
     */
    void notifyDrainQueuedCommands(HasQueuedCommands queuedCommands);

    /**
     * Associate a {@link ConnectionWatchdog} with the {@link Endpoint}.
     *
     * @param connectionWatchdog the connection watchdog.
     */
    void registerConnectionWatchdog(ConnectionWatchdog connectionWatchdog);

}
