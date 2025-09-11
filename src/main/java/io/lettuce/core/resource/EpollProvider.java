/*
 * Copyright 2019-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
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

import java.net.SocketAddress;
import java.time.Duration;
import java.util.concurrent.ThreadFactory;

import io.lettuce.core.internal.LettuceAssert;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollDatagramChannel;
import io.netty.channel.epoll.EpollDomainSocketChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollIoHandler;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.unix.DomainSocketAddress;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.internal.SystemPropertyUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Wraps and provides Epoll classes. This is to protect the user from {@link ClassNotFoundException}'s caused by the absence of
 * the {@literal netty-transport-native-epoll} library during runtime. Internal API.
 *
 * @author Mark Paluch
 * @author Yohei Ueki
 * @since 4.4
 */
public class EpollProvider {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(EpollProvider.class);

    private static final String EPOLL_ENABLED_KEY = "io.lettuce.core.epoll";

    private static final boolean EPOLL_ENABLED = Boolean.parseBoolean(SystemPropertyUtil.get(EPOLL_ENABLED_KEY, "true"));

    private static final boolean EPOLL_AVAILABLE;

    private static final EventLoopResources EPOLL_RESOURCES;

    static {

        boolean availability;
        try {
            Class.forName("io.netty.channel.epoll.Epoll");
            availability = Epoll.isAvailable();
        } catch (ClassNotFoundException e) {
            availability = false;
        }

        EPOLL_AVAILABLE = availability;

        if (EPOLL_AVAILABLE) {
            logger.debug("Starting with epoll library");
            EPOLL_RESOURCES = new EventLoopResourcesWrapper(EpollResources.INSTANCE, EpollProvider::checkForEpollLibrary);

        } else {
            logger.debug("Starting without optional epoll library");
            EPOLL_RESOURCES = new EventLoopResourcesWrapper(UnavailableResources.INSTANCE, EpollProvider::checkForEpollLibrary);
        }
    }

    /**
     * @return {@code true} if epoll is available.
     */
    public static boolean isAvailable() {
        return EPOLL_AVAILABLE && EPOLL_ENABLED;
    }

    /**
     * Check whether the Epoll library is available on the class path.
     *
     * @throws IllegalStateException if the {@literal netty-transport-native-epoll} library is not available
     */
    static void checkForEpollLibrary() {

        LettuceAssert.assertState(EPOLL_ENABLED,
                String.format("epoll use is disabled via System properties (%s)", EPOLL_ENABLED_KEY));
        LettuceAssert.assertState(isAvailable(),
                "netty-transport-native-epoll is not available. Make sure netty-transport-native-epoll library on the class path and supported by your operating system.");
    }

    /**
     * Returns the {@link EventLoopResources} for epoll-backed transport. Check availability with {@link #isAvailable()} prior
     * to obtaining the resources.
     *
     * @return the {@link EventLoopResources}. May be unavailable.
     *
     * @since 6.0
     */
    public static EventLoopResources getResources() {
        return EPOLL_RESOURCES;
    }

    /**
     * Apply Keep-Alive options.
     *
     * @since 6.1
     */
    public static void applyKeepAlive(Bootstrap bootstrap, int count, Duration idle, Duration interval) {

        bootstrap.option(EpollChannelOption.TCP_KEEPCNT, count);
        bootstrap.option(EpollChannelOption.TCP_KEEPIDLE, Math.toIntExact(idle.getSeconds()));
        bootstrap.option(EpollChannelOption.TCP_KEEPINTVL, Math.toIntExact(interval.getSeconds()));
    }

    /**
     * Apply TcpUserTimeout options.
     */
    public static void applyTcpUserTimeout(Bootstrap bootstrap, Duration timeout) {
        bootstrap.option(EpollChannelOption.TCP_USER_TIMEOUT, Math.toIntExact(timeout.toMillis()));
    }

    /**
     * {@link EventLoopResources} for available Epoll.
     */
    enum EpollResources implements EventLoopResources {

        INSTANCE;

        @Override
        public boolean matches(Class<? extends EventExecutorGroup> type) {

            LettuceAssert.notNull(type, "EventLoopGroup type must not be null");

            // Support both old deprecated EpollEventLoopGroup and new MultiThreadIoEventLoopGroup
            return type.equals(EpollEventLoopGroup.class) || type.equals(MultiThreadIoEventLoopGroup.class);
        }

        @Override
        public Class<? extends EventLoopGroup> eventLoopGroupClass() {
            // Return the new recommended class, but keep backward compatibility
            return MultiThreadIoEventLoopGroup.class;
        }

        @Override
        public EventLoopGroup newEventLoopGroup(int nThreads, ThreadFactory threadFactory) {
            // Use the new Netty 4.2 approach with IoHandlerFactory
            return new MultiThreadIoEventLoopGroup(nThreads, threadFactory, EpollIoHandler.newFactory());
        }

        @Override
        public Class<? extends Channel> socketChannelClass() {
            return EpollSocketChannel.class;
        }

        @Override
        public Class<? extends Channel> domainSocketChannelClass() {
            return EpollDomainSocketChannel.class;
        }

        @Override
        public Class<? extends DatagramChannel> datagramChannelClass() {
            return EpollDatagramChannel.class;
        }

        @Override
        public SocketAddress newSocketAddress(String socketPath) {
            return new DomainSocketAddress(socketPath);
        }

    }

}
