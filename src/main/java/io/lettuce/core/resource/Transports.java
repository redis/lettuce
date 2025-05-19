package io.lettuce.core.resource;

import io.lettuce.core.internal.LettuceAssert;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * Transport infrastructure utility class. This class provides {@link EventLoopGroup} and {@link Channel} classes for TCP socket
 * and domain socket transports.
 *
 * @author Mark Paluch
 * @author Yohei Ueki
 * @since 4.4
 */
public class Transports {

    /**
     * @return the default {@link EventLoopGroup} for socket transport that is compatible with {@link #socketChannelClass()}.
     */
    public static Class<? extends EventLoopGroup> eventLoopGroupClass() {

        if (NativeTransports.isAvailable()) {
            return NativeTransports.eventLoopGroupClass();
        }

        return NioEventLoopGroup.class;
    }

    /**
     * @return the default {@link Channel} for socket (network/TCP) transport.
     */
    public static Class<? extends Channel> socketChannelClass() {

        if (NativeTransports.isAvailable()) {
            return NativeTransports.socketChannelClass();
        }

        return NioSocketChannel.class;
    }

    /**
     * @return the default {@link DatagramChannel} for socket (network/UDP) transport.
     */
    public static Class<? extends DatagramChannel> datagramChannelClass() {

        if (NativeTransports.isAvailable()) {
            return NativeTransports.datagramChannelClass();
        }

        return NioDatagramChannel.class;
    }

    /**
     * Native transport support.
     */
    public static class NativeTransports {

        static EventLoopResources RESOURCES = KqueueProvider.isAvailable() ? KqueueProvider.getResources()
                : IOUringProvider.isAvailable() ? IOUringProvider.getResources() : EpollProvider.getResources();

        /**
         * @return {@code true} if a native transport is available.
         */
        static boolean isAvailable() {
            return EpollProvider.isAvailable() || KqueueProvider.isAvailable() || IOUringProvider.isAvailable();
        }

        /**
         * @return {@code true} if a native transport for domain sockets is available.
         */
        public static boolean isDomainSocketSupported() {
            return EpollProvider.isAvailable() || KqueueProvider.isAvailable();
        }

        /**
         * @return the native transport socket {@link Channel} class.
         */
        static Class<? extends Channel> socketChannelClass() {
            return RESOURCES.socketChannelClass();
        }

        /**
         * @return the native transport socket {@link DatagramChannel} class.
         */
        static Class<? extends DatagramChannel> datagramChannelClass() {
            return RESOURCES.datagramChannelClass();
        }

        /**
         * @return the native transport domain socket {@link Channel} class.
         */
        public static Class<? extends Channel> domainSocketChannelClass() {
            assertDomainSocketAvailable();
            return EpollProvider.isAvailable() && IOUringProvider.isAvailable()
                    ? EpollProvider.getResources().domainSocketChannelClass()
                    : RESOURCES.domainSocketChannelClass();
        }

        /**
         * @return the native transport {@link EventLoopGroup} class. Defaults to TCP sockets. See
         *         {@link #eventLoopGroupClass(boolean)} to request a specific EventLoopGroup for Domain Socket usage.
         */
        public static Class<? extends EventLoopGroup> eventLoopGroupClass() {
            return eventLoopGroupClass(false);
        }

        /**
         * @return the native transport {@link EventLoopGroup} class.
         * @param domainSocket {@code true} to indicate Unix Domain Socket usage, {@code false} otherwise.
         * @since 6.3.3
         */
        public static Class<? extends EventLoopGroup> eventLoopGroupClass(boolean domainSocket) {

            return domainSocket && EpollProvider.isAvailable() && IOUringProvider.isAvailable()
                    ? EpollProvider.getResources().eventLoopGroupClass()
                    : RESOURCES.eventLoopGroupClass();
        }

        public static void assertDomainSocketAvailable() {

            LettuceAssert.assertState(NativeTransports.isDomainSocketSupported(),
                    "A unix domain socket connection requires epoll or kqueue and neither is available");
        }

    }

}
