package com.lambdaworks.redis.masterslave;

import static com.lambdaworks.redis.TestSettings.port;
import static com.lambdaworks.redis.masterslave.MasterSlaveTest.slaveCall;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.lambdaworks.TestClientResources;
import com.lambdaworks.redis.*;
import org.junit.*;
import org.springframework.test.util.ReflectionTestUtils;

import com.lambdaworks.redis.codec.Utf8StringCodec;
import com.lambdaworks.redis.sentinel.AbstractSentinelTest;
import com.lambdaworks.redis.sentinel.SentinelRule;

import io.netty.channel.group.ChannelGroup;

/**
 * @author Mark Paluch
 */
public class MasterSlaveSentinelTest extends AbstractSentinelTest {

    static {
        sentinelClient = RedisClient
            .create(TestClientResources.create(), RedisURI.Builder.sentinel(TestSettings.host(), MASTER_ID).build());
    }

    @Rule
    public SentinelRule sentinelRule = new SentinelRule(sentinelClient, false, 26379, 26380);

    private RedisURI sentinelUri = RedisURI.Builder.sentinel(TestSettings.host(), 26379, MASTER_ID).build();
    private Pattern pattern = Pattern.compile("role:(\\w+)");

    @Before
    public void before() throws Exception {
        sentinelRule.needMasterWithSlave(MASTER_ID, port(3), port(4));
    }

    @Test
    public void testMasterSlaveSentinelBasic() throws Exception {

        StatefulRedisMasterSlaveConnection<String, String> connection = MasterSlave.connect(sentinelClient,
                new Utf8StringCodec(), sentinelUri);

        connection.setReadFrom(ReadFrom.SLAVE);
        String server = slaveCall(connection);

        assertThatServerIs(server, "slave");

        connection.setReadFrom(ReadFrom.MASTER);
        server = slaveCall(connection);
        assertThatServerIs(server, "master");

        connection.close();
    }

    @Test
    public void testMasterSlaveSentinelConnectionCount() throws Exception {

        ChannelGroup channels = (ChannelGroup) ReflectionTestUtils.getField(sentinelClient, "channels");
        int count = channels.size();

        StatefulRedisMasterSlaveConnection<String, String> connection = MasterSlave.connect(sentinelClient,
                new Utf8StringCodec(), sentinelUri);

        connection.sync().ping();
        connection.setReadFrom(ReadFrom.SLAVE);
        slaveCall(connection);

        assertThat(channels.size()).isEqualTo(count + 2 /* connections */ + 1 /* sentinel connections */);

        connection.close();
    }

    @Test
    public void testMasterSlaveSentinelClosesSentinelConnections() throws Exception {

        ChannelGroup channels = (ChannelGroup) ReflectionTestUtils.getField(sentinelClient, "channels");
        int count = channels.size();

        StatefulRedisMasterSlaveConnection<String, String> connection = MasterSlave.connect(sentinelClient,
                new Utf8StringCodec(), sentinelUri);

        connection.sync().ping();
        connection.setReadFrom(ReadFrom.SLAVE);
        slaveCall(connection);
        connection.close();

        assertThat(channels.size()).isEqualTo(count);
    }

    protected void assertThatServerIs(String server, String expectation) {
        Matcher matcher = pattern.matcher(server);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group(1)).isEqualTo(expectation);
    }

}
