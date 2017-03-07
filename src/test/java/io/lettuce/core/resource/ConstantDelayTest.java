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
 * @author Mark Paluch
 */
public class ConstantDelayTest {

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotCreateIfDelayIsNegative() throws Exception {
        Delay.constant(-1, TimeUnit.MILLISECONDS);
    }

    @Test
    public void shouldCreateZeroDelay() throws Exception {

        Delay delay = Delay.constant(0, TimeUnit.MILLISECONDS);

        assertThat(delay.createDelay(0)).isEqualTo(0);
        assertThat(delay.createDelay(5)).isEqualTo(0);
    }

    @Test
    public void shouldCreateConstantDelay() throws Exception {

        Delay delay = Delay.constant(100, TimeUnit.MILLISECONDS);

        assertThat(delay.createDelay(0)).isEqualTo(100);
        assertThat(delay.createDelay(5)).isEqualTo(100);
    }
}
