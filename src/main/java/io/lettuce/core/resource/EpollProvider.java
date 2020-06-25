/*
 * Copyright 2019-2020 the original author or authors.
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
            EPOLL_RESOURCES = AvailableEpollResources.INSTANCE;

        } else {
            logger.debug("Starting without optional epoll library");
            EPOLL_RESOURCES = UnavailableEpollResources.INSTANCE;
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
     * {@link EventLoopResources} for unavailable EPoll.
     */
    enum UnavailableEpollResources implements EventLoopResources {

        INSTANCE;

        @Override
        public Class<? extends Channel> domainSocketChannelClass() {

            checkForEpollLibrary();
            return null;
        }

        @Override
        public Class<? extends EventLoopGroup> eventLoopGroupClass() {

            checkForEpollLibrary();
            return null;
        }

        @Override
        public boolean matches(Class<? extends EventExecutorGroup> type) {

            checkForEpollLibrary();
            return false;
        }

        @Override
        public EventLoopGroup newEventLoopGroup(int nThreads, ThreadFactory threadFactory) {

            checkForEpollLibrary();
            return null;
        }

        @Override
        public SocketAddress newSocketAddress(String socketPath) {

            checkForEpollLibrary();
            return null;
        }

        @Override
        public Class<? extends Channel> socketChannelClass() {

            checkForEpollLibrary();
            return null;
        }

    }

    /**
     * {@link EventLoopResources} for available Epoll.
     */
    enum AvailableEpollResources implements EventLoopResources {

        INSTANCE;

        @Override
        public boolean matches(Class<? extends EventExecutorGroup> type) {

            LettuceAssert.notNull(type, "EventLoopGroup type must not be null");

            return type.equals(EpollEventLoopGroup.class);
        }

        @Override
        public EventLoopGroup newEventLoopGroup(int nThreads, ThreadFactory threadFactory) {

            checkForEpollLibrary();

            return new EpollEventLoopGroup(nThreads, threadFactory);
        }

        @Override
        public Class<? extends Channel> domainSocketChannelClass() {

            checkForEpollLibrary();

            return EpollDomainSocketChannel.class;
        }

        @Override
        public Class<? extends Channel> socketChannelClass() {

            checkForEpollLibrary();

            return EpollSocketChannel.class;
        }

        @Override
        public Class<? extends EventLoopGroup> eventLoopGroupClass() {

            checkForEpollLibrary();

            return EpollEventLoopGroup.class;
        }

        @Override
        public SocketAddress newSocketAddress(String socketPath) {

            checkForEpollLibrary();

            return new DomainSocketAddress(socketPath);
        }

    }

}
