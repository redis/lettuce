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
package io.lettuce.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.SocketAddress;
import java.util.concurrent.ThreadFactory;

import io.lettuce.core.internal.LettuceAssert;
import io.lettuce.core.internal.LettuceClassUtils;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
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
 * @since 4.4
 */
public class KqueueProvider {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(KqueueProvider.class);

    private static final String KQUEUE_ENABLED_KEY = "io.lettuce.core.kqueue";

    private static final boolean KQUEUE_ENABLED = Boolean.parseBoolean(SystemPropertyUtil.get(KQUEUE_ENABLED_KEY, "true"));

    private static final boolean KQUEUE_AVAILABLE;

    private static final KqueueResources KQUEUE_RESOURCES;

    static {

        boolean availability = false;
        try {

            Class<?> kqueue = Class.forName("io.netty.channel.kqueue.KQueue");

            Method isAvailable = kqueue.getDeclaredMethod("isAvailable");

            availability = (Boolean) isAvailable.invoke(null);
        } catch (ReflectiveOperationException e) {
        }

        KQUEUE_AVAILABLE = availability;

        if (KQUEUE_AVAILABLE) {
            logger.debug("Starting with kqueue library");
            KQUEUE_RESOURCES = AvailableKqueueResources.INSTANCE;

        } else {
            logger.debug("Starting without optional kqueue library");
            KQUEUE_RESOURCES = UnavailableKqueueResources.INSTANCE;
        }
    }

    /**
     *
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
     *
     * @param type must not be {@code null}.
     * @return {@code true} if {@code type} is a {@link io.netty.channel.kqueue.KQueueEventLoopGroup}.
     */
    public static boolean isEventLoopGroup(Class<? extends EventExecutorGroup> type) {
        return KQUEUE_RESOURCES.isEventLoopGroup(type);
    }

    /**
     * Create a new {@link io.netty.channel.kqueue.KQueueEventLoopGroup}.
     *
     * @param nThreads
     * @param threadFactory
     * @return the {@link EventLoopGroup}.
     */
    public static EventLoopGroup newEventLoopGroup(int nThreads, ThreadFactory threadFactory) {
        return KQUEUE_RESOURCES.newEventLoopGroup(nThreads, threadFactory);
    }

    /**
     *
     * @return the {@link io.netty.channel.kqueue.KQueueDomainSocketChannel} class.
     */
    static Class<? extends Channel> domainSocketChannelClass() {
        return KQUEUE_RESOURCES.domainSocketChannelClass();
    }

    /**
     *
     * @return the {@link io.netty.channel.kqueue.KQueueSocketChannel} class.
     */
    static Class<? extends Channel> socketChannelClass() {
        return KQUEUE_RESOURCES.socketChannelClass();
    }

    /**
     *
     * @return the {@link io.netty.channel.kqueue.KQueueEventLoopGroup} class.
     */
    static Class<? extends EventLoopGroup> eventLoopGroupClass() {
        return KQUEUE_RESOURCES.eventLoopGroupClass();
    }

    static SocketAddress newSocketAddress(String socketPath) {
        return KQUEUE_RESOURCES.newSocketAddress(socketPath);
    }

    /**
     * @author Mark Paluch
     */
    public interface KqueueResources {

        /**
         * @param type must not be {@code null}.
         * @return {@code true} if {@code type} is a {@link io.netty.channel.kqueue.KQueueEventLoopGroup}.
         */
        boolean isEventLoopGroup(Class<? extends EventExecutorGroup> type);

        /**
         * Create a new {@link io.netty.channel.kqueue.KQueueEventLoopGroup}.
         *
         * @param nThreads
         * @param threadFactory
         * @return the {@link EventLoopGroup}.
         */
        EventLoopGroup newEventLoopGroup(int nThreads, ThreadFactory threadFactory);

        /**
         * @return the {@link io.netty.channel.kqueue.KQueueDomainSocketChannel} class.
         */
        Class<? extends Channel> domainSocketChannelClass();

        /**
         * @return the {@link io.netty.channel.kqueue.KQueueSocketChannel} class.
         */
        Class<? extends Channel> socketChannelClass();

        /**
         * @return the {@link io.netty.channel.kqueue.KQueueEventLoopGroup} class.
         */
        Class<? extends EventLoopGroup> eventLoopGroupClass();

        SocketAddress newSocketAddress(String socketPath);

    }

    /**
     * {@link KqueueResources} for unavailable EPoll.
     */
    enum UnavailableKqueueResources implements KqueueResources {

        INSTANCE;

        @Override
        public Class<? extends Channel> domainSocketChannelClass() {

            checkForKqueueLibrary();
            return null;
        }

        @Override
        public Class<? extends EventLoopGroup> eventLoopGroupClass() {

            checkForKqueueLibrary();
            return null;
        }

        @Override
        public boolean isEventLoopGroup(Class<? extends EventExecutorGroup> type) {

            checkForKqueueLibrary();
            return false;
        }

        @Override
        public EventLoopGroup newEventLoopGroup(int nThreads, ThreadFactory threadFactory) {

            checkForKqueueLibrary();
            return null;
        }

        @Override
        public SocketAddress newSocketAddress(String socketPath) {

            checkForKqueueLibrary();
            return null;
        }

        @Override
        public Class<? extends Channel> socketChannelClass() {

            checkForKqueueLibrary();
            return null;
        }

    }

    /**
     * {@link KqueueResources} for available kqueue.
     */
    enum AvailableKqueueResources implements KqueueResources {

        INSTANCE;

        @Override
        public boolean isEventLoopGroup(Class<? extends EventExecutorGroup> type) {

            LettuceAssert.notNull(type, "EventLoopGroup type must not be null");

            return type.equals(eventLoopGroupClass());
        }

        @Override
        @SuppressWarnings("unchecked")
        public EventLoopGroup newEventLoopGroup(int nThreads, ThreadFactory threadFactory) {

            checkForKqueueLibrary();

            try {
                Constructor<EventLoopGroup> constructor = (Constructor) eventLoopGroupClass()
                        .getDeclaredConstructor(Integer.TYPE, ThreadFactory.class);

                return constructor.newInstance(nThreads, threadFactory);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Cannot create KQueueEventLoopGroup");
            }
        }

        @Override
        public Class<? extends Channel> domainSocketChannelClass() {

            checkForKqueueLibrary();
            return forName("io.netty.channel.kqueue.KQueueDomainSocketChannel");
        }

        @Override
        public Class<? extends Channel> socketChannelClass() {

            checkForKqueueLibrary();
            return forName("io.netty.channel.kqueue.KQueueSocketChannel");
        }

        @Override
        public Class<? extends EventLoopGroup> eventLoopGroupClass() {

            checkForKqueueLibrary();
            return forName("io.netty.channel.kqueue.KQueueEventLoopGroup");
        }

        @Override
        public SocketAddress newSocketAddress(String socketPath) {

            checkForKqueueLibrary();
            return new DomainSocketAddress(socketPath);
        }

    }

    @SuppressWarnings("unchecked")
    static <T> Class<T> forName(String name) {

        try {
            return (Class) LettuceClassUtils.forName(name);
        } catch (ClassNotFoundException e) {
            throw new NoClassDefFoundError(name);
        }
    }

}
