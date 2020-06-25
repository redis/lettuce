/*
 * Copyright 2016-2020 the original author or authors.
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

import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.resource.EpollProvider;
import io.lettuce.core.resource.EventLoopResources;
import io.lettuce.core.resource.KqueueProvider;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * Transport infrastructure utility class. This class provides {@link EventLoopGroup} and {@link Channel} classes for socket and
 * native socket transports.
 *
 * @author Mark Paluch
 * @since 4.4
 */
class Transports {

    /**
     * @return the default {@link EventLoopGroup} for socket transport that is compatible with {@link #socketChannelClass()}.
     */
    static Class<? extends EventLoopGroup> eventLoopGroupClass() {

        if (NativeTransports.isSocketSupported()) {
            return NativeTransports.eventLoopGroupClass();
        }

        return NioEventLoopGroup.class;
    }

    /**
     * @return the default {@link Channel} for socket (network/TCP) transport.
     */
    static Class<? extends Channel> socketChannelClass() {

        if (NativeTransports.isSocketSupported()) {
            return NativeTransports.socketChannelClass();
        }

        return NioSocketChannel.class;
    }

    /**
     * Native transport support.
     */
    static class NativeTransports {

        static EventLoopResources RESOURCES = KqueueProvider.isAvailable() ? KqueueProvider.getResources()
                : EpollProvider.getResources();

        /**
         * @return {@code true} if a native transport is available.
         */
        static boolean isSocketSupported() {
            return EpollProvider.isAvailable() || KqueueProvider.isAvailable();
        }

        /**
         * @return the native transport socket {@link Channel} class.
         */
        static Class<? extends Channel> socketChannelClass() {
            return RESOURCES.socketChannelClass();
        }

        /**
         * @return the native transport domain socket {@link Channel} class.
         */
        static Class<? extends Channel> domainSocketChannelClass() {
            return RESOURCES.domainSocketChannelClass();
        }

        /**
         * @return the native transport {@link EventLoopGroup} class.
         */
        static Class<? extends EventLoopGroup> eventLoopGroupClass() {
            return RESOURCES.eventLoopGroupClass();
        }

        static void assertAvailable() {

            LettuceAssert.assertState(NativeTransports.isSocketSupported(),
                    "A unix domain socket connections requires epoll or kqueue and neither is available");
        }

    }

}
