package com.lambdaworks.redis.masterslave;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;

import java.util.Collections;
import java.util.List;
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
import com.lambdaworks.redis.models.role.RedisInstance;
import com.lambdaworks.redis.models.role.RedisNodeDescription;
import com.lambdaworks.redis.models.role.RoleParser;

/**
 * @author Mark Paluch
 */
public class MasterSlaveTest extends AbstractRedisClientTest {

    private RedisURI masterURI = RedisURI.Builder.redis(host, TestSettings.port(3)).withPassword(passwd).withDatabase(5)
            .build();
    private StatefulRedisMasterSlaveConnectionImpl<String, String> connection;
    private RedisAsyncCommands<String, String> connectionToNode1;
    private RedisAsyncCommands<String, String> connectionToNode2;

    private RedisURI master;
    private RedisURI slave;

    @Before
    public void before() throws Exception {

        RedisURI node1 = RedisURI.Builder.redis(host, TestSettings.port(3)).withDatabase(2).build();
        RedisURI node2 = RedisURI.Builder.redis(host, TestSettings.port(4)).withDatabase(2).build();

        connectionToNode1 = client.connect(node1).async();
        connectionToNode2 = client.connect(node2).async();

        RedisInstance node1Instance = RoleParser.parse(connectionToNode1.role().get(2, TimeUnit.SECONDS));
        RedisInstance node2Instance = RoleParser.parse(connectionToNode2.role().get(2, TimeUnit.SECONDS));

        if (node1Instance.getRole() == RedisInstance.Role.MASTER && node2Instance.getRole() == RedisInstance.Role.SLAVE) {
            master = node1;
            slave = node2;
        } else if (node2Instance.getRole() == RedisInstance.Role.MASTER
                && node1Instance.getRole() == RedisInstance.Role.SLAVE) {
            master = node2;
            slave = node1;
        } else {
            assumeTrue(String.format("Cannot run the test because I don't have a distinct master and slave but %s and %s",
                    node1Instance, node2Instance), false);
        }

        connectionToNode1.configSet("requirepass", passwd);
        connectionToNode1.configSet("masterauth", passwd);
        connectionToNode1.auth(passwd);

        connectionToNode2.configSet("requirepass", passwd);
        connectionToNode2.configSet("masterauth", passwd);
        connectionToNode2.auth(passwd);

        connection = (StatefulRedisMasterSlaveConnectionImpl) MasterSlave.connect(client, new Utf8StringCodec(), masterURI);
        connection.setReadFrom(ReadFrom.SLAVE);
    }

    @After
    public void after() throws Exception {

        if (connectionToNode1 != null) {
            connectionToNode1.configSet("requirepass", "");
            connectionToNode1.configSet("masterauth", "").get(1, TimeUnit.SECONDS);
            connectionToNode1.getStatefulConnection().close();
        }

        if (connectionToNode2 != null) {
            connectionToNode2.configSet("requirepass", "");
            connectionToNode2.configSet("masterauth", "").get(1, TimeUnit.SECONDS);
            connectionToNode2.getStatefulConnection().close();
        }

        if (connection != null) {
            connection.close();
        }
    }

    @Test
    public void testMasterSlaveReadFromMaster() throws Exception {

        connection.setReadFrom(ReadFrom.MASTER);
        String server = connection.sync().info("server");

        Pattern pattern = Pattern.compile("tcp_port:(\\d+)");
        Matcher matcher = pattern.matcher(server);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group(1)).isEqualTo("" + master.getPort());
    }

    @Test
    public void testMasterSlaveReadFromSlave() throws Exception {

        String server = connection.sync().info("server");

        Pattern pattern = Pattern.compile("tcp_port:(\\d+)");
        Matcher matcher = pattern.matcher(server);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group(1)).isEqualTo("" + slave.getPort());
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
