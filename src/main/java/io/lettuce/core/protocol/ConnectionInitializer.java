package io.lettuce.core.protocol;

import java.util.concurrent.CompletionStage;

import io.netty.channel.Channel;

/**
 * Initialize a connection to prepare it for usage.
 *
 * @author Mark Paluch
 * @since 6.0
 */
public interface ConnectionInitializer {

    /**
     * Initialize the connection for usage. This method is invoked after establishing the transport connection and before the
     * connection is used for user-space commands.
     *
     * @param channel the {@link Channel} to initialize.
     * @return the {@link CompletionStage} that completes once the channel is fully initialized.
     */
    CompletionStage<Void> initialize(Channel channel);

}
