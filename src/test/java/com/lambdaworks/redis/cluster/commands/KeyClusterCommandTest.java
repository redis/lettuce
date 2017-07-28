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
package com.lambdaworks.redis.cluster.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.lambdaworks.RedisConditions;
import com.lambdaworks.TestClientResources;
import com.lambdaworks.redis.AbstractRedisClientTest;
import com.lambdaworks.redis.FastShutdown;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.TestSettings;
import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.cluster.ClusterTestUtil;
import com.lambdaworks.redis.cluster.RedisClusterClient;
import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;

/**
 * @author Mark Paluch
 */
public class KeyClusterCommandTest extends AbstractRedisClientTest {

    private static RedisClusterClient redisClusterClient;
    private StatefulRedisClusterConnection<String, String> clusterConnection;

    @BeforeClass
    public static void setupClient() {
        redisClusterClient = RedisClusterClient
.create(TestClientResources.get(),
                RedisURI.Builder.redis(TestSettings.host(), TestSettings.port(900)).build());
    }

    @AfterClass
    public static void closeClient() {
        FastShutdown.shutdown(redisClusterClient);
    }

    @Before
    public void openConnection() throws Exception {
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
    public void del() throws Exception {

        redis.set(key, "value");
        redis.set("a", "value");
        redis.set("b", "value");

        assertThat(redis.del(key, "a", "b")).isEqualTo(3);
        assertThat(redis.exists(key)).isFalse();
        assertThat(redis.exists("a")).isFalse();
        assertThat(redis.exists("b")).isFalse();
    }

    @Test
    public void exists() throws Exception {

        assertThat(redis.exists(key, "a", "b")).isEqualTo(0);

        redis.set(key, "value");
        redis.set("a", "value");
        redis.set("b", "value");

        assertThat(redis.exists(key, "a", "b")).isEqualTo(3);
    }

    @Test
    public void touch() throws Exception {

        assumeTrue(RedisConditions.of(redis).hasCommand("TOUCH"));

        redis.set(key, "value");
        redis.set("a", "value");
        redis.set("b", "value");

        assertThat(redis.touch(key, "a", "b")).isEqualTo(3);
        assertThat(redis.exists(key, "a", "b")).isEqualTo(3);
    }

    @Test
    public void unlink() throws Exception {

        assumeTrue(RedisConditions.of(redis).hasCommand("UNLINK"));

        redis.set(key, "value");
        redis.set("a", "value");
        redis.set("b", "value");

        assertThat(redis.unlink(key, "a", "b")).isEqualTo(3);
        assertThat(redis.exists(key)).isFalse();
    }

}
