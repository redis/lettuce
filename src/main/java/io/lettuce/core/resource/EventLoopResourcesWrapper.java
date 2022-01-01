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
 * Wrapper for {@link EventLoopResources} that applies a {@link Runnable verification} before calling the delegate method.
 *
 * @author Mark Paluch
 */
class EventLoopResourcesWrapper implements EventLoopResources {

    private final EventLoopResources delegate;

    private final Runnable verifier;

    EventLoopResourcesWrapper(EventLoopResources delegate, Runnable verifier) {
        this.delegate = delegate;
        this.verifier = verifier;
    }

    @Override
    public boolean matches(Class<? extends EventExecutorGroup> type) {
        verifier.run();
        return delegate.matches(type);
    }

    @Override
    public Class<? extends EventLoopGroup> eventLoopGroupClass() {
        verifier.run();
        return delegate.eventLoopGroupClass();
    }

    @Override
    public EventLoopGroup newEventLoopGroup(int nThreads, ThreadFactory threadFactory) {
        verifier.run();
        return delegate.newEventLoopGroup(nThreads, threadFactory);
    }

    @Override
    public Class<? extends Channel> socketChannelClass() {
        verifier.run();
        return delegate.socketChannelClass();
    }

    @Override
    public Class<? extends Channel> domainSocketChannelClass() {
        verifier.run();
        return delegate.domainSocketChannelClass();
    }

    @Override
    public Class<? extends DatagramChannel> datagramChannelClass() {
        verifier.run();
        return delegate.datagramChannelClass();
    }

    @Override
    public SocketAddress newSocketAddress(String socketPath) {
        verifier.run();
        return delegate.newSocketAddress(socketPath);
    }

}
