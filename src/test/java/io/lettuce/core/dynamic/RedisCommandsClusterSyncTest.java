/*
 * Copyright 2017-2018 the original author or authors.
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
package io.lettuce.core.dynamic;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;

import org.junit.*;

import io.lettuce.core.RedisURI;
import io.lettuce.core.TestSupport;
import io.lettuce.core.Value;
import io.lettuce.core.cluster.ClusterRule;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.dynamic.annotation.Command;
import io.lettuce.core.dynamic.domain.Timeout;
import io.lettuce.core.internal.LettuceLists;
import io.lettuce.test.resource.FastShutdown;
import io.lettuce.test.resource.TestClientResources;

/**
 * @author Mark Paluch
 */
public class RedisCommandsClusterSyncTest extends TestSupport {

    private static final int port1 = 7379;
    private static final int port2 = port1 + 1;
    private static final int port3 = port1 + 2;
    private static final int port4 = port1 + 3;

    private static RedisClusterClient clusterClient;

    @Rule
    public ClusterRule clusterRule = new ClusterRule(clusterClient, port1, port2, port3, port4);

    private StatefulRedisClusterConnection<String, String> connection;

    @BeforeClass
    public static void setupClusterClient() {
        clusterClient = RedisClusterClient.create(TestClientResources.get(),
                LettuceLists.unmodifiableList(RedisURI.Builder.redis(host, port1).build()));
    }

    @AfterClass
    public static void shutdownClusterClient() {
        FastShutdown.shutdown(clusterClient);
    }

    @Before
    public void before() {
        connection = clusterClient.connect();
    }

    @After
    public void tearDown() {
        connection.close();
    }

    @Test
    public void sync() {

        RedisCommandFactory factory = new RedisCommandFactory(connection);

        SynchronousCommands api = factory.getCommands(SynchronousCommands.class);

        api.setSync(key, value, Timeout.create(Duration.ofSeconds(10)));
        assertThat(api.get("key")).isEqualTo("value");
        assertThat(api.getAsBytes("key")).isEqualTo("value".getBytes());
    }

    @Test
    public void shouldRouteBinaryKey() {

        connection.sync().set(key, value);

        RedisCommandFactory factory = new RedisCommandFactory(connection);

        SynchronousCommands api = factory.getCommands(SynchronousCommands.class);

        assertThat(api.get(key.getBytes())).isEqualTo(value.getBytes());
    }

    @Test
    public void mgetAsValues() {

        connection.sync().set(key, value);

        RedisCommandFactory factory = new RedisCommandFactory(connection);

        SynchronousCommands api = factory.getCommands(SynchronousCommands.class);

        List<Value<String>> values = api.mgetAsValues(key);
        assertThat(values).hasSize(1);
        assertThat(values.get(0)).isEqualTo(Value.just(value));
    }

    interface SynchronousCommands extends Commands {

        byte[] get(byte[] key);

        String get(String key);

        @Command("GET")
        byte[] getAsBytes(String key);

        @Command("SET")
        String setSync(String key, String value, Timeout timeout);

        @Command("MGET")
        List<Value<String>> mgetAsValues(String... keys);
    }

}
