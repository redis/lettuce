/*
 * Copyright 2011-2018 the original author or authors.
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
 * @since 4.4
 */
public class EpollProvider {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(EpollProvider.class);

    private static final String EPOLL_ENABLED_KEY = "io.lettuce.core.epoll";
    private static final boolean EPOLL_ENABLED = Boolean.parseBoolean(SystemPropertyUtil.get(EPOLL_ENABLED_KEY, "true"));

    private static final boolean EPOLL_AVAILABLE;
    private static final EpollResources epollResources;

    static {

        boolean availability = false;
        try {

            Class.forName("io.netty.channel.epoll.Epoll");
            availability = Epoll.isAvailable();
        } catch (ClassNotFoundException e) {
        }

        EPOLL_AVAILABLE = availability;

        if (EPOLL_AVAILABLE) {
            logger.info("Starting with epoll library");
            epollResources = AvailableEpollResources.INSTANCE;

        } else {
            logger.info("Starting without optional epoll library");
            epollResources = UnavailableEpollResources.INSTANCE;
        }
    }

    /**
     * @return {@literal true} if epoll is available.
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
        LettuceAssert
                .assertState(
                        isAvailable(),
                        "netty-transport-native-epoll is not available. Make sure netty-transport-native-epoll library on the class path and supported by your operating system.");
    }

    /**
     * @param type must not be {@literal null}.
     * @return {@literal true} if {@code type} is a {@link io.netty.channel.epoll.EpollEventLoopGroup}.
     */
    public static boolean isEventLoopGroup(Class<? extends EventExecutorGroup> type) {
        return epollResources.isEventLoopGroup(type);
    }

    /**
     * Create a new {@link io.netty.channel.epoll.EpollEventLoopGroup}.
     *
     * @param nThreads
     * @param threadFactory
     * @return the {@link EventLoopGroup}.
     */
    public static EventLoopGroup newEventLoopGroup(int nThreads, ThreadFactory threadFactory) {
        return epollResources.newEventLoopGroup(nThreads, threadFactory);
    }

    /**
     * @return the {@link io.netty.channel.epoll.EpollDomainSocketChannel} class.
     */
    static Class<? extends Channel> domainSocketChannelClass() {
        return epollResources.domainSocketChannelClass();
    }

    /**
     * @return the {@link io.netty.channel.epoll.EpollSocketChannel} class.
     */
    static Class<? extends Channel> socketChannelClass() {
        return epollResources.socketChannelClass();
    }

    /**
     * @return the {@link io.netty.channel.epoll.EpollEventLoopGroup} class.
     */
    static Class<? extends EventLoopGroup> eventLoopGroupClass() {
        return epollResources.eventLoopGroupClass();
    }

    static SocketAddress newSocketAddress(String socketPath) {
        return epollResources.newSocketAddress(socketPath);
    }

    /**
     * @author Mark Paluch
     */
    public interface EpollResources {

        /**
         * @param type must not be {@literal null}.
         * @return {@literal true} if {@code type} is a {@link EpollEventLoopGroup}.
         */
        boolean isEventLoopGroup(Class<? extends EventExecutorGroup> type);

        /**
         * Create a new {@link EpollEventLoopGroup}.
         *
         * @param nThreads
         * @param threadFactory
         * @return the {@link EventLoopGroup}.
         */
        EventLoopGroup newEventLoopGroup(int nThreads, ThreadFactory threadFactory);

        /**
         * @return the {@link EpollDomainSocketChannel} class.
         */
        Class<? extends Channel> domainSocketChannelClass();

        /**
         * @return the {@link EpollSocketChannel} class.
         */
        Class<? extends Channel> socketChannelClass();

        /**
         * @return the {@link EpollEventLoopGroup} class.
         */
        Class<? extends EventLoopGroup> eventLoopGroupClass();

        SocketAddress newSocketAddress(String socketPath);
    }

    /**
     * {@link EpollResources} for unavailable EPoll.
     */
    enum UnavailableEpollResources implements EpollResources {

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
        public boolean isEventLoopGroup(Class<? extends EventExecutorGroup> type) {

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
     * {@link EpollResources} for available Epoll.
     */
    enum AvailableEpollResources implements EpollResources {

        INSTANCE;

        @Override
        public boolean isEventLoopGroup(Class<? extends EventExecutorGroup> type) {

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
