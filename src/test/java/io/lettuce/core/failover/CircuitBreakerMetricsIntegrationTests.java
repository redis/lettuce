package io.lettuce.core.failover;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.inject.Inject;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.RedisURI;
import io.lettuce.core.failover.api.StatefulRedisMultiDbConnection;
import io.lettuce.test.LettuceExtension;

/**
 * Integration tests for circuit breaker metrics tracking in multi-database connections.
 *
 * @author Ali Takavci
 * @since 7.1
 */
@ExtendWith(LettuceExtension.class)
@Tag("integration")
class CircuitBreakerMetricsIntegrationTests extends MultiDbTestSupport {

    @Inject
    CircuitBreakerMetricsIntegrationTests(MultiDbClient client) {
        super(client);
    }

    @Test
    void shouldTrackSuccessfulCommands() {
        StatefulRedisMultiDbConnection<String, String> connection = multiDbClient.connect();
        RedisURI endpoint = connection.getCurrentEndpoint();

        // Execute successful command
        connection.sync().set("key", "value");

        // Get metrics
        CircuitBreaker cb = connection.getCircuitBreaker(endpoint);
        assertNotNull(cb);
        assertThat(cb.getMetrics().getSnapshot().getSuccessCount()).isGreaterThanOrEqualTo(1);
        assertThat(cb.getMetrics().getSnapshot().getFailureCount()).isEqualTo(0);

        connection.close();
    }

    @Test
    void shouldTrackMultipleCommands() {
        StatefulRedisMultiDbConnection<String, String> connection = multiDbClient.connect();
        RedisURI endpoint = connection.getCurrentEndpoint();

        // Execute multiple commands
        connection.sync().set("key1", "value1");
        connection.sync().set("key2", "value2");
        connection.sync().get("key1");

        // Get metrics
        CircuitBreaker cb = connection.getCircuitBreaker(endpoint);
        assertThat(cb.getMetrics().getSnapshot().getSuccessCount()).isGreaterThanOrEqualTo(3);
        assertThat(cb.getMetrics().getSnapshot().getFailureCount()).isEqualTo(0);

        connection.close();
    }

    @Test
    void shouldIsolatMetricsPerEndpoint() {
        StatefulRedisMultiDbConnection<String, String> connection = multiDbClient.connect();
        List<RedisURI> endpoints = StreamSupport.stream(connection.getEndpoints().spliterator(), false)
                .collect(Collectors.toList());

        // Execute command on first endpoint
        connection.sync().set("key1", "value1");
        RedisURI firstEndpoint = connection.getCurrentEndpoint();

        // Switch to second endpoint
        RedisURI secondEndpoint = endpoints.stream().filter(uri -> !uri.equals(firstEndpoint)).findFirst()
                .orElseThrow(() -> new IllegalStateException("No second endpoint found"));
        connection.switchToDatabase(secondEndpoint);

        // Execute command on second endpoint
        connection.sync().set("key2", "value2");

        // Get metrics for both endpoints
        CircuitBreaker cb1 = connection.getCircuitBreaker(firstEndpoint);
        CircuitBreaker cb2 = connection.getCircuitBreaker(secondEndpoint);

        // Verify isolation - each endpoint has its own metrics
        assertThat(cb1.getMetrics().getSnapshot().getSuccessCount()).isGreaterThanOrEqualTo(1);
        assertThat(cb2.getMetrics().getSnapshot().getSuccessCount()).isGreaterThanOrEqualTo(1);

        connection.close();
    }

    @Test
    void shouldThrowExceptionForUnknownEndpoint() {
        StatefulRedisMultiDbConnection<String, String> connection = multiDbClient.connect();

        RedisURI unknownEndpoint = RedisURI.create("redis://unknown:9999");

        assertThatThrownBy(() -> connection.getCircuitBreaker(unknownEndpoint)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown endpoint");

        connection.close();
    }

    @Test
    void shouldMaintainMetricsAfterSwitch() {
        StatefulRedisMultiDbConnection<String, String> connection = multiDbClient.connect();
        RedisURI firstEndpoint = connection.getCurrentEndpoint();

        // Execute command on first endpoint
        connection.sync().set("key1", "value1");
        CircuitBreaker cb1Before = connection.getCircuitBreaker(firstEndpoint);
        long successes1Before = cb1Before.getMetrics().getSnapshot().getSuccessCount();

        // Switch to second endpoint
        List<RedisURI> endpoints = StreamSupport.stream(connection.getEndpoints().spliterator(), false)
                .collect(Collectors.toList());
        RedisURI secondEndpoint = endpoints.stream().filter(uri -> !uri.equals(firstEndpoint)).findFirst()
                .orElseThrow(() -> new IllegalStateException("No second endpoint found"));
        connection.switchToDatabase(secondEndpoint);

        // Execute command on second endpoint
        connection.sync().set("key2", "value2");

        // Switch back to first endpoint
        connection.switchToDatabase(firstEndpoint);

        // Verify metrics for first endpoint are unchanged
        CircuitBreaker cb1After = connection.getCircuitBreaker(firstEndpoint);
        assertThat(cb1After.getMetrics().getSnapshot().getSuccessCount()).isEqualTo(successes1Before);

        connection.close();
    }

    @Test
    void shouldExposeMetricsViaCircuitBreaker() {
        StatefulRedisMultiDbConnection<String, String> connection = multiDbClient.connect();
        RedisURI endpoint = connection.getCurrentEndpoint();

        // Execute commands
        connection.sync().set("key", "value");
        connection.sync().get("key");

        // Get circuit breaker and verify metrics are accessible
        CircuitBreaker cb = connection.getCircuitBreaker(endpoint);
        assertNotNull(cb);
        assertNotNull(cb.getMetrics());
        assertThat(cb.getMetrics().getSnapshot().getSuccessCount()).isGreaterThanOrEqualTo(2);

        connection.close();
    }

}
