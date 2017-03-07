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
package io.lettuce.core.cluster.commands.reactive;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import io.lettuce.core.FastShutdown;
import io.lettuce.core.RedisURI;
import io.lettuce.core.TestSettings;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.cluster.ClusterTestUtil;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.commands.ListCommandTest;
import io.lettuce.util.ReactiveSyncInvocationHandler;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Paluch
 */
public class ListClusterReactiveCommandTest extends ListCommandTest {
    private static RedisClusterClient redisClusterClient;
    private StatefulRedisClusterConnection<String, String> clusterConnection;

    @BeforeClass
    public static void setupClient() {
        redisClusterClient = RedisClusterClient.create(RedisURI.Builder.redis(TestSettings.host(), TestSettings.port(900)).build());
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
    protected RedisCommands<String, String> connect() {
        clusterConnection = redisClusterClient.connect();
        return ReactiveSyncInvocationHandler.sync(redisClusterClient.connect());
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
