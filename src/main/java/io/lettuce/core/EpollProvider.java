/*
 * Copyright 2011-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core;

import java.net.SocketAddress;
import java.util.concurrent.ThreadFactory;

import io.lettuce.core.internal.LettuceAssert;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollDomainSocketChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
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
 */
public class EpollProvider {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(EpollProvider.class);

    private static final String EPOLL_ENABLED_KEY = "io.lettuce.core.epoll";
    private static final boolean EPOLL_ENABLED = Boolean.parseBoolean(SystemPropertyUtil.get(EPOLL_ENABLED_KEY, "true"));

    private static final boolean EPOLL_AVAILABLE;

    static {

        boolean availability = false;
        try {

            Class.forName("io.netty.channel.epoll.Epoll");
            availability = Epoll.isAvailable();
        } catch (ClassNotFoundException e) {
        }

        EPOLL_AVAILABLE = availability;

        if (EPOLL_AVAILABLE) {
            logger.debug("Starting with Epoll library");

        } else {
            logger.debug("Starting without optional Epoll library");
        }
    }

    /**
     * @param type must not be {@literal null}.
     * @return {@literal true} if {@code type} is a {@link io.netty.channel.epoll.EpollEventLoopGroup}.
     */
    public static boolean isEventLoopGroup(Class<? extends EventExecutorGroup> type) {

        LettuceAssert.notNull(type, "EventLoopGroup type must not be null");

        return type.equals(EpollEventLoopGroup.class);
    }

    /**
     * Create a new {@link io.netty.channel.epoll.EpollEventLoopGroup}.
     *
     * @param nThreads
     * @param threadFactory
     * @return the {@link EventLoopGroup}.
     */
    public static EventLoopGroup newEventLoopGroup(int nThreads, ThreadFactory threadFactory) {
        return new EpollEventLoopGroup(nThreads, threadFactory);
    }

    /**
     * @return {@literal true} if epoll is available.
     */
    public static boolean isAvailable() {
        return EPOLL_AVAILABLE && EPOLL_ENABLED;
    }

    /**
     * @return the {@link io.netty.channel.epoll.EpollDomainSocketChannel} class.
     */
    static Class<? extends Channel> domainSocketChannelClass() {
        return EpollDomainSocketChannel.class;
    }

    /**
     * @return the {@link io.netty.channel.epoll.EpollSocketChannel} class.
     */
    static Class<? extends Channel> socketChannelClass() {
        return EpollSocketChannel.class;
    }

    /**
     * @return the {@link io.netty.channel.epoll.EpollEventLoopGroup} class.
     */
    static Class<? extends EventLoopGroup> eventLoopGroupClass() {
        return EpollEventLoopGroup.class;
    }

    /**
     * Check whether the Epoll library is available on the class path.
     *
     * @throws IllegalStateException if the {@literal netty-transport-native-epoll} library is not available
     *
     */
    static void checkForEpollLibrary() {

        LettuceAssert.assertState(EPOLL_ENABLED,
                String.format("epoll use is disabled via System properties (%s)", EPOLL_ENABLED_KEY));
        LettuceAssert
                .assertState(
                        isAvailable(),
                        "Cannot connect using sockets without Epoll support. Make sure netty-transport-native-epoll library on the class path and supported by your operating system.");
    }

    static SocketAddress newSocketAddress(String socketPath) {
        return new DomainSocketAddress(socketPath);
    }
}
