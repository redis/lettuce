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
package io.lettuce.core.cluster.commands;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.lettuce.core.AbstractRedisClientTest;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.cluster.ClusterTestUtil;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.test.condition.EnabledOnCommand;
import io.lettuce.test.resource.FastShutdown;
import io.lettuce.test.resource.TestClientResources;
import io.lettuce.test.settings.TestSettings;

/**
 * @author Mark Paluch
 */
public class KeyClusterCommandTest extends AbstractRedisClientTest {

    private static RedisClusterClient redisClusterClient;
    private StatefulRedisClusterConnection<String, String> clusterConnection;

    @BeforeAll
    public static void setupClient() {
        redisClusterClient = RedisClusterClient.create(TestClientResources.get(),
                RedisURI.Builder.redis(TestSettings.host(), TestSettings.port(900)).build());
    }

    @AfterAll
    static void closeClient() {
        FastShutdown.shutdown(redisClusterClient);
    }

    @BeforeEach
    public void openConnection() {
        redis = connect();
        ClusterTestUtil.flushDatabaseOfAllNodes(clusterConnection);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected RedisCommands<String, String> connect() {
        clusterConnection = redisClusterClient.connect();
        return ClusterTestUtil.redisCommandsOverCluster(clusterConnection);
    }

    @Test
    void del() {

        redis.set(key, "value");
        redis.set("a", "value");
        redis.set("b", "value");

        assertThat(redis.del(key, "a", "b")).isEqualTo(3);
        assertThat(redis.exists(key)).isEqualTo(0);
        assertThat(redis.exists("a")).isEqualTo(0);
        assertThat(redis.exists("b")).isEqualTo(0);
    }

    @Test
    void exists() {

        assertThat(redis.exists(key, "a", "b")).isEqualTo(0);

        redis.set(key, "value");
        redis.set("a", "value");
        redis.set("b", "value");

        assertThat(redis.exists(key, "a", "b")).isEqualTo(3);
    }

    @Test
    @EnabledOnCommand("TOUCH")
    void touch() {

        redis.set(key, "value");
        redis.set("a", "value");
        redis.set("b", "value");

        assertThat(redis.touch(key, "a", "b")).isEqualTo(3);
        assertThat(redis.exists(key, "a", "b")).isEqualTo(3);
    }

    @Test
    @EnabledOnCommand("UNLINK")
    void unlink() {

        redis.set(key, "value");
        redis.set("a", "value");
        redis.set("b", "value");

        assertThat(redis.unlink(key, "a", "b")).isEqualTo(3);
        assertThat(redis.exists(key)).isEqualTo(0);
    }

}
