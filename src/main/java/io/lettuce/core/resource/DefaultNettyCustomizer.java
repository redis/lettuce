package io.lettuce.core.resource;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;

/**
 * Default (empty) {@link NettyCustomizer} implementation.
 *
 * @author Mark Paluch
 * @since 4.4
 */
enum DefaultNettyCustomizer implements NettyCustomizer {

    INSTANCE;

    @Override
    public void afterBootstrapInitialized(Bootstrap bootstrap) {
        // no-op
    }

    @Override
    public void afterChannelInitialized(Channel channel) {
        // no-op
    }

}
