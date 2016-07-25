package com.lambdaworks.redis.cluster.commands;

import static org.assertj.core.api.Assertions.assertThat;

import com.lambdaworks.TestClientResources;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.lambdaworks.redis.*;
import com.lambdaworks.redis.cluster.AbstractClusterTest;
import com.lambdaworks.redis.cluster.ClusterTestUtil;
import com.lambdaworks.redis.cluster.RedisClusterClient;
import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;
import com.lambdaworks.redis.cluster.api.sync.RedisAdvancedClusterCommands;
import com.lambdaworks.redis.codec.Utf8StringCodec;
import com.lambdaworks.redis.commands.CustomCommandTest;
import com.lambdaworks.redis.output.StatusOutput;
import com.lambdaworks.redis.protocol.*;

import rx.Observable;

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
    public void standaloneAsyncPing() throws Exception {

        RedisCommand<String, String, String> command = new Command<>(CustomCommandTest.MyCommands.PING,
                new StatusOutput<>(new Utf8StringCodec()), null);

        AsyncCommand<String, String, String> async = new AsyncCommand<>(command);
        redisClusterConnection.dispatch(async);

        assertThat(async.get()).isEqualTo("PONG");
    }

    @Test
    public void standaloneFireAndForget() throws Exception {

        RedisCommand<String, String, String> command = new Command<>(CustomCommandTest.MyCommands.PING,
                new StatusOutput<>(new Utf8StringCodec()), null);
        redisClusterConnection.dispatch(command);
        assertThat(command.isCancelled()).isFalse();

    }

    @Test
    public void standaloneReactivePing() throws Exception {

        RedisCommand<String, String, String> command = new Command<>(CustomCommandTest.MyCommands.PING,
                new StatusOutput<>(new Utf8StringCodec()), null);
        ReactiveCommandDispatcher<String, String, String> dispatcher = new ReactiveCommandDispatcher<>(command,
                redisClusterConnection, false);

        String result = Observable.create(dispatcher).toBlocking().first();

        assertThat(result).isEqualTo("PONG");
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
