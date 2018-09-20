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
package io.lettuce.core.cluster;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import io.lettuce.core.cluster.ClusterConnectionProvider.Intent;
import io.lettuce.core.internal.HostAndPort;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandType;
import io.lettuce.core.protocol.RedisCommand;

/**
 * @author Mark Paluch
 */
class ClusterDistributionChannelWriterUnitTests {

    @Test
    void shouldParseAskTargetCorrectly() {

        HostAndPort askTarget = ClusterDistributionChannelWriter.getAskTarget("ASK 1234 127.0.0.1:6381");

        assertThat(askTarget.getHostText()).isEqualTo("127.0.0.1");
        assertThat(askTarget.getPort()).isEqualTo(6381);
    }

    @Test
    void shouldParseIPv6AskTargetCorrectly() {

        HostAndPort askTarget = ClusterDistributionChannelWriter.getAskTarget("ASK 1234 1:2:3:4::6:6381");

        assertThat(askTarget.getHostText()).isEqualTo("1:2:3:4::6");
        assertThat(askTarget.getPort()).isEqualTo(6381);
    }

    @Test
    void shouldParseMovedTargetCorrectly() {

        HostAndPort moveTarget = ClusterDistributionChannelWriter.getMoveTarget("MOVED 1234 127.0.0.1:6381");

        assertThat(moveTarget.getHostText()).isEqualTo("127.0.0.1");
        assertThat(moveTarget.getPort()).isEqualTo(6381);
    }

    @Test
    void shouldParseIPv6MovedTargetCorrectly() {

        HostAndPort moveTarget = ClusterDistributionChannelWriter.getMoveTarget("MOVED 1234 1:2:3:4::6:6381");

        assertThat(moveTarget.getHostText()).isEqualTo("1:2:3:4::6");
        assertThat(moveTarget.getPort()).isEqualTo(6381);
    }

    @Test
    void shouldReturnIntentForWriteCommand() {

        RedisCommand<String, String, String> set = new Command<>(CommandType.SET, null);
        RedisCommand<String, String, String> mset = new Command<>(CommandType.MSET, null);

        assertThat(ClusterDistributionChannelWriter.getIntent(Arrays.asList(set, mset))).isEqualTo(Intent.WRITE);

        assertThat(ClusterDistributionChannelWriter.getIntent(Collections.singletonList(set))).isEqualTo(Intent.WRITE);
    }

    @Test
    void shouldReturnDefaultIntentForNoCommands() {

        assertThat(ClusterDistributionChannelWriter.getIntent(Collections.emptyList())).isEqualTo(Intent.WRITE);
    }

    @Test
    void shouldReturnIntentForReadCommand() {

        RedisCommand<String, String, String> get = new Command<>(CommandType.GET, null);
        RedisCommand<String, String, String> mget = new Command<>(CommandType.MGET, null);

        assertThat(ClusterDistributionChannelWriter.getIntent(Arrays.asList(get, mget))).isEqualTo(Intent.READ);

        assertThat(ClusterDistributionChannelWriter.getIntent(Collections.singletonList(get))).isEqualTo(Intent.READ);
    }

    @Test
    void shouldReturnIntentForMixedCommands() {

        RedisCommand<String, String, String> set = new Command<>(CommandType.SET, null);
        RedisCommand<String, String, String> mget = new Command<>(CommandType.MGET, null);

        assertThat(ClusterDistributionChannelWriter.getIntent(Arrays.asList(set, mget))).isEqualTo(Intent.WRITE);

        assertThat(ClusterDistributionChannelWriter.getIntent(Collections.singletonList(set))).isEqualTo(Intent.WRITE);
    }
}
