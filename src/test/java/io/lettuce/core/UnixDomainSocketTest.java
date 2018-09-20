/*
 * Copyright 2011-2018 the original author or authors.
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
package io.lettuce.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.sentinel.SentinelRule;
import io.lettuce.core.sentinel.api.StatefulRedisSentinelConnection;
import io.lettuce.test.resource.FastShutdown;
import io.lettuce.test.resource.TestClientResources;
import io.lettuce.test.settings.TestSettings;
import io.netty.util.internal.SystemPropertyUtil;

/**
 * @author Mark Paluch
 */
public class UnixDomainSocketTest {

    private static final String MASTER_ID = "mymaster";

    private static RedisClient sentinelClient;

    @Rule
    public SentinelRule sentinelRule = new SentinelRule(sentinelClient, false, 26379, 26380);

    private Logger log = LogManager.getLogger(getClass());
    private String key = "key";
    private String value = "value";

    @BeforeClass
    public static void setupClient() {
        sentinelClient = getRedisSentinelClient();
    }

    @AfterClass
    public static void shutdownClient() {
        FastShutdown.shutdown(sentinelClient);
    }

    @Test
    public void standalone_RedisClientWithSocket() throws Exception {

        assumeTestSupported();

        RedisURI redisURI = getSocketRedisUri();

        RedisClient redisClient = RedisClient.create(TestClientResources.get(), redisURI);

        StatefulRedisConnection<String, String> connection = redisClient.connect();
        someRedisAction(connection.sync());
        connection.close();

        FastShutdown.shutdown(redisClient);
    }

    @Test
    public void standalone_ConnectToSocket() throws Exception {

        assumeTestSupported();

        RedisURI redisURI = getSocketRedisUri();

        RedisClient redisClient = RedisClient.create(TestClientResources.get());

        StatefulRedisConnection<String, String> connection = redisClient.connect(redisURI);

        someRedisAction(connection.sync());
        connection.close();

        FastShutdown.shutdown(redisClient);
    }

    @Test
    public void sentinel_RedisClientWithSocket() throws Exception {

        assumeTestSupported();

        RedisURI uri = new RedisURI();
        uri.getSentinels().add(getSentinelSocketRedisUri());
        uri.setSentinelMasterId("mymaster");

        RedisClient redisClient = RedisClient.create(TestClientResources.get(), uri);

        StatefulRedisConnection<String, String> connection = redisClient.connect();

        someRedisAction(connection.sync());

        connection.close();

        StatefulRedisSentinelConnection<String, String> sentinelConnection = redisClient.connectSentinel();

        assertThat(sentinelConnection.sync().ping()).isEqualTo("PONG");
        sentinelConnection.close();

        FastShutdown.shutdown(redisClient);
    }

    @Test
    public void sentinel_ConnectToSocket() throws Exception {

        assumeTestSupported();

        RedisURI uri = new RedisURI();
        uri.getSentinels().add(getSentinelSocketRedisUri());
        uri.setSentinelMasterId("mymaster");

        RedisClient redisClient = RedisClient.create(TestClientResources.get());

        StatefulRedisConnection<String, String> connection = redisClient.connect(uri);

        someRedisAction(connection.sync());

        connection.close();

        StatefulRedisSentinelConnection<String, String> sentinelConnection = redisClient.connectSentinel(uri);

        assertThat(sentinelConnection.sync().ping()).isEqualTo("PONG");
        sentinelConnection.close();

        FastShutdown.shutdown(redisClient);
    }

    @Test
    public void sentinel_socket_and_inet() throws Exception {

        sentinelRule.waitForMaster(MASTER_ID);
        assumeTestSupported();

        RedisURI uri = new RedisURI();
        uri.getSentinels().add(getSentinelSocketRedisUri());
        uri.getSentinels().add(RedisURI.create(RedisURI.URI_SCHEME_REDIS + "://" + TestSettings.host() + ":26379"));
        uri.setSentinelMasterId(MASTER_ID);

        RedisClient redisClient = RedisClient.create(TestClientResources.get(), uri);

        StatefulRedisSentinelConnection<String, String> sentinelConnection = redisClient
                .connectSentinel(getSentinelSocketRedisUri());
        log.info("Masters: " + sentinelConnection.sync().masters());

        try {
            redisClient.connect();
            fail("Missing validation exception");
        } catch (RedisConnectionException e) {
            assertThat(e).hasMessageContaining("You cannot mix unix domain socket and IP socket URI's");
        } finally {
            FastShutdown.shutdown(redisClient);
        }

    }

    private void someRedisAction(RedisCommands<String, String> connection) {
        connection.set(key, value);
        String result = connection.get(key);

        assertThat(result).isEqualTo(value);
    }

    private static RedisClient getRedisSentinelClient() {
        return RedisClient.create(TestClientResources.get(), RedisURI.Builder.sentinel(TestSettings.host(), MASTER_ID).build());
    }

    private void assumeTestSupported() {
        String osName = SystemPropertyUtil.get("os.name").toLowerCase(Locale.UK).trim();
        assumeTrue(Transports.NativeTransports.isSocketSupported(), "Only supported on Linux/OSX, your os is " + osName
                + " with epoll/kqueue support.");
    }

    private static RedisURI getSocketRedisUri() throws IOException {
        File file = new File(TestSettings.socket()).getCanonicalFile();
        return RedisURI.create(RedisURI.URI_SCHEME_REDIS_SOCKET + "://" + file.getCanonicalPath());
    }

    private static RedisURI getSentinelSocketRedisUri() throws IOException {
        File file = new File(TestSettings.sentinelSocket()).getCanonicalFile();
        return RedisURI.create(RedisURI.URI_SCHEME_REDIS_SOCKET + "://" + file.getCanonicalPath());
    }

}
