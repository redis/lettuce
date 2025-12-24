package io.lettuce.core.resource;

import io.lettuce.core.internal.LettuceAssert;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

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
     * @deprecated since 7.3.0, use {@link #eventLoopResources()} to obtain {@link EventLoopResources} and call
     *             {@link EventLoopResources#eventLoopGroupClass()}. This method will be removed in 8.0.
     */
    @Deprecated
    public static Class<? extends EventLoopGroup> eventLoopGroupClass() {
        return eventLoopResources().eventLoopGroupClass();
    }

    /**
     * @return the default {@link Channel} for socket (network/TCP) transport.
     * @deprecated since 7.3.0, use {@link #eventLoopResources()} to obtain {@link EventLoopResources} and call
     *             {@link EventLoopResources#socketChannelClass()}. This method will be removed in 8.0.
     */
    @Deprecated
    public static Class<? extends Channel> socketChannelClass() {
        return eventLoopResources().socketChannelClass();
    }

    /**
     * @return the default {@link DatagramChannel} for socket (network/UDP) transport.
     * @deprecated since 7.3.0, use {@link #eventLoopResources()} to obtain {@link EventLoopResources} and call
     *             {@link EventLoopResources#datagramChannelClass()}. This method will be removed in 8.0.
     */
    @Deprecated
    public static Class<? extends DatagramChannel> datagramChannelClass() {
        return eventLoopResources().datagramChannelClass();
    }

    /**
     * Returns the best available {@link EventLoopResources} based on the runtime environment. This method selects native
     * transports (Epoll, Kqueue, IOUring) when available, falling back to NIO otherwise.
     * <p>
     * Priority order: Epoll &gt; Kqueue &gt; IOUring &gt; NIO
     * <p>
     * The returned {@link EventLoopResources} automatically handles domain socket support. When both Epoll and IOUring are
     * available, Epoll is selected (as it supports domain sockets while IOUring does not).
     *
     * @return the best available {@link EventLoopResources}
     * @since 7.3.0
     */
    public static EventLoopResources eventLoopResources() {

        if (NativeTransports.isAvailable()) {
            return NativeTransports.RESOURCES;
        }

        return NioProvider.getResources();
    }

    /**
     * Native transport support.
     */
    public static class NativeTransports {

        private static final InternalLogger transportsLogger = InternalLoggerFactory.getInstance(Transports.class);

        static EventLoopResources RESOURCES = EpollProvider.isAvailable() ? EpollProvider.getResources()
                : KqueueProvider.isAvailable() ? KqueueProvider.getResources() : IOUringProvider.getResources();

        /**
         * @return {@code true} if a native transport is available.
         */
        static boolean isAvailable() {
            if (EpollProvider.isAvailable() && IOUringProvider.isAvailable()) {
                transportsLogger.warn("Both epoll and io_uring native transports are available, epoll has been prioritized.");
            }
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
         * @deprecated since 7.3.0, use {@link Transports#eventLoopResources()} to obtain {@link EventLoopResources} and call
         *             {@link EventLoopResources#socketChannelClass()}. This method will be removed in 8.0.
         */
        @Deprecated
        static Class<? extends Channel> socketChannelClass() {
            return RESOURCES.socketChannelClass();
        }

        /**
         * @return the native transport socket {@link DatagramChannel} class.
         * @deprecated since 7.3.0, use {@link Transports#eventLoopResources()} to obtain {@link EventLoopResources} and call
         *             {@link EventLoopResources#datagramChannelClass()}. This method will be removed in 8.0.
         */
        @Deprecated
        static Class<? extends DatagramChannel> datagramChannelClass() {
            return RESOURCES.datagramChannelClass();
        }

        /**
         * @return the native transport domain socket {@link Channel} class.
         * @deprecated since 7.3.0, use {@link Transports#eventLoopResources()} to obtain {@link EventLoopResources} and call
         *             {@link EventLoopResources#domainSocketChannelClass()}. With the corrected transport priority order (Epoll
         *             &gt; Kqueue &gt; IOUring), the returned resources automatically handle domain socket support. This method
         *             will be removed in 8.0.
         */
        @Deprecated
        public static Class<? extends Channel> domainSocketChannelClass() {
            assertDomainSocketAvailable();
            return RESOURCES.domainSocketChannelClass();
        }

        /**
         * @return the native transport {@link EventLoopGroup} class.
         * @deprecated since 7.3.0, use {@link Transports#eventLoopResources()} to obtain {@link EventLoopResources} and call
         *             {@link EventLoopResources#eventLoopGroupClass()}. This method will be removed in 8.0.
         */
        @Deprecated
        public static Class<? extends EventLoopGroup> eventLoopGroupClass() {
            return RESOURCES.eventLoopGroupClass();
        }

        /**
         * @return the native transport {@link EventLoopGroup} class.
         * @param domainSocket {@code true} to indicate Unix Domain Socket usage, {@code false} otherwise.
         * @deprecated since 7.3.0, the {@code domainSocket} parameter is no longer needed. Use
         *             {@link Transports#eventLoopResources()} to obtain {@link EventLoopResources} and call
         *             {@link EventLoopResources#eventLoopGroupClass()}. With the corrected transport priority order (Epoll &gt;
         *             Kqueue &gt; IOUring), the returned resources automatically handle domain socket support. This method will
         *             be removed in 8.0.
         * @since 6.3.3
         */
        @Deprecated
        public static Class<? extends EventLoopGroup> eventLoopGroupClass(boolean domainSocket) {
            return RESOURCES.eventLoopGroupClass();
        }

        public static void assertDomainSocketAvailable() {

            LettuceAssert.assertState(NativeTransports.isDomainSocketSupported(),
                    "A unix domain socket connection requires epoll or kqueue and neither is available");
        }

    }

}
