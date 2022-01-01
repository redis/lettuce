/*
 * Copyright 2020-2022 the original author or authors.
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
import io.netty.channel.socket.DatagramChannel;
import io.netty.util.concurrent.EventExecutorGroup;

/**
 * Unavailable {@link EventLoopResources}.
 *
 * @author Mark Paluch
 */
enum UnavailableResources implements EventLoopResources {

    INSTANCE;

    @Override
    public boolean matches(Class<? extends EventExecutorGroup> type) {
        return false;
    }

    @Override
    public Class<? extends EventLoopGroup> eventLoopGroupClass() {
        return null;
    }

    @Override
    public EventLoopGroup newEventLoopGroup(int nThreads, ThreadFactory threadFactory) {
        return null;
    }

    @Override
    public Class<? extends Channel> socketChannelClass() {

        return null;
    }

    @Override
    public Class<? extends Channel> domainSocketChannelClass() {
        return null;
    }

    @Override
    public Class<? extends DatagramChannel> datagramChannelClass() {
        return null;
    }

    @Override
    public SocketAddress newSocketAddress(String socketPath) {
        return null;
    }

}
