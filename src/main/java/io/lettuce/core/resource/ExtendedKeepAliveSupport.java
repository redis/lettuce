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
import jdk.net.ExtendedSocketOptions;

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
 *
 * @author Aleksandar Todorov
 * @since 7.5
 */
public class ExtendedKeepAliveSupport {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ExtendedKeepAliveSupport.class);

    private static final boolean EXTENDED_KEEPALIVE_SUPPORTED;

    static {
        // Extended keep-alive is supported if:
        // 1. io_uring is available (Linux), OR
        // 2. epoll is available (Linux), OR
        // 3. NIO extended options are available AND kqueue is NOT available (not macOS)
        EXTENDED_KEEPALIVE_SUPPORTED = IOUringProvider.isAvailable() || EpollProvider.isAvailable()
                || (ExtendedNioSocketOptions.isAvailable() && !KqueueProvider.isAvailable());
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

        if (IOUringProvider.isAvailable()) {
            IOUringProvider.applyKeepAlive(bootstrap, count, idle, interval);
            return true;
        }

        if (EpollProvider.isAvailable()) {
            EpollProvider.applyKeepAlive(bootstrap, count, idle, interval);
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
     * Utility to support Java 11 {@link ExtendedSocketOptions extended keepalive options}.
     */
    @SuppressWarnings("unchecked")
    static class ExtendedNioSocketOptions {

        private static final SocketOption<Integer> TCP_KEEPCOUNT;

        private static final SocketOption<Integer> TCP_KEEPIDLE;

        private static final SocketOption<Integer> TCP_KEEPINTERVAL;

        static {

            SocketOption<Integer> keepCount = null;
            SocketOption<Integer> keepIdle = null;
            SocketOption<Integer> keepInterval = null;
            try {

                keepCount = (SocketOption<Integer>) ExtendedSocketOptions.class.getDeclaredField("TCP_KEEPCOUNT").get(null);
                keepIdle = (SocketOption<Integer>) ExtendedSocketOptions.class.getDeclaredField("TCP_KEEPIDLE").get(null);
                keepInterval = (SocketOption<Integer>) ExtendedSocketOptions.class.getDeclaredField("TCP_KEEPINTERVAL")
                        .get(null);
            } catch (ReflectiveOperationException e) {
                logger.trace("Cannot extract ExtendedSocketOptions for KeepAlive", e);
            }

            TCP_KEEPCOUNT = keepCount;
            TCP_KEEPIDLE = keepIdle;
            TCP_KEEPINTERVAL = keepInterval;
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
