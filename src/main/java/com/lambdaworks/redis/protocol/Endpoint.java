package com.lambdaworks.redis.protocol;

import java.util.Optional;

import io.netty.channel.Channel;

/**
 * Wraps a stateful {@link Endpoint} that abstracts the underlying channel. Endpoints may be connected, disconnected and in
 * closed states. Endpoints may feature reconnection capabilities with replaying queued commands.
 *
 * @author Mark Paluch
 */
interface Endpoint {

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
     * Register a component holding a queue.
     * 
     * @param queueHolder the queue holder.
     */
    void registerQueue(HasQueuedCommands queueHolder);

    /**
     * Unregister a component holding a queue.
     *
     * @param queueHolder the queue holder.
     */
    void unregisterQueue(HasQueuedCommands queueHolder);

    /**
     * Signal the endpoint to drain queued commands from the queue holder.
     * 
     * @param queuedCommands the queue holder.
     */
    void notifyDrainQueuedCommands(HasQueuedCommands queuedCommands);

    /**
     * Associate a {@link ConnectionWatchdog} with the {@link Endpoint}.
     * 
     * @param connectionWatchdog the connection watchdog, may be empty.
     */
    void registerConnectionWatchdog(Optional<ConnectionWatchdog> connectionWatchdog);

}
