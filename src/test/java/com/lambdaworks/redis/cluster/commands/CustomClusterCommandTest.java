package com.lambdaworks.redis.cluster.commands;

import com.lambdaworks.redis.*;
import com.lambdaworks.redis.cluster.AbstractClusterTest;
import com.lambdaworks.redis.cluster.RedisAdvancedClusterConnection;
import com.lambdaworks.redis.codec.Utf8StringCodec;
import com.lambdaworks.redis.output.StatusOutput;
import com.lambdaworks.redis.protocol.CommandArgs;
import com.lambdaworks.redis.protocol.CommandType;
import org.junit.*;

import com.lambdaworks.redis.cluster.ClusterTestUtil;
import com.lambdaworks.redis.cluster.RedisClusterClient;

import java.util.List;

import static com.sun.corba.se.spi.activation.IIOP_CLEAR_TEXT.value;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Paluch
 */
public class CustomClusterCommandTest extends AbstractClusterTest {

    private static RedisClusterClient redisClusterClient;
    private RedisAdvancedClusterConnection<String, String> redis;

    @BeforeClass
    public static void setupClient() {
        redisClusterClient = new RedisClusterClient(
                RedisURI.Builder.redis(TestSettings.host(), TestSettings.port(900)).build());
    }

    @AfterClass
    public static void closeClient() {
        FastShutdown.shutdown(redisClusterClient);
    }

    @Before
    public void openConnection() throws Exception {
        redis = redisClusterClient.connectCluster();
        redis.flushall();
    }

   protected final Utf8StringCodec utf8StringCodec = new Utf8StringCodec();

    @Test
    public void dispatchSet() throws Exception {

        String response = redis.dispatch(CommandType.SET, new StatusOutput<String, String>(utf8StringCodec),
                new CommandArgs<String, String>(utf8StringCodec).addKey(key).addValue(value));

        assertThat(response).isEqualTo("OK");
    }

    @Test
    public void dispatchWithoutArgs() throws Exception {

        String response = redis.dispatch(CommandType.INFO, new StatusOutput<String, String>(utf8StringCodec));

        assertThat(response).contains("connected_clients");
    }

    @Test(expected = RedisCommandExecutionException.class)
    public void dispatchShouldFailForWrongDataType() throws Exception {

        redis.hset(key, key, value);
        redis.dispatch(CommandType.GET, new StatusOutput<String, String>(utf8StringCodec),
                new CommandArgs<String, String>(utf8StringCodec).addKey(key));
    }

}
