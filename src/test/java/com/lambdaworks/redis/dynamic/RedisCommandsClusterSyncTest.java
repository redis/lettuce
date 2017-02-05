/*
 * Copyright 2017 the original author or authors.
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
package com.lambdaworks.redis.dynamic;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.*;

import com.lambdaworks.TestClientResources;
import com.lambdaworks.redis.AbstractTest;
import com.lambdaworks.redis.FastShutdown;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.Value;
import com.lambdaworks.redis.cluster.ClusterRule;
import com.lambdaworks.redis.cluster.RedisClusterClient;
import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;
import com.lambdaworks.redis.dynamic.annotation.Command;
import com.lambdaworks.redis.dynamic.domain.Timeout;
import com.lambdaworks.redis.internal.LettuceLists;

/**
 * @author Mark Paluch
 */
public class RedisCommandsClusterSyncTest extends AbstractTest {

    public static final int port1 = 7379;
    public static final int port2 = port1 + 1;
    public static final int port3 = port1 + 2;
    public static final int port4 = port1 + 3;

    protected static RedisClusterClient clusterClient;

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

        api.setSync(key, value, Timeout.create(10, TimeUnit.SECONDS));
        assertThat(api.get("key")).isEqualTo("value");
        assertThat(api.getAsBytes("key")).isEqualTo("value".getBytes());
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

    static interface SynchronousCommands extends Commands {

        String get(String key);

        @Command("GET")
        byte[] getAsBytes(String key);

        @Command("SET")
        String setSync(String key, String value, Timeout timeout);

        @Command("MGET")
        List<Value<String>> mgetAsValues(String... keys);
    }

}
