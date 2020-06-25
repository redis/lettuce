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
 * @author Jongyeol Choi
 * @author Mark Paluch
 */
class DecorrelatedJitterDelayUnitTests {

    @Test
    void shouldNotCreateIfLowerBoundIsNegative() {
        assertThatThrownBy(() -> Delay.decorrelatedJitter(-1, 100, 0, TimeUnit.MILLISECONDS))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldNotCreateIfLowerBoundIsSameAsUpperBound() {
        assertThatThrownBy(() -> Delay.decorrelatedJitter(100, 100, 1, TimeUnit.MILLISECONDS))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void negativeAttemptShouldReturnZero() {

        Delay delay = Delay.decorrelatedJitter().get();

        assertThat(delay.createDelay(-1)).isEqualTo(Duration.ZERO);
    }

    @Test
    void zeroShouldReturnZero() {

        Delay delay = Delay.decorrelatedJitter().get();

        assertThat(delay.createDelay(0)).isEqualTo(Duration.ZERO);
    }

    @Test
    void testDefaultDelays() {

        Delay delay = Delay.decorrelatedJitter().get();

        for (int i = 0; i < 1000; i++) {
            assertThat(delay.createDelay(1).toMillis()).isBetween(0L, 1L);
            assertThat(delay.createDelay(2).toMillis()).isBetween(0L, 3L);
            assertThat(delay.createDelay(3).toMillis()).isBetween(0L, 9L);
            assertThat(delay.createDelay(4).toMillis()).isBetween(0L, 27L);
            assertThat(delay.createDelay(5).toMillis()).isBetween(0L, 81L);
            assertThat(delay.createDelay(6).toMillis()).isBetween(0L, 243L);
            assertThat(delay.createDelay(7).toMillis()).isBetween(0L, 729L);
            assertThat(delay.createDelay(8).toMillis()).isBetween(0L, 2187L);
            assertThat(delay.createDelay(9).toMillis()).isBetween(0L, 6561L);
            assertThat(delay.createDelay(10).toMillis()).isBetween(0L, 19683L);
            assertThat(delay.createDelay(11).toMillis()).isBetween(0L, 30000L);
            assertThat(delay.createDelay(Integer.MAX_VALUE).toMillis()).isBetween(0L, 30000L);
        }
    }

}
