package io.lettuce.core.resource;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;

/**
 * Strategy interface to customize netty {@link io.netty.bootstrap.Bootstrap} and {@link io.netty.channel.Channel} via callback
 * hooks. <br/>
 * <b>Extending the NettyCustomizer API</b>
 * <p>
 * Contrary to other driver options, the options available in this class should be considered as advanced feature and as such,
 * they should only be modified by expert users. A misconfiguration introduced by the means of this API can have unexpected
 * results and cause the driver to completely fail to connect.
 *
 * @author Mark Paluch
 * @since 4.4
 */
public interface NettyCustomizer {

    /**
     * Hook invoked each time the driver creates a new Connection and configures a new instance of Bootstrap for it. This hook
     * is called after the driver has applied all {@link java.net.SocketOption}s. This is a good place to add extra
     * {@link io.netty.channel.ChannelOption}s to the {@link Bootstrap}.
     *
     * @param bootstrap must not be {@code null}.
     */
    default void afterBootstrapInitialized(Bootstrap bootstrap) {
    }

    /**
     * Hook invoked each time the driver initializes the channel. This hook is called after the driver has registered all its
     * internal channel handlers, and applied the configured options.
     *
     * @param channel must not be {@code null}.
     */
    default void afterChannelInitialized(Channel channel) {
    }

}
