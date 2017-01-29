/*
 * Copyright 2011-2017 the original author or authors.
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
package com.lambdaworks.redis.cluster.commands;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.lambdaworks.TestClientResources;
import com.lambdaworks.redis.FastShutdown;
import com.lambdaworks.redis.RedisCommandExecutionException;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.TestSettings;
import com.lambdaworks.redis.cluster.AbstractClusterTest;
import com.lambdaworks.redis.cluster.ClusterTestUtil;
import com.lambdaworks.redis.cluster.RedisClusterClient;
import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;
import com.lambdaworks.redis.cluster.api.sync.RedisAdvancedClusterCommands;
import com.lambdaworks.redis.codec.Utf8StringCodec;
import com.lambdaworks.redis.commands.CustomCommandTest;
import com.lambdaworks.redis.output.StatusOutput;
import com.lambdaworks.redis.protocol.*;

/**
 * @author Mark Paluch
 */
public class CustomClusterCommandTest extends AbstractClusterTest {

    private static final Utf8StringCodec utf8StringCodec = new Utf8StringCodec();
    private static RedisClusterClient redisClusterClient;
    private StatefulRedisClusterConnection<String, String> redisClusterConnection;
    private RedisAdvancedClusterCommands<String, String> redis;

    @BeforeClass
    public static void setupClient() {
        redisClusterClient = RedisClusterClient.create(TestClientResources.get(),
                RedisURI.Builder.redis(TestSettings.host(), TestSettings.port(900)).build());
    }

    @AfterClass
    public static void closeClient() {
        FastShutdown.shutdown(redisClusterClient);
    }

    @Before
    public void openConnection() throws Exception {
        redisClusterConnection = redisClusterClient.connect();
        redis = redisClusterConnection.sync();
        ClusterTestUtil.flushDatabaseOfAllNodes(redisClusterConnection);
    }

    @Test
    public void dispatchSet() throws Exception {

        String response = redis.dispatch(CustomCommandTest.MyCommands.SET, new StatusOutput<>(utf8StringCodec),
                new CommandArgs<>(utf8StringCodec).addKey(key).addValue(value));

        assertThat(response).isEqualTo("OK");
    }

    @Test
    public void dispatchWithoutArgs() throws Exception {

        String response = redis.dispatch(CustomCommandTest.MyCommands.INFO, new StatusOutput<>(utf8StringCodec));

        assertThat(response).contains("connected_clients");
    }

    @Test(expected = RedisCommandExecutionException.class)
    public void dispatchShouldFailForWrongDataType() throws Exception {

        redis.hset(key, key, value);
        redis.dispatch(CommandType.GET, new StatusOutput<>(utf8StringCodec), new CommandArgs<>(utf8StringCodec).addKey(key));
    }

    @Test
    public void clusterAsyncPing() throws Exception {

        RedisCommand<String, String, String> command = new Command<>(CustomCommandTest.MyCommands.PING, new StatusOutput<>(
                utf8StringCodec), null);

        AsyncCommand<String, String, String> async = new AsyncCommand<>(command);
        redisClusterConnection.dispatch(async);

        assertThat(async.get()).isEqualTo("PONG");
    }

    @Test
    public void clusterAsyncBatchPing() throws Exception {

        RedisCommand<String, String, String> command1 = new Command<>(CustomCommandTest.MyCommands.PING, new StatusOutput<>(
                utf8StringCodec), null);

        RedisCommand<String, String, String> command2 = new Command<>(CustomCommandTest.MyCommands.PING, new StatusOutput<>(
                utf8StringCodec), null);

        AsyncCommand<String, String, String> async1 = new AsyncCommand<>(command1);
        AsyncCommand<String, String, String> async2 = new AsyncCommand<>(command2);
        redisClusterConnection.dispatch(Arrays.asList(async1, async2));

        assertThat(async1.get()).isEqualTo("PONG");
        assertThat(async2.get()).isEqualTo("PONG");
    }

    @Test
    public void clusterAsyncBatchSet() throws Exception {

        RedisCommand<String, String, String> command1 = new Command<>(CommandType.SET, new StatusOutput<>(utf8StringCodec),
                new CommandArgs<>(utf8StringCodec).addKey("key1").addValue("value"));

        RedisCommand<String, String, String> command2 = new Command<>(CommandType.GET, new StatusOutput<>(utf8StringCodec),
                new CommandArgs<>(utf8StringCodec).addKey("key1"));

        RedisCommand<String, String, String> command3 = new Command<>(CommandType.SET, new StatusOutput<>(utf8StringCodec),
                new CommandArgs<>(utf8StringCodec).addKey("other-key1").addValue("value"));

        AsyncCommand<String, String, String> async1 = new AsyncCommand<>(command1);
        AsyncCommand<String, String, String> async2 = new AsyncCommand<>(command2);
        AsyncCommand<String, String, String> async3 = new AsyncCommand<>(command3);
        redisClusterConnection.dispatch(Arrays.asList(async1, async2, async3));

        assertThat(async1.get()).isEqualTo("OK");
        assertThat(async2.get()).isEqualTo("value");
        assertThat(async3.get()).isEqualTo("OK");
    }

    @Test
    public void clusterFireAndForget() throws Exception {

        RedisCommand<String, String, String> command = new Command<>(CustomCommandTest.MyCommands.PING, new StatusOutput<>(
                utf8StringCodec), null);
        redisClusterConnection.dispatch(command);
        assertThat(command.isCancelled()).isFalse();

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
