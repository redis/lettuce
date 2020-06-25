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

import java.util.concurrent.TimeUnit;

import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.concurrent.Future;

/**
 * Provider for {@link EventLoopGroup EventLoopGroups and EventExecutorGroups}. A event loop group is a heavy-weight instance
 * holding and providing {@link Thread} instances. Multiple instances can be created but are expensive. Keeping too many
 * instances open can exhaust the number of open files.
 * <p>
 * Usually, the default settings are sufficient. However, customizing might be useful for some special cases where multiple
 * {@link io.lettuce.core.RedisClient} or {@link io.lettuce.core.cluster.RedisClusterClient} instances are needed that share one
 * or more event loop groups.
 * </p>
 * <p>
 * The {@link EventLoopGroupProvider} allows to allocate and release instances implementing {@link EventExecutorGroup}. The
 * {@link EventExecutorGroup} instances must not be terminated or shutdown by the user. Resources are managed by the particular
 * {@link EventLoopGroupProvider}.
 * </p>
 * You can implement your own {@link EventLoopGroupProvider} to share existing {@link EventLoopGroup EventLoopGroup's} with
 * lettuce.
 *
 * @author Mark Paluch
 * @since 3.4
 */
public interface EventLoopGroupProvider {

    /**
     * Retrieve a {@link EventLoopGroup} for the {@link EventLoopGroup channel} {@code type}. Do not terminate or shutdown the
     * instance. Call the {@link #release(EventExecutorGroup, long, long, TimeUnit)} to release an individual instance or
     * {@link #shutdown(long, long, TimeUnit)} method to free the all resources.
     *
     * @param type class of the {@link EventLoopGroup}, must not be {@code null}.
     * @param <T> type of the {@link EventLoopGroup}.
     * @return the {@link EventLoopGroup}.
     */
    <T extends EventLoopGroup> T allocate(Class<T> type);

    /**
     * Returns the pool size (number of threads) for IO threads. The indicated size does not reflect the number for all IO
     * threads, it is the number of threads that are used to create a particular thread pool.
     *
     * @return the pool size (number of threads) for all IO tasks.
     */
    int threadPoolSize();

    /**
     * Release a {@code eventLoopGroup} instance. The method will shutdown/terminate the {@link EventExecutorGroup} if it is no
     * longer needed.
     *
     * @param eventLoopGroup the eventLoopGroup instance, must not be {@code null}.
     * @param quietPeriod the quiet period.
     * @param timeout the timeout.
     * @param unit time unit for the quiet period/the timeout.
     * @return a close future to synchronize the called for shutting down.
     */
    Future<Boolean> release(EventExecutorGroup eventLoopGroup, long quietPeriod, long timeout, TimeUnit unit);

    /**
     * Shutdown the provider and release all instances.
     *
     * @param quietPeriod the quiet period.
     * @param timeout the timeout.
     * @param timeUnit the unit of {@code quietPeriod} and {@code timeout}.
     * @return a close future to synchronize the called for shutting down.
     */
    Future<Boolean> shutdown(long quietPeriod, long timeout, TimeUnit timeUnit);

}
