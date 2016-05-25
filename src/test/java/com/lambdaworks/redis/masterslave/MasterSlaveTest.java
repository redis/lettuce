package com.lambdaworks.redis.masterslave;

import com.lambdaworks.redis.*;
import com.lambdaworks.redis.api.async.RedisAsyncCommands;
import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.codec.Utf8StringCodec;
import com.lambdaworks.redis.models.role.RedisNodeDescription;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Paluch
 */
public class MasterSlaveTest extends AbstractRedisClientTest {

    private RedisURI masterURI = RedisURI.Builder.redis(host, TestSettings.port(3)).withPassword(passwd).withDatabase(5)
            .build();
    private StatefulRedisMasterSlaveConnectionImpl<String, String> connection;
    private RedisAsyncCommands<String, String> node1;
    private RedisAsyncCommands<String, String> node2;

    @Before
    public void before() throws Exception {

        node1 = client.connect(RedisURI.Builder.redis(host, TestSettings.port(3)).build()).async();
        node2 = client.connect(RedisURI.Builder.redis(host, TestSettings.port(4)).build()).async();

        node1.configSet("requirepass", passwd);
        node1.configSet("masterauth", passwd);
        node1.auth(passwd);

        node2.configSet("requirepass", passwd);
        node2.configSet("masterauth", passwd);
        node2.auth(passwd);

        connection = (StatefulRedisMasterSlaveConnectionImpl) MasterSlave.connect(client, new Utf8StringCodec(), masterURI);
        connection.setReadFrom(ReadFrom.SLAVE);
    }

    @After
    public void after() throws Exception {

        node1.configSet("requirepass", "");
        node1.configSet("masterauth", "").get(1, TimeUnit.SECONDS);

        node2.configSet("requirepass", "");
        node2.configSet("masterauth", "").get(1, TimeUnit.SECONDS);

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


    @Test
    public void testConnectToSlave() throws Exception {

        connection.close();

        RedisURI slaveUri = RedisURI.Builder.redis(host, TestSettings.port(4)).withPassword(passwd).build();
        connection = (StatefulRedisMasterSlaveConnectionImpl) MasterSlave.connect(client, new Utf8StringCodec(), slaveUri);

        RedisCommands<String, String> sync = connection.sync();
        sync.set(key, value);
    }

    @Test(expected = RedisException.class)
    public void noSlaveForRead() throws Exception {

        connection.setReadFrom(new ReadFrom() {
            @Override
            public List<RedisNodeDescription> select(Nodes nodes) {
                return Collections.emptyList();
            }
        });

        slaveCall(connection);
    }

    @Test
    public void testConnectionCount() throws Exception {

        MasterSlaveConnectionProvider connectionProvider = getConnectionProvider();

        assertThat(connectionProvider.getConnectionCount()).isEqualTo(1);
        slaveCall(connection);

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
