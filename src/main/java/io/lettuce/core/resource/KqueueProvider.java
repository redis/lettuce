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
import java.util.concurrent.ThreadFactory;

import io.lettuce.core.internal.LettuceAssert;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueDatagramChannel;
import io.netty.channel.kqueue.KQueueDomainSocketChannel;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueIoHandler;
import io.netty.channel.kqueue.KQueueSocketChannel;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.unix.DomainSocketAddress;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.internal.SystemPropertyUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Wraps and provides kqueue classes. This is to protect the user from {@link ClassNotFoundException}'s caused by the absence of
 * the {@literal netty-transport-native-kqueue} library during runtime. Internal API.
 *
 * @author Mark Paluch
 * @author Yohei Ueki
 * @since 4.4
 */
public class KqueueProvider {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(KqueueProvider.class);

    private static final String KQUEUE_ENABLED_KEY = "io.lettuce.core.kqueue";

    private static final boolean KQUEUE_ENABLED = Boolean.parseBoolean(SystemPropertyUtil.get(KQUEUE_ENABLED_KEY, "true"));

    private static final boolean KQUEUE_AVAILABLE;

    private static final EventLoopResources KQUEUE_RESOURCES;

    static {

        boolean availability;
        try {
            Class.forName("io.netty.channel.kqueue.KQueue");
            availability = KQueue.isAvailable();
        } catch (ClassNotFoundException e) {
            availability = false;
        }

        KQUEUE_AVAILABLE = availability;

        if (KQUEUE_AVAILABLE) {
            logger.debug("Starting with kqueue library");
            KQUEUE_RESOURCES = new EventLoopResourcesWrapper(KqueueResources.INSTANCE, KqueueProvider::checkForKqueueLibrary);

        } else {
            logger.debug("Starting without optional kqueue library");
            KQUEUE_RESOURCES = new EventLoopResourcesWrapper(UnavailableResources.INSTANCE,
                    KqueueProvider::checkForKqueueLibrary);
        }
    }

    /**
     * @return {@code true} if kqueue is available.
     */
    public static boolean isAvailable() {
        return KQUEUE_AVAILABLE && KQUEUE_ENABLED;
    }

    /**
     * Check whether the kqueue library is available on the class path.
     *
     * @throws IllegalStateException if the {@literal netty-transport-native-kqueue} library is not available
     */
    static void checkForKqueueLibrary() {

        LettuceAssert.assertState(KQUEUE_ENABLED,
                String.format("kqueue use is disabled via System properties (%s)", KQUEUE_ENABLED_KEY));
        LettuceAssert.assertState(isAvailable(),
                "netty-transport-native-kqueue is not available. Make sure netty-transport-native-kqueue library on the class path and supported by your operating system.");
    }

    /**
     * Returns the {@link EventLoopResources} for kqueue-backed transport. Check availability with {@link #isAvailable()} prior
     * to obtaining the resources.
     *
     * @return the {@link EventLoopResources}. May be unavailable.
     *
     * @since 6.0
     */
    public static EventLoopResources getResources() {
        return KQUEUE_RESOURCES;
    }

    /**
     * {@link EventLoopResources} for unavailable Kqueue.
     */
    enum UnavailableKqueueResources implements EventLoopResources {

        INSTANCE;

        @Override
        public boolean matches(Class<? extends EventExecutorGroup> type) {

            checkForKqueueLibrary();
            return false;
        }

        @Override
        public Class<? extends EventLoopGroup> eventLoopGroupClass() {

            checkForKqueueLibrary();
            return null;
        }

        @Override
        public EventLoopGroup newEventLoopGroup(int nThreads, ThreadFactory threadFactory) {

            checkForKqueueLibrary();
            return null;
        }

        @Override
        public Class<? extends Channel> socketChannelClass() {

            checkForKqueueLibrary();
            return null;
        }

        @Override
        public Class<? extends Channel> domainSocketChannelClass() {

            checkForKqueueLibrary();
            return null;
        }

        @Override
        public SocketAddress newSocketAddress(String socketPath) {

            checkForKqueueLibrary();
            return null;
        }

        @Override
        public Class<? extends DatagramChannel> datagramChannelClass() {

            checkForKqueueLibrary();
            return null;
        }

    }

    /**
     * {@link EventLoopResources} for available kqueue.
     */
    enum KqueueResources implements EventLoopResources {

        INSTANCE;

        @Override
        public boolean matches(Class<? extends EventExecutorGroup> type) {

            LettuceAssert.notNull(type, "EventLoopGroup type must not be null");

            // Support both old deprecated KQueueEventLoopGroup and new MultiThreadIoEventLoopGroup
            return type.equals(KQueueEventLoopGroup.class) || type.equals(MultiThreadIoEventLoopGroup.class);
        }

        @Override
        public Class<? extends EventLoopGroup> eventLoopGroupClass() {

            checkForKqueueLibrary();

            // Return the new recommended class, but keep backward compatibility
            return MultiThreadIoEventLoopGroup.class;
        }

        @Override
        public EventLoopGroup newEventLoopGroup(int nThreads, ThreadFactory threadFactory) {

            checkForKqueueLibrary();

            // Use the new Netty 4.2 approach with IoHandlerFactory
            return new MultiThreadIoEventLoopGroup(nThreads, threadFactory, KQueueIoHandler.newFactory());
        }

        @Override
        public Class<? extends Channel> socketChannelClass() {

            checkForKqueueLibrary();

            return KQueueSocketChannel.class;
        }

        @Override
        public Class<? extends DatagramChannel> datagramChannelClass() {

            checkForKqueueLibrary();

            return KQueueDatagramChannel.class;
        }

        @Override
        public Class<? extends Channel> domainSocketChannelClass() {

            checkForKqueueLibrary();

            return KQueueDomainSocketChannel.class;
        }

        @Override
        public SocketAddress newSocketAddress(String socketPath) {

            checkForKqueueLibrary();

            return new DomainSocketAddress(socketPath);
        }

    }

}
