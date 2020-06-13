/*
 * Copyright 2020 the original author or authors.
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
package io.lettuce.core.masterreplica;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.Arrays;

import javax.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.*;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.protocol.*;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.TestFutures;

/**
 * @author Mark Paluch
 */
@ExtendWith(LettuceExtension.class)
class CustomCommandIntegrationTests extends TestSupport {

    private final RedisClient redisClient;

    private StatefulRedisConnection<String, String> connection;
    private RedisCommands<String, String> redis;

    @Inject
    CustomCommandIntegrationTests(RedisClient redisClient) {
        this.redisClient = redisClient;
    }

    @BeforeEach
    void before() {

        RedisURI uri = RedisURI.create("redis-sentinel://127.0.0.1:26379?sentinelMasterId=mymaster&timeout=5s");
        connection = MasterReplica.connect(redisClient, StringCodec.UTF8, uri);
        redis = connection.sync();
        redis.flushall();
    }

    @AfterEach
    void after() {
        connection.close();
    }

    @Test
    void dispatchSet() {

        String response = redis.dispatch(MyCommands.SET, new StatusOutput<>(StringCodec.UTF8),
                new CommandArgs<>(StringCodec.UTF8).addKey(key).addValue(value));

        assertThat(response).isEqualTo("OK");
    }

    @Test
    void dispatchWithoutArgs() {

        String response = redis.dispatch(MyCommands.INFO, new StatusOutput<>(StringCodec.UTF8));

        assertThat(response).contains("connected_clients");
    }

    @Test
    void dispatchShouldFailForWrongDataType() {

        redis.hset(key, key, value);
        assertThatThrownBy(() -> redis.dispatch(CommandType.GET, new StatusOutput<>(StringCodec.UTF8),
                new CommandArgs<>(StringCodec.UTF8).addKey(key))).isInstanceOf(RedisCommandExecutionException.class);
    }

    @Test
    void dispatchTransactions() {

        redis.multi();
        String response = redis.dispatch(CommandType.SET, new StatusOutput<>(StringCodec.UTF8),
                new CommandArgs<>(StringCodec.UTF8).addKey(key).addValue(value));

        TransactionResult exec = redis.exec();

        assertThat(response).isNull();
        assertThat(exec).hasSize(1).contains("OK");
    }

    @Test
    void masterReplicaAsyncPing() {

        RedisCommand<String, String, String> command = new Command<>(MyCommands.PING, new StatusOutput<>(StringCodec.UTF8),
                null);

        AsyncCommand<String, String, String> async = new AsyncCommand<>(command);
        getStandaloneConnection().dispatch(async);

        assertThat(TestFutures.getOrTimeout(async.toCompletableFuture())).isEqualTo("PONG");
    }

    @Test
    void masterReplicaAsyncBatchPing() {

        RedisCommand<String, String, String> command1 = new Command<>(CommandType.SET, new StatusOutput<>(StringCodec.UTF8),
                new CommandArgs<>(StringCodec.UTF8).addKey("key1").addValue("value"));

        RedisCommand<String, String, String> command2 = new Command<>(CommandType.GET, new StatusOutput<>(StringCodec.UTF8),
                new CommandArgs<>(StringCodec.UTF8).addKey("key1"));

        RedisCommand<String, String, String> command3 = new Command<>(CommandType.SET, new StatusOutput<>(StringCodec.UTF8),
                new CommandArgs<>(StringCodec.UTF8).addKey("other-key1").addValue("value"));

        AsyncCommand<String, String, String> async1 = new AsyncCommand<>(command1);
        AsyncCommand<String, String, String> async2 = new AsyncCommand<>(command2);
        AsyncCommand<String, String, String> async3 = new AsyncCommand<>(command3);
        getStandaloneConnection().dispatch(Arrays.asList(async1, async2, async3));

        assertThat(TestFutures.getOrTimeout(async1.toCompletableFuture())).isEqualTo("OK");
        assertThat(TestFutures.getOrTimeout(async2.toCompletableFuture())).isEqualTo("value");
        assertThat(TestFutures.getOrTimeout(async3.toCompletableFuture())).isEqualTo("OK");
    }

    @Test
    void masterReplicaFireAndForget() {

        RedisCommand<String, String, String> command = new Command<>(MyCommands.PING, new StatusOutput<>(StringCodec.UTF8),
                null);
        getStandaloneConnection().dispatch(command);
        assertThat(command.isCancelled()).isFalse();

    }

    private StatefulRedisConnection<String, String> getStandaloneConnection() {

        assumeTrue(redis.getStatefulConnection() instanceof StatefulRedisConnection);
        return redis.getStatefulConnection();
    }

    public enum MyCommands implements ProtocolKeyword {
        PING, SET, INFO;

        private final byte name[];

        MyCommands() {
            // cache the bytes for the command name. Reduces memory and cpu pressure when using commands.
            name = name().getBytes();
        }

        @Override
        public byte[] getBytes() {
            return name;
        }
    }
}
