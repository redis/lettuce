/*
 * Copyright 2024, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.resource;

import java.net.SocketOption;
import java.time.Duration;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.socket.nio.NioChannelOption;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Utility class to determine if extended TCP keep-alive options are supported on the current platform and to apply them.
 * <p>
 * Extended keep-alive options (TCP_KEEPIDLE, TCP_KEEPINTVL, TCP_KEEPCNT) are supported on:
 * <ul>
 * <li>Linux with io_uring transport</li>
 * <li>Linux with epoll transport</li>
 * <li>NIO transport with Java 11+ (except on macOS/kqueue)</li>
 * </ul>
 * <p>
 * macOS (kqueue) does not support per-socket extended keep-alive options.
 * <p>
 * When {@code jdk.net.ExtendedSocketOptions} is not present (for example a jlink runtime image without the {@code jdk.net}
 * module), NIO extended keep-alive is treated as unavailable and callers fall back to plain {@code SO_KEEPALIVE} instead of
 * failing class initialization.
 *
 * @author Aleksandar Todorov
 * @since 7.5
 */
public class ExtendedKeepAliveSupport {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ExtendedKeepAliveSupport.class);

    private static final boolean EXTENDED_KEEPALIVE_SUPPORTED;

    static {
        // Extended keep-alive is supported if:
        // 1. epoll is available (Linux), OR
        // 2. io_uring is available (Linux), OR
        // 3. NIO extended options are available AND kqueue is NOT available (not macOS)
        //
        // Probe must never throw: restricted/custom runtimes may lack jdk.net (see #3862).
        boolean supported = false;
        try {
            supported = EpollProvider.isAvailable() || IOUringProvider.isAvailable()
                    || (ExtendedNioSocketOptions.isAvailable() && !KqueueProvider.isAvailable());
        } catch (Throwable e) {
            logger.debug("Extended keep-alive support detection failed; falling back to plain SO_KEEPALIVE", e);
        }
        EXTENDED_KEEPALIVE_SUPPORTED = supported;
    }

    /**
     * Returns whether extended TCP keep-alive options are supported on the current platform.
     *
     * @return {@code true} if extended keep-alive options can be applied, {@code false} otherwise
     */
    public static boolean isSupported() {
        return EXTENDED_KEEPALIVE_SUPPORTED;
    }

    /**
     * Apply extended keep-alive options to the bootstrap. This method delegates to the appropriate provider based on platform
     * availability.
     *
     * @param bootstrap the Netty bootstrap to configure
     * @param count the maximum number of keepalive probes TCP should send before dropping the connection
     * @param idle the time the connection needs to remain idle before TCP starts sending keepalive probes
     * @param interval the time between individual keepalive probes
     * @return {@code true} if the options were applied, {@code false} otherwise
     */
    public static boolean applyKeepAlive(Bootstrap bootstrap, int count, Duration idle, Duration interval) {

        // Order must match the native transport priority used to build the channel/event loop
        // (see Transports.NativeTransports: Epoll > Kqueue > IOUring), otherwise keep-alive
        // options for the wrong transport get applied to the bootstrap.
        if (EpollProvider.isAvailable()) {
            EpollProvider.applyKeepAlive(bootstrap, count, idle, interval);
            return true;
        }

        if (IOUringProvider.isAvailable()) {
            IOUringProvider.applyKeepAlive(bootstrap, count, idle, interval);
            return true;
        }

        if (ExtendedNioSocketOptions.isAvailable() && !KqueueProvider.isAvailable()) {
            ExtendedNioSocketOptions.applyKeepAlive(bootstrap, count, idle, interval);
            return true;
        }

        return false;
    }

    private ExtendedKeepAliveSupport() {
        // Utility class
    }

    /**
     * Utility to support Java 11 {@code jdk.net.ExtendedSocketOptions} extended keepalive options.
     */
    @SuppressWarnings("unchecked")
    static class ExtendedNioSocketOptions {

        private static final SocketOption<Integer> TCP_KEEPCOUNT;

        private static final SocketOption<Integer> TCP_KEEPIDLE;

        private static final SocketOption<Integer> TCP_KEEPINTERVAL;

        static {

            SocketOption<Integer>[] resolved = resolveOptions(ExtendedNioSocketOptions.class.getClassLoader());
            TCP_KEEPCOUNT = resolved[0];
            TCP_KEEPIDLE = resolved[1];
            TCP_KEEPINTERVAL = resolved[2];
        }

        /**
         * Resolve TCP keep-alive {@link SocketOption} constants from {@code jdk.net.ExtendedSocketOptions}.
         * <p>
         * Uses reflective lookup (no hard dependency on the {@code jdk.net} module) so restricted runtimes without that module
         * degrade gracefully.
         *
         * @param classLoader class loader used to load {@code jdk.net.ExtendedSocketOptions}
         * @return array of {@code [TCP_KEEPCOUNT, TCP_KEEPIDLE, TCP_KEEPINTERVAL]}, elements may be {@code null}
         */
        static SocketOption<Integer>[] resolveOptions(ClassLoader classLoader) {

            SocketOption<Integer> keepCount = null;
            SocketOption<Integer> keepIdle = null;
            SocketOption<Integer> keepInterval = null;
            try {

                Class<?> extendedSocketOptions = Class.forName("jdk.net.ExtendedSocketOptions", true, classLoader);
                keepCount = (SocketOption<Integer>) extendedSocketOptions.getDeclaredField("TCP_KEEPCOUNT").get(null);
                keepIdle = (SocketOption<Integer>) extendedSocketOptions.getDeclaredField("TCP_KEEPIDLE").get(null);
                keepInterval = (SocketOption<Integer>) extendedSocketOptions.getDeclaredField("TCP_KEEPINTERVAL").get(null);
            } catch (ReflectiveOperationException | NoClassDefFoundError | UnsupportedOperationException e) {
                // ReflectiveOperationException covers ClassNotFoundException / NoSuchFieldException / etc.
                // NoClassDefFoundError covers restricted runtimes that lack the jdk.net module (#3862).
                logger.trace("Cannot extract ExtendedSocketOptions for KeepAlive", e);
            }

            return new SocketOption[] { keepCount, keepIdle, keepInterval };
        }

        static boolean isAvailable() {
            return TCP_KEEPCOUNT != null && TCP_KEEPIDLE != null && TCP_KEEPINTERVAL != null;
        }

        /**
         * Apply Keep-Alive options.
         */
        static void applyKeepAlive(Bootstrap bootstrap, int count, Duration idle, Duration interval) {

            bootstrap.option(NioChannelOption.of(TCP_KEEPCOUNT), count);
            bootstrap.option(NioChannelOption.of(TCP_KEEPIDLE), Math.toIntExact(idle.getSeconds()));
            bootstrap.option(NioChannelOption.of(TCP_KEEPINTERVAL), Math.toIntExact(interval.getSeconds()));
        }

    }

}
