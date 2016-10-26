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
package biz.paluch.redis.extensibility;

import static org.assertj.core.api.Assertions.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.lambdaworks.redis.FastShutdown;
import com.lambdaworks.redis.RedisConnection;
import com.lambdaworks.redis.TestSettings;
import com.lambdaworks.redis.pubsub.RedisPubSubAsyncCommandsImpl;
import com.lambdaworks.redis.pubsub.api.async.RedisPubSubAsyncCommands;

/**
 * Test for override/extensability of RedisClient
 */
public class MyExtendedRedisClientTest {
    public static final String host = TestSettings.host();
    public static final int port = TestSettings.port();

    protected static MyExtendedRedisClient client;
    protected RedisConnection<String, String> redis;
    protected String key = "key";
    protected String value = "value";

    @BeforeClass
    public static void setupClient() {
        client = getRedisClient();
    }

    protected static MyExtendedRedisClient getRedisClient() {
        return new MyExtendedRedisClient(host, port);
    }

    @AfterClass
    public static void shutdownClient() {
        FastShutdown.shutdown(client);
    }

    @Test
    public void testPubsub() throws Exception {
        RedisPubSubAsyncCommands<String, String> connection = client.connectPubSub().async();
        assertThat(connection).isInstanceOf(RedisPubSubAsyncCommandsImpl.class);
        assertThat(connection.getStatefulConnection()).isInstanceOf(MyPubSubConnection.class);
        connection.set("key", "value").get();
        connection.close();

    }
}
