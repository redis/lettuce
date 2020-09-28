/*
 * Copyright 2011-2020 the original author or authors.
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

import static io.lettuce.core.resource.PromiseAdapter.toBooleanPromise;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import io.lettuce.core.internal.LettuceAssert;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.*;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Default implementation which manages one event loop group instance per type.
 *
 * @author Mark Paluch
 * @since 3.4
 */
public class DefaultEventLoopGroupProvider implements EventLoopGroupProvider {

    protected static final InternalLogger logger = InternalLoggerFactory.getInstance(DefaultEventLoopGroupProvider.class);

    private final Lock lock = new ReentrantLock();

    private final Map<Class<? extends EventExecutorGroup>, EventExecutorGroup> eventLoopGroups = new ConcurrentHashMap<>(2);

    private final Map<ExecutorService, Long> refCounter = new ConcurrentHashMap<>(2);

    private final int numberOfThreads;

    private final ThreadFactoryProvider threadFactoryProvider;

    private volatile boolean shutdownCalled = false;

    /**
     * Creates a new instance of {@link DefaultEventLoopGroupProvider}.
     *
     * @param numberOfThreads number of threads (pool size)
     */
    public DefaultEventLoopGroupProvider(int numberOfThreads) {
        this(numberOfThreads, DefaultThreadFactoryProvider.INSTANCE);
    }

    /**
     * Creates a new instance of {@link DefaultEventLoopGroupProvider}.
     *
     * @param numberOfThreads number of threads (pool size)
     * @param threadFactoryProvider provides access to {@link ThreadFactory}.
     * @since 6.0
     */
    public DefaultEventLoopGroupProvider(int numberOfThreads, ThreadFactoryProvider threadFactoryProvider) {

        LettuceAssert.isTrue(numberOfThreads > 0, "Number of threads must be greater than zero");
        LettuceAssert.notNull(threadFactoryProvider, "ThreadFactoryProvider must not be null");

        this.numberOfThreads = numberOfThreads;
        this.threadFactoryProvider = threadFactoryProvider;
    }

    @Override
    public <T extends EventLoopGroup> T allocate(Class<T> type) {

        lock.lock();
        try {
            logger.debug("Allocating executor {}", type.getName());
            return addReference(getOrCreate(type));
        } finally {
            lock.unlock();
        }
    }

    private <T extends ExecutorService> T addReference(T reference) {

        lock.lock();
        try {
            long counter = 0;
            if (refCounter.containsKey(reference)) {
                counter = refCounter.get(reference);
            }

            logger.debug("Adding reference to {}, existing ref count {}", reference, counter);
            counter++;
            refCounter.put(reference, counter);
        } finally {
            lock.unlock();
        }

        return reference;
    }

    private <T extends ExecutorService> T release(T reference) {

        lock.lock();
        try {
            long counter = 0;
            if (refCounter.containsKey(reference)) {
                counter = refCounter.get(reference);
            }

            if (counter < 1) {
                logger.debug("Attempting to release {} but ref count is {}", reference, counter);
            }

            counter--;
            if (counter == 0) {
                refCounter.remove(reference);
            } else {
                refCounter.put(reference, counter);
            }
        } finally {
            lock.unlock();
        }

        return reference;
    }

    @SuppressWarnings("unchecked")
    private <T extends EventLoopGroup> T getOrCreate(Class<T> type) {

        if (shutdownCalled) {
            throw new IllegalStateException("Provider is shut down and can not longer provide resources");
        }

        if (!eventLoopGroups.containsKey(type)) {
            eventLoopGroups.put(type, doCreateEventLoopGroup(type, numberOfThreads, threadFactoryProvider));
        }

        return (T) eventLoopGroups.get(type);
    }

    /**
     * Customization hook for {@link EventLoopGroup} creation.
     *
     * @param <T>
     * @param type requested event loop group type.
     * @param numberOfThreads number of threads to create.
     * @param threadFactoryProvider provider for {@link ThreadFactory}.
     * @return
     * @since 6.0
     */
    protected <T extends EventLoopGroup> EventExecutorGroup doCreateEventLoopGroup(Class<T> type, int numberOfThreads,
            ThreadFactoryProvider threadFactoryProvider) {
        return createEventLoopGroup(type, numberOfThreads, threadFactoryProvider);
    }

    /**
     * Create an instance of a {@link EventExecutorGroup} using the default {@link ThreadFactoryProvider}. Supported types are:
     * <ul>
     * <li>DefaultEventExecutorGroup</li>
     * <li>NioEventLoopGroup</li>
     * <li>EpollEventLoopGroup</li>
     * <li>KqueueEventLoopGroup</li>
     * </ul>
     *
     * @param type the type
     * @param numberOfThreads the number of threads to use for the {@link EventExecutorGroup}
     * @param <T> type parameter
     * @return a new instance of a {@link EventExecutorGroup}
     * @throws IllegalArgumentException if the {@code type} is not supported.
     */
    public static <T extends EventExecutorGroup> EventExecutorGroup createEventLoopGroup(Class<T> type, int numberOfThreads) {
        return createEventLoopGroup(type, numberOfThreads, DefaultThreadFactoryProvider.INSTANCE);
    }

    /**
     * Create an instance of a {@link EventExecutorGroup}. Supported types are:
     * <ul>
     * <li>DefaultEventExecutorGroup</li>
     * <li>NioEventLoopGroup</li>
     * <li>EpollEventLoopGroup</li>
     * <li>KqueueEventLoopGroup</li>
     * </ul>
     *
     * @param type the type
     * @param numberOfThreads the number of threads to use for the {@link EventExecutorGroup}
     * @param <T> type parameter
     * @return a new instance of a {@link EventExecutorGroup}
     * @throws IllegalArgumentException if the {@code type} is not supported.
     * @since 5.3
     */
    private static <T extends EventExecutorGroup> EventExecutorGroup createEventLoopGroup(Class<T> type, int numberOfThreads,
            ThreadFactoryProvider factoryProvider) {

        logger.debug("Creating executor {}", type.getName());

        if (DefaultEventExecutorGroup.class.equals(type)) {
            return new DefaultEventExecutorGroup(numberOfThreads,
                    factoryProvider.getThreadFactory("lettuce-eventExecutorLoop"));
        }

        if (NioEventLoopGroup.class.equals(type)) {
            return new NioEventLoopGroup(numberOfThreads, factoryProvider.getThreadFactory("lettuce-nioEventLoop"));
        }

        if (EpollProvider.isAvailable()) {

            EventLoopResources resources = EpollProvider.getResources();

            if (resources.matches(type)) {
                return resources.newEventLoopGroup(numberOfThreads, factoryProvider.getThreadFactory("lettuce-epollEventLoop"));
            }
        }

        if (KqueueProvider.isAvailable()) {

            EventLoopResources resources = KqueueProvider.getResources();

            if (resources.matches(type)) {
                return resources.newEventLoopGroup(numberOfThreads,
                        factoryProvider.getThreadFactory("lettuce-kqueueEventLoop"));
            }
        }

        throw new IllegalArgumentException(String.format("Type %s not supported", type.getName()));
    }

    @Override
    public Promise<Boolean> release(EventExecutorGroup eventLoopGroup, long quietPeriod, long timeout, TimeUnit unit) {
        return toBooleanPromise(doRelease(eventLoopGroup, quietPeriod, timeout, unit));
    }

    private Future<?> doRelease(EventExecutorGroup eventLoopGroup, long quietPeriod, long timeout, TimeUnit unit) {

        logger.debug("Release executor {}", eventLoopGroup);

        Class<?> key = getKey(release(eventLoopGroup));

        if ((key == null && eventLoopGroup.isShuttingDown()) || refCounter.containsKey(eventLoopGroup)) {
            return new SucceededFuture<>(ImmediateEventExecutor.INSTANCE, true);
        }

        if (key != null) {
            eventLoopGroups.remove(key);
        }

        return eventLoopGroup.shutdownGracefully(quietPeriod, timeout, unit);
    }

    private Class<?> getKey(EventExecutorGroup eventLoopGroup) {
        Class<?> key = null;

        Map<Class<? extends EventExecutorGroup>, EventExecutorGroup> copy = new HashMap<>(eventLoopGroups);
        for (Map.Entry<Class<? extends EventExecutorGroup>, EventExecutorGroup> entry : copy.entrySet()) {
            if (entry.getValue() == eventLoopGroup) {
                key = entry.getKey();
                break;
            }
        }
        return key;
    }

    @Override
    public int threadPoolSize() {
        return numberOfThreads;
    }

    @Override
    public Future<Boolean> shutdown(long quietPeriod, long timeout, TimeUnit timeUnit) {

        logger.debug("Initiate shutdown ({}, {}, {})", quietPeriod, timeout, timeUnit);

        shutdownCalled = true;

        Map<Class<? extends EventExecutorGroup>, EventExecutorGroup> copy = new HashMap<>(eventLoopGroups);

        DefaultPromise<Void> overall = new DefaultPromise<>(ImmediateEventExecutor.INSTANCE);
        PromiseCombiner combiner = new PromiseCombiner(ImmediateEventExecutor.INSTANCE);

        for (EventExecutorGroup executorGroup : copy.values()) {
            combiner.add(doRelease(executorGroup, quietPeriod, timeout, timeUnit));
        }

        combiner.finish(overall);

        return PromiseAdapter.toBooleanPromise(overall);
    }

    /**
     * Interface to provide a custom {@link java.util.concurrent.ThreadFactory}. Implementations are asked through
     * {@link #getThreadFactory(String)} to provide a thread factory for a given pool name.
     *
     * @since 6.0
     */
    public interface ThreadFactoryProvider {

        /**
         * Return a {@link ThreadFactory} for the given {@code poolName}.
         *
         * @param poolName a descriptive pool name. Typically used as prefix for thread names.
         * @return the {@link ThreadFactory}.
         */
        ThreadFactory getThreadFactory(String poolName);

    }

    enum DefaultThreadFactoryProvider implements ThreadFactoryProvider {

        INSTANCE;

        @Override
        public ThreadFactory getThreadFactory(String poolName) {
            return new DefaultThreadFactory(poolName, true);
        }

    }

}
