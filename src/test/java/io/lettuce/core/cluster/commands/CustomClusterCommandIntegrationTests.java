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
package io.lettuce.core.cluster.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.TestSupport;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.commands.CustomCommandIntegrationTests;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.protocol.*;
import io.lettuce.test.Futures;
import io.lettuce.test.LettuceExtension;

/**
 * @author Mark Paluch
 */
@ExtendWith(LettuceExtension.class)
class CustomClusterCommandIntegrationTests extends TestSupport {

    private final StatefulRedisClusterConnection<String, String> connection;

    private RedisAdvancedClusterCommands<String, String> redis;

    @Inject
    CustomClusterCommandIntegrationTests(StatefulRedisClusterConnection<String, String> connection) {
        this.connection = connection;
        this.redis = connection.sync();
        this.redis.flushall();
    }

    @Test
    void dispatchSet() {

        String response = redis.dispatch(CustomCommandIntegrationTests.MyCommands.SET, new StatusOutput<>(StringCodec.UTF8),
                new CommandArgs<>(StringCodec.UTF8).addKey(key).addValue(value));

        assertThat(response).isEqualTo("OK");
    }

    @Test
    void dispatchWithoutArgs() {

        String response = redis.dispatch(CustomCommandIntegrationTests.MyCommands.INFO, new StatusOutput<>(StringCodec.UTF8));

        assertThat(response).contains("connected_clients");
    }

    @Test
    void dispatchShouldFailForWrongDataType() {

        redis.hset(key, key, value);
        assertThatThrownBy(() -> redis.dispatch(CommandType.GET, new StatusOutput<>(StringCodec.UTF8),
                new CommandArgs<>(StringCodec.UTF8).addKey(key))).isInstanceOf(RedisCommandExecutionException.class);
    }

    @Test
    void clusterAsyncPing() {

        RedisCommand<String, String, String> command = new Command<>(CustomCommandIntegrationTests.MyCommands.PING,
                new StatusOutput<>(StringCodec.UTF8), null);

        AsyncCommand<String, String, String> async = new AsyncCommand<>(command);
        connection.dispatch(async);

        assertThat(Futures.get((RedisFuture) async)).isEqualTo("PONG");
    }

    @Test
    void clusterAsyncBatchPing() {

        RedisCommand<String, String, String> command1 = new Command<>(CustomCommandIntegrationTests.MyCommands.PING,
                new StatusOutput<>(StringCodec.UTF8), null);

        RedisCommand<String, String, String> command2 = new Command<>(CustomCommandIntegrationTests.MyCommands.PING,
                new StatusOutput<>(StringCodec.UTF8), null);

        AsyncCommand<String, String, String> async1 = new AsyncCommand<>(command1);
        AsyncCommand<String, String, String> async2 = new AsyncCommand<>(command2);
        connection.dispatch(Arrays.asList(async1, async2));

        assertThat(Futures.get(async1.toCompletableFuture())).isEqualTo("PONG");
        assertThat(Futures.get(async2.toCompletableFuture())).isEqualTo("PONG");
    }

    @Test
    void clusterAsyncBatchSet() {

        RedisCommand<String, String, String> command1 = new Command<>(CommandType.SET, new StatusOutput<>(StringCodec.UTF8),
                new CommandArgs<>(StringCodec.UTF8).addKey("key1").addValue("value"));

        RedisCommand<String, String, String> command2 = new Command<>(CommandType.GET, new StatusOutput<>(StringCodec.UTF8),
                new CommandArgs<>(StringCodec.UTF8).addKey("key1"));

        RedisCommand<String, String, String> command3 = new Command<>(CommandType.SET, new StatusOutput<>(StringCodec.UTF8),
                new CommandArgs<>(StringCodec.UTF8).addKey("other-key1").addValue("value"));

        AsyncCommand<String, String, String> async1 = new AsyncCommand<>(command1);
        AsyncCommand<String, String, String> async2 = new AsyncCommand<>(command2);
        AsyncCommand<String, String, String> async3 = new AsyncCommand<>(command3);
        connection.dispatch(Arrays.asList(async1, async2, async3));

        assertThat(Futures.get(async1.toCompletableFuture())).isEqualTo("OK");
        assertThat(Futures.get(async2.toCompletableFuture())).isEqualTo("value");
        assertThat(Futures.get(async3.toCompletableFuture())).isEqualTo("OK");
    }

    @Test
    void clusterFireAndForget() {

        RedisCommand<String, String, String> command = new Command<>(CustomCommandIntegrationTests.MyCommands.PING,
                new StatusOutput<>(StringCodec.UTF8), null);
        connection.dispatch(command);
        assertThat(command.isCancelled()).isFalse();

    }

}
