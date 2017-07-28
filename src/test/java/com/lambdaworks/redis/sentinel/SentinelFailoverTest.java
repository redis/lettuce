/*
 * Copyright 2011-2016 the original author or authors.
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
package com.lambdaworks.redis.sentinel;

import static com.google.code.tempusfugit.temporal.Duration.seconds;
import static com.lambdaworks.Delay.delay;
import static com.lambdaworks.redis.TestSettings.port;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.*;

import com.lambdaworks.TestClientResources;
import com.lambdaworks.redis.FastShutdown;
import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.TestSettings;
import com.lambdaworks.redis.api.sync.RedisCommands;

/**
 * @author Mark Paluch
 */
@Ignore("For manual runs only. Fails too often due to slow sentinel sync")
public class SentinelFailoverTest extends AbstractSentinelTest {

    @Rule
    public SentinelRule sentinelRule = new SentinelRule(sentinelClient, false, 26379, 26380);

    @BeforeClass
    public static void setupClient() {
        sentinelClient = RedisClient.create(TestClientResources.get(),
                RedisURI.Builder.sentinel(TestSettings.host(), 26380, MASTER_ID).build());
    }

    @Before
    public void openConnection() throws Exception {
        sentinel = sentinelClient.connectSentinelAsync().getStatefulConnection().sync();
        sentinelRule.needMasterWithSlave(MASTER_ID, port(3), port(4));
    }

    @Test
    public void connectToRedisUsingSentinel() throws Exception {

        RedisCommands<String, String> connect = sentinelClient.connect().sync();
        assertThat(connect.ping()).isEqualToIgnoringCase("PONG");

        connect.close();
    }

    @Test
    public void failover() throws Exception {

        RedisClient redisClient = RedisClient.create(TestClientResources.get(),
                RedisURI.Builder.redis(TestSettings.host(), port(3)).build());

        String tcpPort1 = connectUsingSentinelAndGetPort();

        sentinelRule.waitForConnectedSlaves(MASTER_ID);
        sentinel.failover(MASTER_ID);

        delay(seconds(5));

        sentinelRule.waitForConnectedSlaves(MASTER_ID);

        String tcpPort2 = connectUsingSentinelAndGetPort();
        assertThat(tcpPort1).isNotEqualTo(tcpPort2);
        FastShutdown.shutdown(redisClient);
    }

    protected String connectUsingSentinelAndGetPort() {
        RedisCommands<String, String> connectAfterFailover = sentinelClient.connect().sync();
        String tcpPort2 = getTcpPort(connectAfterFailover);
        connectAfterFailover.close();
        return tcpPort2;
    }

    protected String getTcpPort(RedisCommands<String, String> commands) {
        Pattern pattern = Pattern.compile(".*tcp_port\\:(\\d+).*", Pattern.DOTALL);

        Matcher matcher = pattern.matcher(commands.info("server"));
        if (matcher.lookingAt()) {
            return matcher.group(1);
        }
        return null;
    }

}
