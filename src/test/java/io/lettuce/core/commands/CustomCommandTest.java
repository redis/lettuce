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
package io.lettuce.core.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;

import java.util.Arrays;

import org.junit.Test;

import io.lettuce.core.AbstractRedisClientTest;
import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.TransactionResult;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.Utf8StringCodec;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.protocol.*;

/**
 * @author Mark Paluch
 */
public class CustomCommandTest extends AbstractRedisClientTest {

    protected final Utf8StringCodec utf8StringCodec = new Utf8StringCodec();

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
    public void standaloneAsyncPing() throws Exception {

        RedisCommand<String, String, String> command = new Command<>(MyCommands.PING, new StatusOutput<>(utf8StringCodec), null);

        AsyncCommand<String, String, String> async = new AsyncCommand<>(command);
        getStandaloneConnection().dispatch(async);

        assertThat(async.get()).isEqualTo("PONG");
    }

    @Test
    public void standaloneAsyncBatchPing() throws Exception {

        RedisCommand<String, String, String> command1 = new Command<>(MyCommands.PING, new StatusOutput<>(utf8StringCodec),
                null);

        RedisCommand<String, String, String> command2 = new Command<>(MyCommands.PING, new StatusOutput<>(utf8StringCodec),
                null);

        AsyncCommand<String, String, String> async1 = new AsyncCommand<>(command1);
        AsyncCommand<String, String, String> async2 = new AsyncCommand<>(command2);
        getStandaloneConnection().dispatch(Arrays.asList(async1, async2));

        assertThat(async1.get()).isEqualTo("PONG");
        assertThat(async2.get()).isEqualTo("PONG");
    }

    @Test
    public void standaloneAsyncBatchTransaction() throws Exception {

        RedisCommand<String, String, String> multi = new Command<>(CommandType.MULTI, new StatusOutput<>(utf8StringCodec));

        RedisCommand<String, String, String> set = new Command<>(CommandType.SET, new StatusOutput<>(utf8StringCodec),
                new CommandArgs<>(utf8StringCodec).addKey("key").add("value"));

        RedisCommand<String, String, TransactionResult> exec = new Command<>(CommandType.EXEC, null);

        AsyncCommand<String, String, String> async1 = new AsyncCommand<>(multi);
        AsyncCommand<String, String, String> async2 = new AsyncCommand<>(set);
        AsyncCommand<String, String, TransactionResult> async3 = new AsyncCommand<>(exec);
        getStandaloneConnection().dispatch(Arrays.asList(async1, async2, async3));

        assertThat(async1.get()).isEqualTo("OK");
        assertThat(async2.get()).isEqualTo("OK");

        TransactionResult transactionResult = async3.get();
        assertThat(transactionResult.wasRolledBack()).isFalse();
        assertThat(transactionResult.<String> get(0)).isEqualTo("OK");
    }

    @Test
    public void standaloneFireAndForget() throws Exception {

        RedisCommand<String, String, String> command = new Command<>(MyCommands.PING, new StatusOutput<>(utf8StringCodec), null);
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
