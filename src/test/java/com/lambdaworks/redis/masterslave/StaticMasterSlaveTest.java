package com.lambdaworks.redis.masterslave;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.lambdaworks.redis.*;
import com.lambdaworks.redis.api.async.RedisAsyncCommands;
import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.codec.Utf8StringCodec;

/**
 * @author Mark Paluch
 */
public class StaticMasterSlaveTest extends AbstractRedisClientTest {

    private StatefulRedisMasterSlaveConnectionImpl<String, String> connection;

    private RedisURI node1 = RedisURI.Builder.redis(host, TestSettings.port(3)).withPassword(passwd).withDatabase(2).build();
    private RedisURI node2 = RedisURI.Builder.redis(host, TestSettings.port(4)).withPassword(passwd).withDatabase(2).build();

    private RedisAsyncCommands<String, String> connectionToNode1;
    private RedisAsyncCommands<String, String> connectionToNode2;

    @Before
    public void before() throws Exception {

        connectionToNode1 = client.connect(RedisURI.Builder.redis(host, TestSettings.port(3)).build()).async();
        connectionToNode2 = client.connect(RedisURI.Builder.redis(host, TestSettings.port(4)).build()).async();

        connectionToNode1.configSet("requirepass", passwd);
        connectionToNode1.configSet("masterauth", passwd);
        connectionToNode1.auth(passwd);

        connectionToNode2.configSet("requirepass", passwd);
        connectionToNode2.configSet("masterauth", passwd);
        connectionToNode2.auth(passwd);

        connection = (StatefulRedisMasterSlaveConnectionImpl) MasterSlave.connect(client, new Utf8StringCodec(),
                Arrays.asList(node1, node2));
        connection.setReadFrom(ReadFrom.SLAVE);
    }

    @After
    public void after() throws Exception {

        connectionToNode1.configSet("requirepass", "");
        connectionToNode1.configSet("masterauth", "").get(1, TimeUnit.SECONDS);

        connectionToNode2.configSet("requirepass", "");
        connectionToNode2.configSet("masterauth", "").get(1, TimeUnit.SECONDS);

        connection.close();
    }

    @Test
    public void testMasterSlaveStandaloneBasic() throws Exception {

        String server = connection.sync().info("server");

        Pattern pattern = Pattern.compile("tcp_port:(\\d+)");
        Matcher matcher = pattern.matcher(server);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group(1)).isEqualTo("6483");
        assertThat(connection.getReadFrom()).isEqualTo(ReadFrom.SLAVE);
    }

    @Test
    public void testMasterSlaveReadWrite() throws Exception {

        RedisCommands<String, String> redisCommands = connection.sync();
        redisCommands.set(key, value);
        redisCommands.waitForReplication(1, 100);

        assertThat(redisCommands.get(key)).isEqualTo(value);
    }

    @Test(expected = RedisException.class)
    public void noSlaveForRead() throws Exception {

        connection.close();

        connection = (StatefulRedisMasterSlaveConnectionImpl) MasterSlave.connect(client, new Utf8StringCodec(),
                Arrays.asList(node1));
        connection.setReadFrom(ReadFrom.SLAVE);

        slaveCall(connection);
    }

    @Test
    public void shouldWorkWithMasterOnly() throws Exception {

        connection.close();

        connection = (StatefulRedisMasterSlaveConnectionImpl) MasterSlave.connect(client, new Utf8StringCodec(),
                Arrays.asList(node1));

        connection.sync().set(key, value);
        assertThat(connection.sync().get(key)).isEqualTo("value");
    }

    @Test
    public void shouldWorkWithSlaveOnly() throws Exception {

        connection.close();

        connection = (StatefulRedisMasterSlaveConnectionImpl) MasterSlave.connect(client, new Utf8StringCodec(),
                Arrays.asList(node2));
        connection.setReadFrom(ReadFrom.MASTER_PREFERRED);

        assertThat(connection.sync().info()).isNotEmpty();
    }

    @Test(expected = RedisException.class)
    public void noMasterForWrite() throws Exception {

        connection.close();

        connection = (StatefulRedisMasterSlaveConnectionImpl) MasterSlave.connect(client, new Utf8StringCodec(),
                Arrays.asList(node2));

        connection.sync().set(key, value);
    }

    @Test
    public void testConnectionCount() throws Exception {

        MasterSlaveConnectionProvider connectionProvider = getConnectionProvider();

        assertThat(connectionProvider.getConnectionCount()).isEqualTo(0);
        slaveCall(connection);

        assertThat(connectionProvider.getConnectionCount()).isEqualTo(1);

        connection.sync().set(key, value);
        assertThat(connectionProvider.getConnectionCount()).isEqualTo(2);
    }

    @Test
    public void testReconfigureTopology() throws Exception {
        MasterSlaveConnectionProvider connectionProvider = getConnectionProvider();

        slaveCall(connection);

        connectionProvider.setKnownNodes(Collections.emptyList());

        assertThat(connectionProvider.getConnectionCount()).isEqualTo(0);
    }

    protected static String slaveCall(StatefulRedisMasterSlaveConnection<String, String> connection) {
        return connection.sync().info("replication");
    }

    protected MasterSlaveConnectionProvider getConnectionProvider() {
        MasterSlaveChannelWriter<String, String> writer = connection.getChannelWriter();
        return writer.getMasterSlaveConnectionProvider();
    }
}
