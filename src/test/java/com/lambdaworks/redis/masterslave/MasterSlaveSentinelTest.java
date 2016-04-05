package com.lambdaworks.redis.masterslave;

import static com.lambdaworks.redis.masterslave.MasterSlaveTest.slaveCall;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.BeforeClass;
import org.junit.Test;

import com.lambdaworks.redis.ReadFrom;
import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.TestSettings;
import com.lambdaworks.redis.codec.Utf8StringCodec;
import com.lambdaworks.redis.sentinel.AbstractSentinelTest;

/**
 * @author Mark Paluch
 */
public class MasterSlaveSentinelTest extends AbstractSentinelTest {

    private RedisURI sentinelUri = RedisURI.Builder.sentinel(TestSettings.host(), 26379, MASTER_ID).build();
    private Pattern pattern = Pattern.compile("role:(\\w+)");

    @BeforeClass
    public static void setupClient() {
        sentinelClient = RedisClient.create(RedisURI.Builder.sentinel(TestSettings.host(), MASTER_ID).build());
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

    protected void assertThatServerIs(String server, String expectation) {
        Matcher matcher = pattern.matcher(server);

        assertThat(matcher.find()).isTrue();
        assertThat(matcher.group(1)).isEqualTo(expectation);
    }

}
