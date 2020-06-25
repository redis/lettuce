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

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

/**
 * @author Mark Paluch
 */
class ExponentialDelayUnitTests {

    @Test
    void shouldNotCreateIfLowerBoundIsNegative() {
        assertThatThrownBy(() -> Delay.exponential(-1, 100, TimeUnit.MILLISECONDS, 10))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldNotCreateIfLowerBoundIsSameAsUpperBound() {
        assertThatThrownBy(() -> Delay.exponential(100, 100, TimeUnit.MILLISECONDS, 10))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldNotCreateIfPowerIsOne() {
        assertThatThrownBy(() -> Delay.exponential(100, 1000, TimeUnit.MILLISECONDS, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void negativeAttemptShouldReturnZero() {

        Delay delay = Delay.exponential();

        assertThat(delay.createDelay(-1).toMillis()).isEqualTo(0);
    }

    @Test
    void zeroShouldReturnZero() {

        Delay delay = Delay.exponential();

        assertThat(delay.createDelay(0).toMillis()).isEqualTo(0);
    }

    @Test
    void testDefaultDelays() {

        Delay delay = Delay.exponential();

        assertThat(delay.createDelay(1).toMillis()).isEqualTo(1);
        assertThat(delay.createDelay(2).toMillis()).isEqualTo(2);
        assertThat(delay.createDelay(3).toMillis()).isEqualTo(4);
        assertThat(delay.createDelay(4).toMillis()).isEqualTo(8);
        assertThat(delay.createDelay(5).toMillis()).isEqualTo(16);
        assertThat(delay.createDelay(6).toMillis()).isEqualTo(32);
        assertThat(delay.createDelay(7).toMillis()).isEqualTo(64);
        assertThat(delay.createDelay(8).toMillis()).isEqualTo(128);
        assertThat(delay.createDelay(9).toMillis()).isEqualTo(256);
        assertThat(delay.createDelay(10).toMillis()).isEqualTo(512);
        assertThat(delay.createDelay(11).toMillis()).isEqualTo(1024);
        assertThat(delay.createDelay(12).toMillis()).isEqualTo(2048);
        assertThat(delay.createDelay(13).toMillis()).isEqualTo(4096);
        assertThat(delay.createDelay(14).toMillis()).isEqualTo(8192);
        assertThat(delay.createDelay(15).toMillis()).isEqualTo(16384);
        assertThat(delay.createDelay(16).toMillis()).isEqualTo(30000);
        assertThat(delay.createDelay(17).toMillis()).isEqualTo(30000);
        assertThat(delay.createDelay(Integer.MAX_VALUE).toMillis()).isEqualTo(30000);
    }

    @Test
    void testPow10Delays() {

        Delay delay = Delay.exponential(100, 10000, TimeUnit.MILLISECONDS, 10);

        assertThat(delay.createDelay(1).toMillis()).isEqualTo(100);
        assertThat(delay.createDelay(2).toMillis()).isEqualTo(100);
        assertThat(delay.createDelay(3).toMillis()).isEqualTo(100);
        assertThat(delay.createDelay(4).toMillis()).isEqualTo(1000);
        assertThat(delay.createDelay(5).toMillis()).isEqualTo(10000);
        assertThat(delay.createDelay(Integer.MAX_VALUE).toMillis()).isEqualTo(10000);
    }

}
