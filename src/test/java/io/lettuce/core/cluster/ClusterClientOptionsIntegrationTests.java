package io.lettuce.core.cluster;

import static org.assertj.core.api.Assertions.*;

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
