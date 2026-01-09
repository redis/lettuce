/*
 * Copyright 2025-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.resource;

import java.net.SocketAddress;
import java.util.concurrent.ThreadFactory;

import io.lettuce.core.internal.LettuceAssert;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.EventExecutorGroup;

/**
 * Provides NIO event loop resources using Netty 4.2 API. This provider creates NIO event loop groups using
 * {@link MultiThreadIoEventLoopGroup} with {@link NioIoHandler} instead of the deprecated {@link NioEventLoopGroup}
 * constructor, ensuring compatibility with Netty 4.2's new IO model.
 *
 * @author Julien Ruaux
 * @since 7.2.2
 */
class NioProvider {

    /**
     * Returns the {@link EventLoopResources} for NIO-backed transport.
     *
     * @return the {@link EventLoopResources}.
     */
    public static EventLoopResources getResources() {
        return NioResources.INSTANCE;
    }

    /**
     * {@link EventLoopResources} for NIO using Netty 4.2 API.
     */
    enum NioResources implements EventLoopResources {

        INSTANCE;

        @Override
        public boolean matches(Class<? extends EventExecutorGroup> type) {

            LettuceAssert.notNull(type, "EventLoopGroup type must not be null");

            // Support both old deprecated NioEventLoopGroup and new MultiThreadIoEventLoopGroup
            return type.equals(NioEventLoopGroup.class) || type.equals(MultiThreadIoEventLoopGroup.class);
        }

        @Override
        public Class<? extends EventLoopGroup> eventLoopGroupClass() {
            // Return the new recommended class, but keep backward compatibility
            return MultiThreadIoEventLoopGroup.class;
        }

        @Override
        public EventLoopGroup newEventLoopGroup(int nThreads, ThreadFactory threadFactory) {
            // Use the new Netty 4.2 approach with IoHandlerFactory
            return new MultiThreadIoEventLoopGroup(nThreads, threadFactory, NioIoHandler.newFactory());
        }

        @Override
        public Class<? extends Channel> socketChannelClass() {
            return NioSocketChannel.class;
        }

        @Override
        public Class<? extends Channel> domainSocketChannelClass() {
            throw new UnsupportedOperationException("Domain sockets are not supported with NIO transport");
        }

        @Override
        public Class<? extends DatagramChannel> datagramChannelClass() {
            return NioDatagramChannel.class;
        }

        @Override
        public SocketAddress newSocketAddress(String socketPath) {
            throw new UnsupportedOperationException("Domain sockets are not supported with NIO transport");
        }

        @Override
        public String threadNamePrefix() {
            return "lettuce-nioEventLoop";
        }

    }

}
