/*
 * Copyright 2011-2018 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Fail.fail;

import org.junit.jupiter.api.Test;

import io.lettuce.core.AbstractRedisClientTest;
import io.lettuce.core.api.sync.RedisHLLCommands;

public class HLLCommandTest extends AbstractRedisClientTest {

    private RedisHLLCommands<String, String> commands() {
        return redis;
    }

    @Test
    void pfadd() {

        assertThat(commands().pfadd(key, value, value)).isEqualTo(1);
        assertThat(commands().pfadd(key, value, value)).isEqualTo(0);
        assertThat(commands().pfadd(key, value)).isEqualTo(0);
    }

    @Test
    void pfaddNoValues() {
        assertThatThrownBy(() -> commands().pfadd(key)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void pfaddNullValues() {
        try {
            commands().pfadd(key, null);
            fail("Missing IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        try {
            commands().pfadd(key, value, null);
            fail("Missing IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    void pfmerge() {
        commands().pfadd(key, value);
        commands().pfadd("key2", "value2");
        commands().pfadd("key3", "value3");

        assertThat(commands().pfmerge(key, "key2", "key3")).isEqualTo("OK");
        assertThat(commands().pfcount(key)).isEqualTo(3);

        commands().pfadd("key2660", "rand", "mat");
        commands().pfadd("key7112", "mat", "perrin");

        commands().pfmerge("key8885", "key2660", "key7112");

        assertThat(commands().pfcount("key8885")).isEqualTo(3);
    }

    @Test
    void pfmergeNoKeys() {
        assertThatThrownBy(() -> commands().pfmerge(key)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void pfcount() {
        commands().pfadd(key, value);
        commands().pfadd("key2", "value2");
        assertThat(commands().pfcount(key)).isEqualTo(1);
        assertThat(commands().pfcount(key, "key2")).isEqualTo(2);
    }

    @Test
    void pfcountNoKeys() {
        assertThatThrownBy(() -> commands().pfcount()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void pfaddPfmergePfCount() {

        commands().pfadd("key2660", "rand", "mat");
        commands().pfadd("key7112", "mat", "perrin");

        commands().pfmerge("key8885", "key2660", "key7112");

        assertThat(commands().pfcount("key8885")).isEqualTo(3);
    }
}
