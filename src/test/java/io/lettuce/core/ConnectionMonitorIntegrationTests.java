package io.lettuce.core;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.metrics.MicrometerConnectionMonitor;
import io.lettuce.core.metrics.MicrometerOptions;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.Wait;
import io.lettuce.test.resource.TestClientResources;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static io.lettuce.core.metrics.MicrometerConnectionMonitor.METRIC_CONNECTION_INACTIVE_TIME;
import static io.lettuce.core.metrics.MicrometerConnectionMonitor.METRIC_CONNECTION_RECONNECTION_ATTEMPTS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Paluch
 */
@Tag(INTEGRATION_TEST)
@ExtendWith(LettuceExtension.class)
class ConnectionMonitorIntegrationTests extends TestSupport {

    private final MeterRegistry meterRegistry = new SimpleMeterRegistry();

    private final ClientResources clientResources = TestClientResources.get();

    @Test
    void metricConnectionInactiveTime() throws InterruptedException {

        MicrometerOptions options = MicrometerOptions.create();
        MicrometerConnectionMonitor monitor = new MicrometerConnectionMonitor(meterRegistry, options);
        ClientResources resources = clientResources.mutate().connectionMonitor(monitor).build();
        RedisClient client = RedisClient.create(resources, RedisURI.Builder.redis(host, port).build());

        try (StatefulRedisConnection<String, String> connection = client.connect()) {
            RedisCommands<String, String> redis = connection.sync();

            // Force disconnection
            redis.quit();
            Wait.untilTrue(() -> !connection.isOpen()).during(Duration.ofSeconds(1)).waitOrTimeout();

            // Wait for successful reconnection
            Wait.untilTrue(() -> connection.isOpen()).during(Duration.ofSeconds(1)).waitOrTimeout();

            // At least one reconnect attempt
            assertThat(meterRegistry.find(METRIC_CONNECTION_RECONNECTION_ATTEMPTS).counter().count()).isGreaterThanOrEqualTo(1);
            assertThat(meterRegistry.find(METRIC_CONNECTION_INACTIVE_TIME).timers()).isNotEmpty();
            assertThat(meterRegistry.find(METRIC_CONNECTION_INACTIVE_TIME).timer().count()).isEqualTo(1);
            assertThat(meterRegistry.find(METRIC_CONNECTION_INACTIVE_TIME).timer().totalTime(TimeUnit.NANOSECONDS))
                    .isGreaterThan(0);
        }
    }

}
