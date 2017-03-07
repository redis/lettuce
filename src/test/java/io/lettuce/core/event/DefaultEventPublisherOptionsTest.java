/*
 * Copyright 2011-2016 the original author or authors.
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
package io.lettuce.core.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

/**
 * @author Mark Paluch
 */
public class DefaultEventPublisherOptionsTest {

    @Test
    public void testDefault() throws Exception {

        DefaultEventPublisherOptions sut = DefaultEventPublisherOptions.create();

        assertThat(sut.eventEmitInterval()).isEqualTo(10);
        assertThat(sut.eventEmitIntervalUnit()).isEqualTo(TimeUnit.MINUTES);
    }

    @Test
    public void testDisabled() throws Exception {

        DefaultEventPublisherOptions sut = DefaultEventPublisherOptions.disabled();

        assertThat(sut.eventEmitInterval()).isEqualTo(0);
        assertThat(sut.eventEmitIntervalUnit()).isEqualTo(TimeUnit.SECONDS);
    }

    @Test
    public void testBuilder() throws Exception {

        DefaultEventPublisherOptions sut = DefaultEventPublisherOptions.builder().eventEmitInterval(1, TimeUnit.SECONDS)
                .build();

        assertThat(sut.eventEmitInterval()).isEqualTo(1);
        assertThat(sut.eventEmitIntervalUnit()).isEqualTo(TimeUnit.SECONDS);
    }
}
