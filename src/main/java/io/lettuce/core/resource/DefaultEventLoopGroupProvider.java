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
package io.lettuce.core.resource;

import static io.lettuce.core.resource.Futures.toBooleanPromise;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import io.lettuce.core.EpollProvider;
import io.lettuce.core.KqueueProvider;
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

    private final Map<Class<? extends EventExecutorGroup>, EventExecutorGroup> eventLoopGroups = new ConcurrentHashMap<>(2);
    private final Map<ExecutorService, Long> refCounter = new ConcurrentHashMap<>(2);

    private final int numberOfThreads;

    private volatile boolean shutdownCalled = false;

    /**
     * Creates a new instance of {@link DefaultEventLoopGroupProvider}.
     *
     * @param numberOfThreads number of threads (pool size)
     */
    public DefaultEventLoopGroupProvider(int numberOfThreads) {
        this.numberOfThreads = numberOfThreads;
    }

    @Override
    public <T extends EventLoopGroup> T allocate(Class<T> type) {
        synchronized (this) {
            return addReference(getOrCreate(type));
        }
    }

    private <T extends ExecutorService> T addReference(T reference) {

        synchronized (refCounter) {
            long counter = 0;
            if (refCounter.containsKey(reference)) {
                counter = refCounter.get(reference);
            }

            logger.debug("Adding reference to {}, existing ref count {}", reference, counter);
            counter++;
            refCounter.put(reference, counter);
        }

        return reference;
    }

    private <T extends ExecutorService> T release(T reference) {

        synchronized (refCounter) {
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
        }

        return reference;
    }

    @SuppressWarnings("unchecked")
    private <T extends EventLoopGroup> T getOrCreate(Class<T> type) {

        if (shutdownCalled) {
            throw new IllegalStateException("Provider is shut down and can not longer provide resources");
        }

        if (!eventLoopGroups.containsKey(type)) {
            eventLoopGroups.put(type, createEventLoopGroup(type, numberOfThreads));
        }

        return (T) eventLoopGroups.get(type);
    }

    /**
     * Create an instance of a {@link EventExecutorGroup}. Supported types are:
     * <ul>
     * <li>DefaultEventExecutorGroup</li>
     * <li>NioEventLoopGroup</li>
     * <li>EpollEventLoopGroup</li>
     * </ul>
     *
     * @param type the type
     * @param numberOfThreads the number of threads to use for the {@link EventExecutorGroup}
     * @param <T> type parameter
     * @return a new instance of a {@link EventExecutorGroup}
     * @throws IllegalArgumentException if the {@code type} is not supported.
     */
    public static <T extends EventExecutorGroup> EventExecutorGroup createEventLoopGroup(Class<T> type, int numberOfThreads) {

        if (DefaultEventExecutorGroup.class.equals(type)) {
            return new DefaultEventExecutorGroup(numberOfThreads, new DefaultThreadFactory("lettuce-eventExecutorLoop", true));
        }

        if (NioEventLoopGroup.class.equals(type)) {
            return new NioEventLoopGroup(numberOfThreads, new DefaultThreadFactory("lettuce-nioEventLoop", true));
        }

        if (EpollProvider.isAvailable() && EpollProvider.isEventLoopGroup(type)) {
            return EpollProvider.newEventLoopGroup(numberOfThreads, new DefaultThreadFactory("lettuce-epollEventLoop", true));
        }

        if (KqueueProvider.isAvailable() && KqueueProvider.isEventLoopGroup(type)) {
            return KqueueProvider.newEventLoopGroup(numberOfThreads, new DefaultThreadFactory("lettuce-kqueueEventLoop", true));
        }

        throw new IllegalArgumentException(String.format("Type %s not supported", type.getName()));
    }

    @Override
    public Promise<Boolean> release(EventExecutorGroup eventLoopGroup, long quietPeriod, long timeout, TimeUnit unit) {

        Class<?> key = getKey(release(eventLoopGroup));

        if ((key == null && eventLoopGroup.isShuttingDown()) || refCounter.containsKey(eventLoopGroup)) {
            DefaultPromise<Boolean> promise = new DefaultPromise<Boolean>(GlobalEventExecutor.INSTANCE);
            promise.setSuccess(true);
            return promise;
        }

        if (key != null) {
            eventLoopGroups.remove(key);
        }

        Future<?> shutdownFuture = eventLoopGroup.shutdownGracefully(quietPeriod, timeout, unit);
        return toBooleanPromise(shutdownFuture);
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
    @SuppressWarnings("unchecked")
    public Future<Boolean> shutdown(long quietPeriod, long timeout, TimeUnit timeUnit) {
        shutdownCalled = true;

        Map<Class<? extends EventExecutorGroup>, EventExecutorGroup> copy = new HashMap<>(eventLoopGroups);

        DefaultPromise<Boolean> overall = new DefaultPromise<Boolean>(GlobalEventExecutor.INSTANCE);
        DefaultPromise<Boolean> lastRelease = new DefaultPromise<Boolean>(GlobalEventExecutor.INSTANCE);
        Futures.PromiseAggregator<Boolean, Promise<Boolean>> aggregator = new Futures.PromiseAggregator<Boolean, Promise<Boolean>>(overall);

        aggregator.expectMore(1 + copy.size());

        aggregator.arm();

        for (EventExecutorGroup executorGroup : copy.values()) {
            Promise<Boolean> shutdown = toBooleanPromise(release(executorGroup, quietPeriod, timeout, timeUnit));
            aggregator.add(shutdown);
        }

        aggregator.add(lastRelease);
        lastRelease.setSuccess(null);

        return toBooleanPromise(overall);
    }
}
