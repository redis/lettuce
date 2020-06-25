/*
 * Copyright 2018-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.lettuce.core.cluster;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.TestSupport;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.pubsub.StatefulRedisClusterPubSubConnection;
import io.lettuce.test.LettuceExtension;

/**
 * @author Mark Paluch
 */
@ExtendWith(LettuceExtension.class)
class ClusterClientOptionsIntegrationTests extends TestSupport {

    private final RedisClusterClient clusterClient;

    @Inject
    ClusterClientOptionsIntegrationTests(RedisClusterClient clusterClient) {
        this.clusterClient = clusterClient;
    }

    @AfterEach
    void tearDown() {
        clusterClient.setOptions(ClusterClientOptions.create());
    }

    @Test
    void shouldApplyTimeoutOptionsToClusterConnection() throws InterruptedException {

        clusterClient.setOptions(
                ClusterClientOptions.builder().timeoutOptions(TimeoutOptions.enabled(Duration.ofMillis(100))).build());

        try (StatefulRedisClusterConnection<String, String> connection = clusterClient.connect()) {

            connection.setTimeout(Duration.ZERO);
            connection.async().clientPause(300);

            RedisFuture<String> future = connection.async().ping();

            assertThatThrownBy(future::get).isInstanceOf(ExecutionException.class)
                    .hasCauseInstanceOf(RedisCommandTimeoutException.class).hasMessageContaining("100 milli");
        }

        Thread.sleep(300);
    }

    @Test
    void shouldApplyTimeoutOptionsToPubSubClusterConnection() throws InterruptedException {

        clusterClient.setOptions(
                ClusterClientOptions.builder().timeoutOptions(TimeoutOptions.enabled(Duration.ofMillis(100))).build());

        try (StatefulRedisClusterPubSubConnection<String, String> connection = clusterClient.connectPubSub()) {
            connection.setTimeout(Duration.ofMillis(100));

            connection.async().clientPause(300);

            RedisFuture<String> future = connection.async().ping();

            assertThatThrownBy(future::get).isInstanceOf(ExecutionException.class)
                    .hasCauseInstanceOf(RedisCommandTimeoutException.class).hasMessageContaining("100 milli");
        }

        Thread.sleep(300);
    }

}
