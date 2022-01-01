/*
 * Copyright 2020-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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

import static org.assertj.core.api.Assertions.*;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import io.lettuce.core.SocketOptions.KeepAliveOptions;

/**
 * Unit tests for {@link KeepAliveOptions}.
 *
 * @author Mark Paluch
 */
class KeepAliveOptionsUnitTests {

    @Test
    void testNew() {
        KeepAliveOptions disabled = KeepAliveOptions.builder().build();

        assertThat(disabled).isNotNull();
        assertThat(disabled.getIdle()).hasHours(2);
        assertThat(disabled.getInterval()).hasSeconds(75);
        assertThat(disabled.getCount()).isEqualTo(9);
    }

    @Test
    void testBuilder() {

        KeepAliveOptions sut = KeepAliveOptions.builder().enable().count(5).idle(Duration.ofSeconds(20))
                .interval(Duration.ofSeconds(40)).build();

        assertThat(sut.isEnabled()).isTrue();
        assertThat(sut.getIdle()).hasSeconds(20);
        assertThat(sut.getInterval()).hasSeconds(40);
    }

    @Test
    void mutateShouldConfigureNewOptions() {

        KeepAliveOptions sut = KeepAliveOptions.builder().enable().count(5).idle(Duration.ofSeconds(20))
                .interval(Duration.ofSeconds(40)).build();

        KeepAliveOptions reconfigured = sut.mutate().count(7).build();

        assertThat(sut.getCount()).isEqualTo(5);
        assertThat(reconfigured.getCount()).isEqualTo(7);
    }

}
