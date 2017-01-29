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
import static org.junit.Assume.assumeTrue;

import java.util.Arrays;

import org.junit.*;

import com.lambdaworks.TestClientResources;
import com.lambdaworks.redis.*;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.codec.Utf8StringCodec;
import com.lambdaworks.redis.output.StatusOutput;
import com.lambdaworks.redis.protocol.*;

/**
 * @author Mark Paluch
 */
public class CustomCommandTest extends AbstractTest {

    private static final Utf8StringCodec utf8StringCodec = new Utf8StringCodec();
    private static RedisClient redisClient;

    private RedisCommands<String, String> redis;
    private StatefulRedisConnection<String, String> connection;

    @BeforeClass
    public static void beforeClass() {
        redisClient = RedisClient.create(TestClientResources.get());
    }

    @AfterClass
    public static void afterClass() {
        FastShutdown.shutdown(redisClient);
    }

    @Before
    public void before() {

        RedisURI uri = RedisURI.create("redis-sentinel://127.0.0.1:26379?sentinelMasterId=mymaster&timeout=5s");
        connection = MasterSlave.connect(redisClient, utf8StringCodec, uri);

        redis = connection.sync();
    }

    @After
    public void after() {
        connection.close();
    }

    @Test
    public void dispatchSet() throws Exception {

        String response = redis.dispatch(MyCommands.SET, new StatusOutput<>(utf8StringCodec),
                new CommandArgs<>(utf8StringCodec).addKey(key).addValue(value));

        assertThat(response).isEqualTo("OK");
    }

    @Test
    public void dispatchWithoutArgs() throws Exception {

        String response = redis.dispatch(MyCommands.INFO, new StatusOutput<>(utf8StringCodec));

        assertThat(response).contains("connected_clients");
    }

    @Test(expected = RedisCommandExecutionException.class)
    public void dispatchShouldFailForWrongDataType() throws Exception {

        redis.hset(key, key, value);
        redis.dispatch(CommandType.GET, new StatusOutput<>(utf8StringCodec), new CommandArgs<>(utf8StringCodec).addKey(key));
    }

    @Test
    public void dispatchTransactions() throws Exception {

        redis.multi();
        String response = redis.dispatch(CommandType.SET, new StatusOutput<>(utf8StringCodec), new CommandArgs<>(
                utf8StringCodec).addKey(key).addValue(value));

        TransactionResult exec = redis.exec();

        assertThat(response).isNull();
        assertThat(exec).hasSize(1).contains("OK");
    }

    @Test
    public void masterSlaveAsyncPing() throws Exception {

        RedisCommand<String, String, String> command = new Command<>(MyCommands.PING,
                new StatusOutput<>(new Utf8StringCodec()), null);

        AsyncCommand<String, String, String> async = new AsyncCommand<>(command);
        getStandaloneConnection().dispatch(async);

        assertThat(async.get()).isEqualTo("PONG");
    }

    @Test
    public void masterSlaveAsyncBatchPing() throws Exception {

        RedisCommand<String, String, String> command1 = new Command<>(CommandType.SET, new StatusOutput<>(utf8StringCodec),
                new CommandArgs<>(utf8StringCodec).addKey("key1").addValue("value"));

        RedisCommand<String, String, String> command2 = new Command<>(CommandType.GET, new StatusOutput<>(utf8StringCodec),
                new CommandArgs<>(utf8StringCodec).addKey("key1"));

        RedisCommand<String, String, String> command3 = new Command<>(CommandType.SET, new StatusOutput<>(utf8StringCodec),
                new CommandArgs<>(utf8StringCodec).addKey("other-key1").addValue("value"));

        AsyncCommand<String, String, String> async1 = new AsyncCommand<>(command1);
        AsyncCommand<String, String, String> async2 = new AsyncCommand<>(command2);
        AsyncCommand<String, String, String> async3 = new AsyncCommand<>(command3);
        getStandaloneConnection().dispatch(Arrays.asList(async1, async2, async3));

        assertThat(async1.get()).isEqualTo("OK");
        assertThat(async2.get()).isEqualTo("value");
        assertThat(async3.get()).isEqualTo("OK");
    }

    @Test
    public void masterSlaveFireAndForget() throws Exception {

        RedisCommand<String, String, String> command = new Command<>(MyCommands.PING,
                new StatusOutput<>(new Utf8StringCodec()), null);
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
