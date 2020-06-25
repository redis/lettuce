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
package io.lettuce.core.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

/**
 * @author Mark Paluch
 */
class ConstantDelayUnitTests {

    @Test
    void shouldNotCreateIfDelayIsNegative() {
        assertThatThrownBy(() -> Delay.constant(-1, TimeUnit.MILLISECONDS)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldCreateZeroDelay() {

        Delay delay = Delay.constant(0, TimeUnit.MILLISECONDS);

        assertThat(delay.createDelay(0)).isEqualTo(Duration.ZERO);
        assertThat(delay.createDelay(5)).isEqualTo(Duration.ZERO);
    }

    @Test
    void shouldCreateConstantDelay() {

        Delay delay = Delay.constant(100, TimeUnit.MILLISECONDS);

        assertThat(delay.createDelay(0)).isEqualTo(Duration.ofMillis(100));
        assertThat(delay.createDelay(5)).isEqualTo(Duration.ofMillis(100));
    }

}
