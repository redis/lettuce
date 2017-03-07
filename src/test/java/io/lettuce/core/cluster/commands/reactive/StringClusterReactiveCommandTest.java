/*
 * Copyright 2011-2017 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import io.lettuce.core.FastShutdown;
import io.lettuce.core.KeyValue;
import io.lettuce.core.RedisURI;
import io.lettuce.core.TestSettings;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.cluster.ClusterTestUtil;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.reactive.RedisAdvancedClusterReactiveCommands;
import io.lettuce.core.commands.StringCommandTest;
import io.lettuce.util.ReactiveSyncInvocationHandler;

/**
 * @author Mark Paluch
 */
public class StringClusterReactiveCommandTest extends StringCommandTest {
    private static RedisClusterClient redisClusterClient;
    private StatefulRedisClusterConnection<String, String> clusterConnection;

    @BeforeClass
    public static void setupClient() {
        redisClusterClient = RedisClusterClient.create(RedisURI.Builder.redis(TestSettings.host(), TestSettings.port(900))
                .build());
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
    public void mget() throws Exception {

        redis.set(key, value);
        redis.set("key1", value);
        redis.set("key2", value);

        RedisAdvancedClusterReactiveCommands<String, String> reactive = clusterConnection.reactive();

        Flux<KeyValue<String, String>> mget = reactive.mget(key, "key1", "key2");
        StepVerifier.create(mget.next()).expectNext(KeyValue.just(key, value)).verifyComplete();
    }
}
