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
package io.lettuce.core.commands;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

import io.lettuce.core.AbstractRedisClientTest;
import org.assertj.core.api.Assertions;
import org.junit.Test;

/**
 * @author Will Glozer
 * @author Mark Paluch
 */
public class NumericCommandTest extends AbstractRedisClientTest {
    @Test
    public void decr() throws Exception {
        assertThat((long) redis.decr(key)).isEqualTo(-1);
        assertThat((long) redis.decr(key)).isEqualTo(-2);
    }

    @Test
    public void decrby() throws Exception {
        assertThat(redis.decrby(key, 3)).isEqualTo(-3);
        assertThat(redis.decrby(key, 3)).isEqualTo(-6);
    }

    @Test
    public void incr() throws Exception {
        assertThat((long) redis.incr(key)).isEqualTo(1);
        assertThat((long) redis.incr(key)).isEqualTo(2);
    }

    @Test
    public void incrby() throws Exception {
        assertThat(redis.incrby(key, 3)).isEqualTo(3);
        assertThat(redis.incrby(key, 3)).isEqualTo(6);
    }

    @Test
    public void incrbyfloat() throws Exception {

        assertThat(redis.incrbyfloat(key, 3.0)).isEqualTo(3.0, offset(0.1));
        assertThat(redis.incrbyfloat(key, 0.2)).isEqualTo(3.2, offset(0.1));
    }
}
