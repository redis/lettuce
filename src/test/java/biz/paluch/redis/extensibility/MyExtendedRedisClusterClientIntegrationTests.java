/*
 * Copyright 2011-2022 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;

import java.util.Collections;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.lettuce.core.RedisURI;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.cluster.RedisAdvancedClusterAsyncCommandsImpl;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.async.RedisAdvancedClusterAsyncCommands;
import io.lettuce.test.resource.FastShutdown;
import io.lettuce.test.settings.TestSettings;

/**
 * Test for override/extensibility of RedisClusterClient
 */
class MyExtendedRedisClusterClientIntegrationTests {

    private static final String host = TestSettings.host();

    private static final int port = TestSettings.port(900);

    private static MyExtendedRedisClusterClient client;

    protected RedisCommands<String, String> redis;

    protected String key = "key";

    protected String value = "value";

    @BeforeAll
    static void setupClient() {
        client = new MyExtendedRedisClusterClient(null, Collections.singletonList(RedisURI.create(host, port)));
    }

    @AfterAll
    static void shutdownClient() {
        FastShutdown.shutdown(client);
    }

    @Test
    void testConnection() throws Exception {

        StatefulRedisClusterConnection<String, String> connection = client.connect();
        RedisAdvancedClusterAsyncCommands<String, String> commands = connection.async();

        assertThat(commands).isInstanceOf(RedisAdvancedClusterAsyncCommandsImpl.class);
        assertThat(commands.getStatefulConnection()).isInstanceOf(MyRedisClusterConnection.class);

        commands.set("key", "value").get();

        connection.close();
    }

}
