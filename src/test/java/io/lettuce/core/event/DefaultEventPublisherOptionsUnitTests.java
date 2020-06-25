/*
 * Copyright 2011-2020 the original author or authors.
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
package io.lettuce.core.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

/**
 * @author Mark Paluch
 */
class DefaultEventPublisherOptionsUnitTests {

    @Test
    void testDefault() {

        DefaultEventPublisherOptions sut = DefaultEventPublisherOptions.create();

        assertThat(sut.eventEmitInterval()).isEqualTo(Duration.ofMinutes(10));
    }

    @Test
    void testDisabled() {

        DefaultEventPublisherOptions sut = DefaultEventPublisherOptions.disabled();

        assertThat(sut.eventEmitInterval()).isEqualTo(Duration.ZERO);
    }

    @Test
    void testBuilder() {

        DefaultEventPublisherOptions sut = DefaultEventPublisherOptions.builder().eventEmitInterval(1, TimeUnit.SECONDS)
                .build();

        assertThat(sut.eventEmitInterval()).isEqualTo(Duration.ofSeconds(1));
    }

}
