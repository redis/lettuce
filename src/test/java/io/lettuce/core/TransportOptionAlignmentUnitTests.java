/*
 * Copyright 2011-Present, Redis Ltd. and Contributors
 * All rights reserved.
 *
 * Licensed under the MIT License.
 *
 * This file contains contributions from third-party contributors
 * licensed under the Apache License, Version 2.0 (the "License");
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
package io.lettuce.core;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.time.Duration;
import java.util.stream.Stream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.verification.VerificationMode;

import io.lettuce.core.resource.EpollProvider;
import io.lettuce.core.resource.ExtendedKeepAliveSupport;
import io.lettuce.core.resource.IOUringProvider;
import io.netty.bootstrap.Bootstrap;

/**
 * Regression coverage guaranteeing that native-transport socket options are applied by the <em>same</em> transport that builds
 * the channel.
 *
 * <p>
 * Lettuce selects the channel with the priority {@code Epoll > Kqueue > IOUring} (see {@code Transports.NativeTransports}).
 * When both epoll and io_uring are on the classpath it therefore builds an <em>epoll</em> channel, so keep-alive and
 * {@code TCP_USER_TIMEOUT} options must be applied by epoll - applying io_uring options to an epoll channel is silently
 * rejected by Netty ("Unknown channel option ...") and leaves the option unconfigured (see
 * <a href="https://github.com/redis/lettuce/issues/3816">#3816</a>).
 *
 * <p>
 * Transport availability is forced with static mocks, so the "both available" branch is exercised deterministically on every
 * platform without requiring a real io_uring-capable kernel (which most CI runners lack).
 */
@Tag(UNIT_TEST)
class TransportOptionAlignmentUnitTests {

    private enum Transport {
        EPOLL, IOURING
    }

    private static final int COUNT = 3;

    private static final Duration IDLE = Duration.ofSeconds(13);

    private static final Duration INTERVAL = Duration.ofSeconds(13);

    private static final Duration USER_TIMEOUT = Duration.ofSeconds(30);

    static Stream<Arguments> nativeTransportCombinations() {
        return Stream.of(Arguments.of("epoll and io_uring both available", true, true, Transport.EPOLL),
                Arguments.of("epoll only", true, false, Transport.EPOLL),
                Arguments.of("io_uring only", false, true, Transport.IOURING));
    }

    @ParameterizedTest(name = "keep-alive: {0} -> applied by {3}")
    @MethodSource("nativeTransportCombinations")
    void keepAliveAppliedByChannelTransport(String name, boolean epollAvailable, boolean ioUringAvailable, Transport expected) {

        Bootstrap bootstrap = new Bootstrap();

        try (MockedStatic<EpollProvider> epoll = mockStatic(EpollProvider.class);
                MockedStatic<IOUringProvider> ioUring = mockStatic(IOUringProvider.class)) {

            epoll.when(EpollProvider::isAvailable).thenReturn(epollAvailable);
            ioUring.when(IOUringProvider::isAvailable).thenReturn(ioUringAvailable);

            boolean applied = ExtendedKeepAliveSupport.applyKeepAlive(bootstrap, COUNT, IDLE, INTERVAL);

            assertThat(applied).isTrue();
            epoll.verify(() -> EpollProvider.applyKeepAlive(bootstrap, COUNT, IDLE, INTERVAL),
                    invoked(expected == Transport.EPOLL));
            ioUring.verify(() -> IOUringProvider.applyKeepAlive(any(), anyInt(), any(), any()),
                    invoked(expected == Transport.IOURING));
        }
    }

    @ParameterizedTest(name = "TCP_USER_TIMEOUT: {0} -> applied by {3}")
    @MethodSource("nativeTransportCombinations")
    void tcpUserTimeoutAppliedByChannelTransport(String name, boolean epollAvailable, boolean ioUringAvailable,
            Transport expected) {

        Bootstrap bootstrap = new Bootstrap();

        try (MockedStatic<EpollProvider> epoll = mockStatic(EpollProvider.class);
                MockedStatic<IOUringProvider> ioUring = mockStatic(IOUringProvider.class)) {

            epoll.when(EpollProvider::isAvailable).thenReturn(epollAvailable);
            ioUring.when(IOUringProvider::isAvailable).thenReturn(ioUringAvailable);

            boolean applied = ConnectionBuilder.applyTcpUserTimeout(bootstrap, USER_TIMEOUT);

            assertThat(applied).isTrue();
            epoll.verify(() -> EpollProvider.applyTcpUserTimeout(bootstrap, USER_TIMEOUT),
                    invoked(expected == Transport.EPOLL));
            ioUring.verify(() -> IOUringProvider.applyTcpUserTimeout(any(), any()), invoked(expected == Transport.IOURING));
        }
    }

    private static VerificationMode invoked(boolean expected) {
        return expected ? times(1) : never();
    }

}
