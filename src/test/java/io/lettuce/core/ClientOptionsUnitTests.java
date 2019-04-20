/*
 * Copyright 2018-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.lettuce.core.protocol.ProtocolVersion;

/**
 * @author Mark Paluch
 */
class ClientOptionsUnitTests {

    @Test
    void testNew() {
        checkAssertions(ClientOptions.create());
    }

    @Test
    void testBuilder() {
        checkAssertions(ClientOptions.builder().build());
    }

    @Test
    void testCopy() {

        ClientOptions original = ClientOptions.builder().build();
        ClientOptions copy = ClientOptions.copyOf(original);

        checkAssertions(copy);

        assertThat(original.mutate()).isNotSameAs(copy.mutate());
    }

    void checkAssertions(ClientOptions sut) {
        assertThat(sut.isAutoReconnect()).isEqualTo(true);
        assertThat(sut.isCancelCommandsOnReconnectFailure()).isEqualTo(false);
        assertThat(sut.getProtocolVersion()).isEqualTo(ProtocolVersion.RESP3);
        assertThat(sut.isSuspendReconnectOnProtocolFailure()).isEqualTo(false);
        assertThat(sut.getDisconnectedBehavior()).isEqualTo(ClientOptions.DisconnectedBehavior.DEFAULT);
        assertThat(sut.getBufferUsageRatio()).isEqualTo(ClientOptions.DEFAULT_BUFFER_USAGE_RATIO);
    }
}
