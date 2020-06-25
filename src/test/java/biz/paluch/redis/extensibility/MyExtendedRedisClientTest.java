/*
 * Copyright 2011-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package biz.paluch.redis.extensibility;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.lettuce.core.RedisURI;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.pubsub.RedisPubSubAsyncCommandsImpl;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands;
import io.lettuce.test.resource.FastShutdown;
import io.lettuce.test.settings.TestSettings;

/**
 * Test for override/extensability of RedisClient
 */
class MyExtendedRedisClientTest {

    private static final String host = TestSettings.host();

    private static final int port = TestSettings.port();

    private static MyExtendedRedisClient client;

    protected RedisCommands<String, String> redis;

    protected String key = "key";

    protected String value = "value";

    @BeforeAll
    static void setupClient() {
        client = getRedisClient();
    }

    static MyExtendedRedisClient getRedisClient() {
        return new MyExtendedRedisClient(null, RedisURI.create(host, port));
    }

    @AfterAll
    static void shutdownClient() {
        FastShutdown.shutdown(client);
    }

    @Test
    void testPubsub() throws Exception {
        StatefulRedisPubSubConnection<String, String> connection = client.connectPubSub();
        RedisPubSubAsyncCommands<String, String> commands = connection.async();
        assertThat(commands).isInstanceOf(RedisPubSubAsyncCommandsImpl.class);
        assertThat(commands.getStatefulConnection()).isInstanceOf(MyPubSubConnection.class);
        commands.set("key", "value").get();
        connection.close();
    }

}
