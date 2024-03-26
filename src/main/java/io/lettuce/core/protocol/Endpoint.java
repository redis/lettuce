package io.lettuce.core.protocol;

import io.netty.channel.Channel;

/**
 * Wraps a stateful {@link Endpoint} that abstracts the underlying channel. Endpoints may be connected, disconnected and in
 * closed states. Endpoints may feature reconnection capabilities with replaying queued commands.
 *
 * @author Mark Paluch
 */
public interface Endpoint extends PushHandler {

    /**
     * Reset this endpoint to its initial state, clear all buffers and potentially close the bound channel.
     *
     * @since 5.1
     */
    void initialState();

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

    /**
     * @return the endpoint Id.
     * @since 6.1
     */
    String getId();
}
