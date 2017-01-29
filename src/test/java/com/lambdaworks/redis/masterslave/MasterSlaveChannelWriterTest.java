/*
 * Copyright 2017 the original author or authors.
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
package com.lambdaworks.redis.masterslave;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import com.lambdaworks.redis.protocol.Command;
import com.lambdaworks.redis.protocol.CommandType;
import com.lambdaworks.redis.protocol.RedisCommand;

/**
 * @author Mark Paluch
 */
public class MasterSlaveChannelWriterTest {

    @Test
    public void shouldReturnIntentForWriteCommand() {

        RedisCommand<String, String, String> set = new Command<>(CommandType.SET, null);
        RedisCommand<String, String, String> mset = new Command<>(CommandType.MSET, null);

        assertThat(MasterSlaveChannelWriter.getIntent(Arrays.asList(set, mset))).isEqualTo(
                MasterSlaveConnectionProvider.Intent.WRITE);

        assertThat(MasterSlaveChannelWriter.getIntent(Collections.singletonList(set))).isEqualTo(
                MasterSlaveConnectionProvider.Intent.WRITE);
    }

    @Test
    public void shouldReturnDefaultIntentForNoCommands() {

        assertThat(MasterSlaveChannelWriter.getIntent(Collections.emptyList())).isEqualTo(
                MasterSlaveConnectionProvider.Intent.WRITE);
    }

    @Test
    public void shouldReturnIntentForReadCommand() {

        RedisCommand<String, String, String> get = new Command<>(CommandType.GET, null);
        RedisCommand<String, String, String> mget = new Command<>(CommandType.MGET, null);

        assertThat(MasterSlaveChannelWriter.getIntent(Arrays.asList(get, mget))).isEqualTo(
                MasterSlaveConnectionProvider.Intent.READ);

        assertThat(MasterSlaveChannelWriter.getIntent(Collections.singletonList(get))).isEqualTo(
                MasterSlaveConnectionProvider.Intent.READ);
    }

    @Test
    public void shouldReturnIntentForMixedCommands() {

        RedisCommand<String, String, String> set = new Command<>(CommandType.SET, null);
        RedisCommand<String, String, String> mget = new Command<>(CommandType.MGET, null);

        assertThat(MasterSlaveChannelWriter.getIntent(Arrays.asList(set, mget))).isEqualTo(
                MasterSlaveConnectionProvider.Intent.WRITE);

        assertThat(MasterSlaveChannelWriter.getIntent(Collections.singletonList(set))).isEqualTo(
                MasterSlaveConnectionProvider.Intent.WRITE);
    }

}