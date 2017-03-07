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
package io.lettuce.core.resource;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

/**
 * @author Jongyeol Choi
 */
public class EqualJitterDelayTest {

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotCreateIfLowerBoundIsNegative() throws Exception {
        Delay.equalJitter(-1, 100, 1, TimeUnit.MILLISECONDS);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotCreateIfLowerBoundIsSameAsUpperBound() throws Exception {
        Delay.equalJitter(100, 100, 1, TimeUnit.MILLISECONDS);
    }

    @Test
    public void negativeAttemptShouldReturnZero() throws Exception {

        Delay delay = Delay.equalJitter();

        assertThat(delay.createDelay(-1)).isEqualTo(0);
    }

    @Test
    public void zeroShouldReturnZero() throws Exception {

        Delay delay = Delay.equalJitter();

        assertThat(delay.createDelay(0)).isEqualTo(0);
    }

    @Test
    public void testDefaultDelays() throws Exception {

        Delay delay = Delay.equalJitter();

        assertThat(delay.getTimeUnit()).isEqualTo(TimeUnit.MILLISECONDS);

        assertThat(delay.createDelay(1)).isBetween(0L, 1L);
        assertThat(delay.createDelay(2)).isBetween(0L, 2L);
        assertThat(delay.createDelay(3)).isBetween(0L, 4L);
        assertThat(delay.createDelay(4)).isBetween(0L, 8L);
        assertThat(delay.createDelay(5)).isBetween(0L, 16L);
        assertThat(delay.createDelay(6)).isBetween(0L, 32L);
        assertThat(delay.createDelay(7)).isBetween(0L, 64L);
        assertThat(delay.createDelay(8)).isBetween(0L, 128L);
        assertThat(delay.createDelay(9)).isBetween(0L, 256L);
        assertThat(delay.createDelay(10)).isBetween(0L, 512L);
        assertThat(delay.createDelay(11)).isBetween(0L, 1024L);
        assertThat(delay.createDelay(12)).isBetween(0L, 2048L);
        assertThat(delay.createDelay(13)).isBetween(0L, 4096L);
        assertThat(delay.createDelay(14)).isBetween(0L, 8192L);
        assertThat(delay.createDelay(15)).isBetween(0L, 16384L);
        assertThat(delay.createDelay(16)).isBetween(0L, 30000L);
        assertThat(delay.createDelay(17)).isBetween(0L, 30000L);
        assertThat(delay.createDelay(Integer.MAX_VALUE)).isBetween(0L, 30000L);
    }
}
