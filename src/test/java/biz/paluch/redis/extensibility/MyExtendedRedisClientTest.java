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
package biz.paluch.redis.extensibility;

import static org.assertj.core.api.Assertions.assertThat;

import io.lettuce.core.*;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisStringAsyncCommands;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import io.lettuce.core.support.ConnectionPoolSupport;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.pubsub.RedisPubSubAsyncCommandsImpl;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands;

import java.time.Duration;
import java.util.concurrent.ExecutionException;

/**
 * Test for override/extensability of RedisClient
 */
public class MyExtendedRedisClientTest {
    public static final String host = TestSettings.host();
    public static final int port = TestSettings.port();

    protected static MyExtendedRedisClient client;
    protected RedisCommands<String, String> redis;
    protected String key = "key";
    protected String value = "value";

    @BeforeClass
    public static void setupClient() {
        client = getRedisClient();
    }

    protected static MyExtendedRedisClient getRedisClient() {
        return new MyExtendedRedisClient(null, RedisURI.create(host, port));
    }

    @AfterClass
    public static void shutdownClient() {
        FastShutdown.shutdown(client);
    }

    @Test
    public void testPubsub() throws Exception {
        StatefulRedisPubSubConnection<String, String> connection = client
                .connectPubSub();
        RedisPubSubAsyncCommands<String, String> commands = connection.async();
        assertThat(commands).isInstanceOf(RedisPubSubAsyncCommandsImpl.class);
        assertThat(commands.getStatefulConnection()).isInstanceOf(MyPubSubConnection.class);
        String res = commands.set("key", "value").get();
        System.out.println( res );
        connection.close();
    }

    @Test
    public void testGet() throws ExecutionException, InterruptedException {
        RedisURI rediUri = new RedisURI("10.16.46.172", 8008, Duration.ofSeconds(5000));
        RedisClusterClient clusterClient = RedisClusterClient.create(rediUri);
        StatefulRedisClusterConnection<String, String> connection = clusterClient.connect();
        connection.setReadFrom(ReadFrom.SLAVE_PREFERRED);
        RedisAdvancedClusterCommands<String, String> syncCommands = connection.sync();

        String set = syncCommands.set("key1", "value1");
        String get = syncCommands.get("key1");
        System.out.println( get );
    }

    @Test
    public void testPool() throws Exception {
        RedisURI rediUri = new RedisURI("10.16.46.172", 8008, Duration.ofSeconds(5000));
        ClientResources res = DefaultClientResources.create();
        RedisClusterClient clusterClient = RedisClusterClient.create(res, rediUri);

        GenericObjectPool<StatefulRedisClusterConnection<String, String>> pool = ConnectionPoolSupport
                .createGenericObjectPool(() -> clusterClient.connect(), new GenericObjectPoolConfig());
        try (StatefulRedisClusterConnection<String, String> connection = pool.borrowObject()) {
            RedisAdvancedClusterCommands<String, String> commands = connection.sync();
            commands.set("keyp1", "value");
            commands.set("keyp2", "value2");
            commands.get("fdas");
        }
        pool.close();
        client.shutdown();
    }

}
