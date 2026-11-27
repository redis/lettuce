/*
 * Copyright 2018-Present, Redis Ltd. and Contributors
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
import static org.assertj.core.api.Assertions.*;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import io.lettuce.core.SocketOptions.TcpUserTimeoutOptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SocketOptions}.
 *
 * @author Mark Paluch
 */
@Tag(UNIT_TEST)
class SocketOptionsUnitTests {

    @Test
    void testNew() {
        checkAssertions(SocketOptions.create());
    }

    @Test
    void testBuilder() {

        SocketOptions sut = SocketOptions.builder().connectTimeout(1, TimeUnit.MINUTES).keepAlive(true).tcpNoDelay(false)
                .build();

        assertThat(sut.isKeepAlive()).isTrue();
        assertThat(sut.isTcpNoDelay()).isFalse();
        assertThat(sut.getConnectTimeout()).isEqualTo(Duration.ofMinutes(1));
    }

    @Test
    void mutateShouldConfigureNewOptions() {

        SocketOptions sut = SocketOptions.builder().connectTimeout(Duration.ofSeconds(1)).keepAlive(true).tcpNoDelay(true)
                .build();

        SocketOptions reconfigured = sut.mutate().tcpNoDelay(false).build();

        assertThat(sut.isKeepAlive()).isTrue();
        assertThat(sut.isTcpNoDelay()).isTrue();
        assertThat(sut.getConnectTimeout()).isEqualTo(Duration.ofSeconds(1));

        assertThat(reconfigured.isTcpNoDelay()).isFalse();
    }

    @Test
    void shouldConfigureSimpleKeepAlive() {

        SocketOptions sut = SocketOptions.builder().keepAlive(true).build();

        assertThat(sut.isKeepAlive()).isTrue();
        assertThat(sut.isExtendedKeepAlive()).isFalse();
    }

    @Test
    void shouldConfigureSimpleExtendedAlive() {

        SocketOptions sut = SocketOptions.builder().keepAlive(SocketOptions.KeepAliveOptions.builder().build()).build();

        assertThat(sut.isKeepAlive()).isTrue();
        assertThat(sut.isExtendedKeepAlive()).isTrue();
    }

    @Test
    void testCopy() {
        checkAssertions(SocketOptions.copyOf(SocketOptions.builder().build()));
    }

    void checkAssertions(SocketOptions sut) {
        assertThat(sut.isKeepAlive()).isTrue();
        assertThat(sut.isTcpNoDelay()).isTrue();
        assertThat(sut.getConnectTimeout()).isEqualTo(Duration.ofSeconds(10));
    }

    @Test
    void testDefaultTcpUserTimeoutOption() {
        SocketOptions sut = SocketOptions.builder().build();
        assertThat(sut.isEnableTcpUserTimeout()).isFalse();
    }

    @Test
    void testConfigTcpUserTimeoutOption() {
        SocketOptions sut = SocketOptions.builder()
                .tcpUserTimeout(TcpUserTimeoutOptions.builder().enable().tcpUserTimeout(Duration.ofSeconds(60)).build())
                .build();
        assertThat(sut.isEnableTcpUserTimeout()).isTrue();
        assertThat(sut.getTcpUserTimeout().getTcpUserTimeout()).isEqualTo(Duration.ofSeconds(60));
    }

}
