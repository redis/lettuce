package io.lettuce.core.protocol;

import java.util.Deque;

import io.netty.channel.Channel;

/**
 * @author chenxiaofan
 */
public interface AutoBatchFlushEndpoint extends Endpoint {

    @Override
    default void notifyDrainQueuedCommands(HasQueuedCommands queuedCommands) {
        throw new UnsupportedOperationException();
    }

    /**
     * Merge Endpoint#notifyChannelInactive(Channel) and Endpoint#notifyDrainQueuedCommands(HasQueuedCommands)
     *
     * @param channel the channel
     * @param retryableQueuedCommands retryable queued commands in command handler
     */
    void notifyChannelInactiveAfterWatchdogDecision(Channel channel, Deque<RedisCommand<?, ?, ?>> retryableQueuedCommands);

    void notifyReconnectFailed(Throwable throwable);

}
