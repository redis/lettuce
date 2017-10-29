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
package io.lettuce.core.cluster;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import reactor.test.StepVerifier;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.ScanStream;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.reactive.RedisAdvancedClusterReactiveCommands;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;

/**
 * @author Mark Paluch
 */
public class ScanStreamTest extends AbstractClusterTest {

    private StatefulRedisClusterConnection<String, String> connection;
    private RedisAdvancedClusterCommands<String, String> redis;

    @Before
    public void before() throws Exception {

        this.connection = clusterClient.connect();
        this.redis = this.connection.sync();
        this.redis.flushall();
    }

    @After
    public void tearDown() {
        this.connection.close();
    }

    @Test
    public void shouldScanIteratively() {

        for (int i = 0; i < 1000; i++) {
            redis.set("key-" + i, value);
        }

        RedisAdvancedClusterReactiveCommands<String, String> reactive = connection.reactive();

        StepVerifier.create(ScanStream.scan(reactive, ScanArgs.Builder.limit(200)).take(250)).expectNextCount(250)
                .verifyComplete();
        StepVerifier.create(ScanStream.scan(reactive)).expectNextCount(1000).verifyComplete();
    }
}
