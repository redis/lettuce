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
