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

import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.util.concurrent.EventExecutorGroup;

/**
 * Interface to encapsulate EventLoopGroup resources.
 *
 * @author Mark Paluch
 * @since 6.0
 */
public interface EventLoopResources {

    /**
     * Checks if the given {@code type} matches the underlying {@link EventExecutorGroup} type.
     *
     * @param type must not be {@code null}.
     * @return {@code true} if {@code type} is a {@link EventExecutorGroup} of the underlying loop resources.
     */
    boolean matches(Class<? extends EventExecutorGroup> type);

    /**
     * Create a new {@link EpollEventLoopGroup}.
     *
     * @param nThreads number of threads.
     * @param threadFactory the {@link ThreadFactory}.
     * @return the {@link EventLoopGroup}.
     */
    EventLoopGroup newEventLoopGroup(int nThreads, ThreadFactory threadFactory);

    /**
     * @return the Domain Socket {@link Channel} class.
     */
    Class<? extends Channel> domainSocketChannelClass();

    /**
     * @return the {@link Channel} class.
     */
    Class<? extends Channel> socketChannelClass();

    /**
     * @return the {@link EventLoopGroup} class.
     */
    Class<? extends EventLoopGroup> eventLoopGroupClass();

    /**
     * @param socketPath the socket file path.
     * @return a domain socket address object.
     */
    SocketAddress newSocketAddress(String socketPath);

}
