/*
 * Copyright 2017-2020 the original author or authors.
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
