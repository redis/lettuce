/*
 * Copyright 2011-2016 the original author or authors.
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

import java.lang.reflect.Constructor;
import java.net.SocketAddress;
import java.util.Locale;
import java.util.concurrent.ThreadFactory;

import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.internal.LettuceClassUtils;

import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
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
    private static final boolean EPOLL_COMPATIBLE = SystemPropertyUtil.get("os.name").toLowerCase(Locale.UK).trim()
            .startsWith("linux");
    private static final Class<EventLoopGroup> epollEventLoopGroupClass;
    private static final Class<Channel> epollDomainSocketChannelClass;
    private static final Class<Channel> epollSocketChannelClass;
    private static final Class<SocketAddress> domainSocketAddressClass;

    static {

        epollEventLoopGroupClass = getClass("io.netty.channel.epoll.EpollEventLoopGroup");
        epollDomainSocketChannelClass = getClass("io.netty.channel.epoll.EpollDomainSocketChannel");
        epollSocketChannelClass = getClass("io.netty.channel.epoll.EpollSocketChannel");
        domainSocketAddressClass = getClass("io.netty.channel.unix.DomainSocketAddress");

        if (epollDomainSocketChannelClass == null || epollEventLoopGroupClass == null || epollSocketChannelClass == null) {
            logger.debug("Starting without optional Epoll library");
        } else {

            logger.debug("Starting with Epoll library");

            if (!EPOLL_COMPATIBLE) {
                logger.debug(String.format("Epoll not compatible with %s", SystemPropertyUtil.get("os.name")));
            }
        }
    }

    /**
     * @param type must not be {@literal null}.
     * @return {@literal true} if {@code type} is a {@link io.netty.channel.epoll.EpollEventLoopGroup}.
     */
    public static boolean isEventLoopGroup(Class<? extends EventExecutorGroup> type) {

        LettuceAssert.notNull(type, "EventLoopGroup type must not be null");

        return type.equals(epollEventLoopGroupClass);
    }

    /**
     * Create a new {@link io.netty.channel.epoll.EpollEventLoopGroup}.
     *
     * @param nThreads
     * @param threadFactory
     * @return the {@link EventLoopGroup}.
     */
    public static EventLoopGroup newEventLoopGroup(int nThreads, ThreadFactory threadFactory) {

        try {
            Constructor<EventLoopGroup> constructor = epollEventLoopGroupClass.getConstructor(Integer.TYPE,
                    ThreadFactory.class);
            return constructor.newInstance(nThreads, threadFactory);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * @return {@literal true} if epoll is available.
     */
    public static boolean isAvailable() {
        return domainSocketAddressClass != null && epollDomainSocketChannelClass != null && EPOLL_ENABLED && EPOLL_COMPATIBLE;
    }

    /**
     *
     * @return the {@link io.netty.channel.epoll.EpollDomainSocketChannel} class.
     */
    static Class<? extends Channel> domainSocketChannelClass() {
        return epollDomainSocketChannelClass;
    }

    /**
     *
     * @return the {@link io.netty.channel.epoll.EpollSocketChannel} class.
     */
    static Class<? extends Channel> socketChannelClass() {
        return epollSocketChannelClass;
    }

    /**
     *
     * @return the {@link io.netty.channel.epoll.EpollEventLoopGroup} class.
     */
    static Class<? extends EventLoopGroup> eventLoopGroupClass() {
        return epollEventLoopGroupClass;
    }

    /**
     * Check whether the Epoll library is available on the class path.
     *
     * @throws IllegalStateException if the {@literal netty-transport-native-epoll} library is not available
     *
     */
    static void checkForEpollLibrary() {

        LettuceAssert.assertState(EPOLL_ENABLED && EPOLL_COMPATIBLE,
                String.format("epoll use is disabled via System properties (%s)", EPOLL_ENABLED_KEY));
        LettuceAssert.assertState(isAvailable(),
                "Cannot connect using sockets without the optional netty-transport-native-epoll library on the class path");
    }

    static SocketAddress newSocketAddress(String socketPath) {

        try {
            Constructor<SocketAddress> constructor = domainSocketAddressClass.getConstructor(String.class);
            return constructor.newInstance(socketPath);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Try to load class {@literal className}.
     *
     * @param className
     * @param <T> Expected return type for casting.
     * @return instance of {@literal className} or null
     */
    private static <T> Class<T> getClass(String className) {
        try {
            return (Class) LettuceClassUtils.forName(className);
        } catch (ClassNotFoundException e) {
            logger.debug("Cannot load class " + className, e);
        }
        return null;
    }
}
