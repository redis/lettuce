package com.lambdaworks.redis.masterslave;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.lambdaworks.redis.*;
import com.lambdaworks.redis.models.role.RedisNodeDescription;
import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.codec.Utf8StringCodec;

/**
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 */
public class MasterSlaveTest extends AbstractRedisClientTest {

    private RedisURI masterURI = RedisURI.Builder.redis(host, TestSettings.port(3)).build();
    private StatefulRedisMasterSlaveConnectionImpl<String, String> connection;

    @Before
    public void before() throws Exception {
        connection = (StatefulRedisMasterSlaveConnectionImpl) MasterSlave.connect(client, new Utf8StringCodec(), masterURI);
        connection.setReadFrom(ReadFrom.SLAVE);
    }

    @After
    public void after() throws Exception {
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

        connection.setReadFrom(new ReadFrom() {
            @Override
            public List<RedisNodeDescription> select(Nodes nodes) {
                return Lists.emptyList();
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

        assertThat(connectionProvider.getConnectionCount()).isEqualTo(1);
    }

    protected static String slaveCall(StatefulRedisMasterSlaveConnection<String, String> connection) {
        return connection.sync().info("replication");
    }

    protected MasterSlaveConnectionProvider getConnectionProvider() {
        MasterSlaveChannelWriter<String, String> writer = connection.getChannelWriter();
        return writer.getMasterSlaveConnectionProvider();
    }
}
