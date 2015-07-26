package com.lambdaworks.redis;

import java.lang.reflect.Constructor;
import java.net.SocketAddress;

import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Wraps and provides Epoll classes. This is to protect the user from {@link ClassNotFoundException}'s caused by the absence of
 * the {@literal netty-transport-native-epoll} library during runtime.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
class EpollProvider {

    protected static final InternalLogger logger = InternalLoggerFactory.getInstance(EpollProvider.class);

    public final static Class<EventLoopGroup> epollEventLoopGroupClass;
    public final static Class<Channel> epollDomainSocketChannelClass;
    public final static Class<SocketAddress> domainSocketAddressClass;
    static {

        epollEventLoopGroupClass = getClass("io.netty.channel.epoll.EpollEventLoopGroup");
        epollDomainSocketChannelClass = getClass("io.netty.channel.epoll.EpollDomainSocketChannel");
        domainSocketAddressClass = getClass("io.netty.channel.unix.DomainSocketAddress");
        if (epollDomainSocketChannelClass == null || epollEventLoopGroupClass == null) {
            logger.debug("Starting without optional Epoll library");
        }
    }

    /**
     * Try to load class {@literal className}.
     * 
     * @param className
     * @param <T> Expected return type for casting.
     * @return instance of {@literal className} or null
     */
    @SuppressWarnings("unchecked")
    private static <T> Class<T> getClass(String className) {
        try {
            return (Class) JavaRuntime.forName(className);
        } catch (ClassNotFoundException e) {
            logger.debug("Cannot load class " + className, e);
        }
        return null;
    }

    /**
     * Check whether the Epoll library is available on the class path.
     * 
     * @throws IllegalStateException if the {@literal netty-transport-native-epoll} library is not available
     * 
     */
    static void checkForEpollLibrary() {

        if (domainSocketAddressClass == null || epollDomainSocketChannelClass == null) {
            throw new IllegalStateException(
                    "Cannot connect using sockets without the optional netty-transport-native-epoll library on the class path");
        }
    }

    static SocketAddress newSocketAddress(String socketPath) {

        try {
            Constructor<SocketAddress> constructor = domainSocketAddressClass.getConstructor(String.class);
            return constructor.newInstance(socketPath);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static EventLoopGroup newEventLoopGroup(int nThreads) {

        try {
            Constructor<EventLoopGroup> constructor = epollEventLoopGroupClass.getConstructor(Integer.TYPE);
            return constructor.newInstance(nThreads);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
