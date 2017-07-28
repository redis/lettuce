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

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.lambdaworks.TestClientResources;
import com.lambdaworks.redis.FastShutdown;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.TestSettings;
import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.cluster.ClusterTestUtil;
import com.lambdaworks.redis.cluster.RedisClusterClient;
import com.lambdaworks.redis.cluster.api.StatefulRedisClusterConnection;
import com.lambdaworks.redis.commands.ListCommandTest;

/**
 * @author Mark Paluch
 */
public class ListClusterCommandTest extends ListCommandTest {
    private static RedisClusterClient redisClusterClient;
    private StatefulRedisClusterConnection<String, String> clusterConnection;

    @BeforeClass
    public static void setupClient() {
        redisClusterClient = RedisClusterClient.create(TestClientResources.get(),
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
        clusterConnection = redisClusterClient.connectCluster().getStatefulConnection();
        return ClusterTestUtil.redisCommandsOverCluster(clusterConnection);
    }

    // re-implementation because keys have to be on the same slot
    @Test
    public void brpoplpush() throws Exception {

        redis.rpush("UKPDHs8Zlp", "1", "2");
        redis.rpush("br7EPz9bbj", "3", "4");
        assertThat(redis.brpoplpush(1, "UKPDHs8Zlp", "br7EPz9bbj")).isEqualTo("2");
        assertThat(redis.lrange("UKPDHs8Zlp", 0, -1)).isEqualTo(list("1"));
        assertThat(redis.lrange("br7EPz9bbj", 0, -1)).isEqualTo(list("2", "3", "4"));
    }

    @Test
    public void brpoplpushTimeout() throws Exception {
        assertThat(redis.brpoplpush(1, "UKPDHs8Zlp", "br7EPz9bbj")).isNull();
    }

    @Test
    public void blpop() throws Exception {
        redis.rpush("br7EPz9bbj", "2", "3");
        assertThat(redis.blpop(1, "UKPDHs8Zlp", "br7EPz9bbj")).isEqualTo(kv("br7EPz9bbj", "2"));
    }

    @Test
    public void brpop() throws Exception {
        redis.rpush("br7EPz9bbj", "2", "3");
        assertThat(redis.brpop(1, "UKPDHs8Zlp", "br7EPz9bbj")).isEqualTo(kv("br7EPz9bbj", "3"));
    }

    @Test
    public void rpoplpush() throws Exception {
        assertThat(redis.rpoplpush("UKPDHs8Zlp", "br7EPz9bbj")).isNull();
        redis.rpush("UKPDHs8Zlp", "1", "2");
        redis.rpush("br7EPz9bbj", "3", "4");
        assertThat(redis.rpoplpush("UKPDHs8Zlp", "br7EPz9bbj")).isEqualTo("2");
        assertThat(redis.lrange("UKPDHs8Zlp", 0, -1)).isEqualTo(list("1"));
        assertThat(redis.lrange("br7EPz9bbj", 0, -1)).isEqualTo(list("2", "3", "4"));
    }

}
