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
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.unix.DomainSocketAddress;
import io.netty.channel.uring.IoUring;
import io.netty.channel.uring.IoUringChannelOption;
import io.netty.channel.uring.IoUringDatagramChannel;

import io.netty.channel.uring.IoUringIoHandler;
import io.netty.channel.uring.IoUringSocketChannel;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.internal.SystemPropertyUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Wraps and provides io_uring classes. This is to protect the user from {@link ClassNotFoundException}'s caused by the absence
 * of the {@literal netty-transport-native-io_uring} library during runtime. Internal API.
 *
 * @author Mark Paluch
 * @since 6.1
 */
public class IOUringProvider {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(IOUringProvider.class);

    private static final String IOURING_ENABLED_KEY = "io.lettuce.core.iouring";

    private static final boolean IOURING_ENABLED = Boolean.parseBoolean(SystemPropertyUtil.get(IOURING_ENABLED_KEY, "true"));

    private static final boolean IOURING_AVAILABLE;

    private static final EventLoopResources IOURING_RESOURCES;

    static {

        boolean availability;
        try {
            Class.forName("io.netty.channel.uring.IoUring");
            availability = IoUring.isAvailable();
        } catch (ClassNotFoundException e) {
            availability = false;
        }

        IOURING_AVAILABLE = availability;

        if (IOURING_AVAILABLE) {
            logger.debug("Starting with io_uring library");
            IOURING_RESOURCES = new EventLoopResourcesWrapper(IOUringResources.INSTANCE,
                    IOUringProvider::checkForIOUringLibrary);

        } else {
            logger.debug("Starting without optional io_uring library");
            IOURING_RESOURCES = new EventLoopResourcesWrapper(UnavailableResources.INSTANCE,
                    IOUringProvider::checkForIOUringLibrary);
        }
    }

    /**
     * @return {@code true} if io_uring is available.
     */
    public static boolean isAvailable() {
        return IOURING_AVAILABLE && IOURING_ENABLED;
    }

    /**
     * Check whether the io_uring library is available on the class path.
     *
     * @throws IllegalStateException if the {@literal netty-incubator-transport-native-io_uring} library is not available
     */
    static void checkForIOUringLibrary() {

        LettuceAssert.assertState(IOURING_ENABLED,
                String.format("io_uring use is disabled via System properties (%s)", IOURING_ENABLED_KEY));
        LettuceAssert.assertState(isAvailable(),
                "netty-incubator-transport-native-io_uring is not available. Make sure netty-incubator-transport-native-io_uring library on the class path and supported by your operating system.");
    }

    /**
     * Returns the {@link EventLoopResources} for io_uring-backed transport. Check availability with {@link #isAvailable()}
     * prior to obtaining the resources.
     *
     * @return the {@link EventLoopResources}. May be unavailable.
     */
    public static EventLoopResources getResources() {
        return IOURING_RESOURCES;
    }

    /**
     * Apply Keep-Alive options.
     *
     * @since 6.1
     */
    public static void applyKeepAlive(Bootstrap bootstrap, int count, Duration idle, Duration interval) {

        bootstrap.option(IoUringChannelOption.TCP_KEEPCNT, count);
        bootstrap.option(IoUringChannelOption.TCP_KEEPIDLE, Math.toIntExact(idle.getSeconds()));
        bootstrap.option(IoUringChannelOption.TCP_KEEPINTVL, Math.toIntExact(interval.getSeconds()));
    }

    /**
     * Apply TcpUserTimeout options.
     */
    public static void applyTcpUserTimeout(Bootstrap bootstrap, Duration timeout) {
        bootstrap.option(IoUringChannelOption.TCP_USER_TIMEOUT, Math.toIntExact(timeout.toMillis()));
    }

    /**
     * {@link EventLoopResources} for available io_uring.
     */
    enum IOUringResources implements EventLoopResources {

        INSTANCE;

        @Override
        public boolean matches(Class<? extends EventExecutorGroup> type) {

            LettuceAssert.notNull(type, "EventLoopGroup type must not be null");

            // In Netty 4.2, IoUringEventLoopGroup doesn't exist, only MultiThreadIoEventLoopGroup
            return type.equals(MultiThreadIoEventLoopGroup.class);
        }

        @Override
        public Class<? extends EventLoopGroup> eventLoopGroupClass() {
            // Return the new recommended class, but keep backward compatibility
            return MultiThreadIoEventLoopGroup.class;
        }

        @Override
        public EventLoopGroup newEventLoopGroup(int nThreads, ThreadFactory threadFactory) {
            // Use the new Netty 4.2 approach with IoHandlerFactory
            return new MultiThreadIoEventLoopGroup(nThreads, threadFactory, IoUringIoHandler.newFactory());
        }

        @Override
        public Class<? extends Channel> socketChannelClass() {
            return IoUringSocketChannel.class;
        }

        @Override
        public Class<? extends Channel> domainSocketChannelClass() {
            return IoUringSocketChannel.class;
        }

        @Override
        public Class<? extends DatagramChannel> datagramChannelClass() {
            return IoUringDatagramChannel.class;
        }

        @Override
        public SocketAddress newSocketAddress(String socketPath) {
            return new DomainSocketAddress(socketPath);
        }

        @Override
        public String threadNamePrefix() {
            return "lettuce-io_uringEventLoop";
        }

    }

}
