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
package io.lettuce.core.cluster.commands;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;

import io.lettuce.TestClientResources;
import io.lettuce.core.*;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.cluster.ClusterTestUtil;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.commands.StringCommandTest;

/**
 * @author Mark Paluch
 */
public class StringClusterCommandTest extends StringCommandTest {
    private static RedisClusterClient redisClusterClient;
    private StatefulRedisClusterConnection<String, String> clusterConnection;

    @BeforeClass
    public static void setupClient() {
        redisClusterClient = RedisClusterClient.create(
                TestClientResources.get(), RedisURI.Builder.redis(TestSettings.host(), TestSettings.port(900)).build());
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
    public void msetnx() throws Exception {
        redis.set("one", "1");
        Map<String, String> map = new LinkedHashMap<>();
        map.put("one", "1");
        map.put("two", "2");
        assertThat(redis.msetnx(map)).isFalse();
        redis.del("one");
        redis.del("two"); // probably set on a different node
        assertThat(redis.msetnx(map)).isTrue();
        assertThat(redis.get("two")).isEqualTo("2");
    }

    @Test
    public void mgetStreaming() throws Exception {
        setupMget();

        KeyValueStreamingAdapter<String, String> streamingAdapter = new KeyValueStreamingAdapter<>();
        Long count = redis.mget(streamingAdapter, "one", "two");

        assertThat(streamingAdapter.getMap()).containsEntry("one", "1").containsEntry("two", "2");

        assertThat(count.intValue()).isEqualTo(2);
    }
}
