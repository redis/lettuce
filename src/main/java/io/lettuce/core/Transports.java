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

import java.net.SocketOption;
import java.time.Duration;
import java.util.function.Function;

import jdk.net.ExtendedSocketOptions;
import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.resource.EpollProvider;
import io.lettuce.core.resource.EventLoopResources;
import io.lettuce.core.resource.IOUringProvider;
import io.lettuce.core.resource.KqueueProvider;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioChannelOption;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Transport infrastructure utility class. This class provides {@link EventLoopGroup} and {@link Channel} classes for TCP socket
 * and domain socket transports.
 *
 * @author Mark Paluch
 * @since 4.4
 */
class Transports {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(Transports.class);

    /**
     * @return the default {@link EventLoopGroup} for socket transport that is compatible with {@link #socketChannelClass()}.
     */
    static Class<? extends EventLoopGroup> eventLoopGroupClass() {

        if (NativeTransports.isAvailable()) {
            return NativeTransports.eventLoopGroupClass();
        }

        return NioEventLoopGroup.class;
    }

    /**
     * @return the default {@link Channel} for socket (network/TCP) transport.
     */
    static Class<? extends Channel> socketChannelClass() {

        if (NativeTransports.isAvailable()) {
            return NativeTransports.socketChannelClass();
        }

        return NioSocketChannel.class;
    }

    /**
     * Initialize the {@link Bootstrap} and apply {@link SocketOptions}.
     *
     * @since 6.1
     */
    static void configureBootstrap(Bootstrap bootstrap, SocketOptions options, boolean domainSocket,
            Function<Class<? extends EventLoopGroup>, EventLoopGroup> eventLoopGroupProvider) {

        Class<? extends EventLoopGroup> eventLoopGroupClass = Transports.eventLoopGroupClass();

        Class<? extends Channel> channelClass = Transports.socketChannelClass();

        if (domainSocket) {

            NativeTransports.assertDomainSocketAvailable();
            eventLoopGroupClass = NativeTransports.eventLoopGroupClass();
            channelClass = NativeTransports.domainSocketChannelClass();
        }

        EventLoopGroup eventLoopGroup = eventLoopGroupProvider.apply(eventLoopGroupClass);

        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Math.toIntExact(options.getConnectTimeout().toMillis()));

        if (!domainSocket) {
            bootstrap.option(ChannelOption.SO_KEEPALIVE, options.isKeepAlive());
            bootstrap.option(ChannelOption.TCP_NODELAY, options.isTcpNoDelay());
        }

        bootstrap.channel(channelClass).group(eventLoopGroup);

        if (options.isKeepAlive() && options.isExtendedKeepAlive()) {

            SocketOptions.KeepAliveOptions keepAlive = options.getKeepAlive();

            if (IOUringProvider.isAvailable()) {
                IOUringProvider.applyKeepAlive(bootstrap, keepAlive.getCount(), keepAlive.getIdle(), keepAlive.getInterval());
            } else if (EpollProvider.isAvailable()) {
                EpollProvider.applyKeepAlive(bootstrap, keepAlive.getCount(), keepAlive.getIdle(), keepAlive.getInterval());
            } else if (ExtendedNioSocketOptions.isAvailable() && !KqueueProvider.isAvailable()) {
                ExtendedNioSocketOptions.applyKeepAlive(bootstrap, keepAlive.getCount(), keepAlive.getIdle(),
                        keepAlive.getInterval());
            } else {
                logger.warn("Cannot apply extended TCP keepalive options to channel type " + channelClass.getName());
            }
        }
    }

    /**
     * Native transport support.
     */
    static class NativeTransports {

        static EventLoopResources RESOURCES = KqueueProvider.isAvailable() ? KqueueProvider.getResources()
                : IOUringProvider.isAvailable() ? IOUringProvider.getResources() : EpollProvider.getResources();

        /**
         * @return {@code true} if a native transport is available.
         */
        static boolean isAvailable() {
            return EpollProvider.isAvailable() || KqueueProvider.isAvailable() || IOUringProvider.isAvailable();
        }

        /**
         * @return {@code true} if a native transport for domain sockets is available.
         */
        static boolean isDomainSocketSupported() {
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
            assertDomainSocketAvailable();
            return RESOURCES.domainSocketChannelClass();
        }

        /**
         * @return the native transport {@link EventLoopGroup} class.
         */
        static Class<? extends EventLoopGroup> eventLoopGroupClass() {
            return RESOURCES.eventLoopGroupClass();
        }

        static void assertDomainSocketAvailable() {

            LettuceAssert.assertState(NativeTransports.isDomainSocketSupported(),
                    "A unix domain socket connection requires epoll or kqueue and neither is available");
        }

    }

    /**
     * Utility to support Java 11 {@link ExtendedSocketOptions extended keepalive options}.
     */
    @SuppressWarnings("unchecked")
    static class ExtendedNioSocketOptions {

        private static final SocketOption<Integer> TCP_KEEPCOUNT;

        private static final SocketOption<Integer> TCP_KEEPIDLE;

        private static final SocketOption<Integer> TCP_KEEPINTERVAL;

        static {

            SocketOption<Integer> keepCount = null;
            SocketOption<Integer> keepIdle = null;
            SocketOption<Integer> keepInterval = null;
            try {

                keepCount = (SocketOption<Integer>) ExtendedSocketOptions.class.getDeclaredField("TCP_KEEPCOUNT").get(null);
                keepIdle = (SocketOption<Integer>) ExtendedSocketOptions.class.getDeclaredField("TCP_KEEPIDLE").get(null);
                keepInterval = (SocketOption<Integer>) ExtendedSocketOptions.class.getDeclaredField("TCP_KEEPINTERVAL")
                        .get(null);
            } catch (ReflectiveOperationException e) {
                logger.trace("Cannot extract ExtendedSocketOptions for KeepAlive", e);
            }

            TCP_KEEPCOUNT = keepCount;
            TCP_KEEPIDLE = keepIdle;
            TCP_KEEPINTERVAL = keepInterval;
        }

        public static boolean isAvailable() {
            return TCP_KEEPCOUNT != null && TCP_KEEPIDLE != null && TCP_KEEPINTERVAL != null;
        }

        /**
         * Apply Keep-Alive options.
         *
         */
        public static void applyKeepAlive(Bootstrap bootstrap, int count, Duration idle, Duration interval) {

            bootstrap.option(NioChannelOption.of(TCP_KEEPCOUNT), count);
            bootstrap.option(NioChannelOption.of(TCP_KEEPIDLE), Math.toIntExact(idle.getSeconds()));
            bootstrap.option(NioChannelOption.of(TCP_KEEPINTERVAL), Math.toIntExact(interval.getSeconds()));
        }

    }

}
