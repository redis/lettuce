package io.lettuce.core.failover;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.awaitility.Durations;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.failover.api.StatefulRedisMultiDbConnection;

/**
 * Integration tests for MultiDbClient.
 * <p>
 * These tests were migrated from the original MultiDbClient design and adapted to the current API. Most tests require a running
 * Redis instance as they test actual connection operations.
 * <p>
 * <b>API Differences from Original Tests:</b>
 * <ul>
 * <li>Original: {@code multiDbClient.setActive(uri)} → Current: {@code connection.switchToDatabase(uri)}</li>
 * <li>Original: {@code multiDbClient.getActive()} → Current: {@code connection.getCurrentEndpoint()}</li>
 * <li>Original: {@code multiDbClient.getEndpoints()} returns {@code RedisEndpoints} → Current: returns
 * {@code Iterable<RedisURI>}</li>
 * <li>Original: {@code multiDbClient.addEndpoint(uri)} → Current: {@code connection.addDatabase(uri, weight)}</li>
 * <li>Original: {@code multiDbClient.removeEndpoint(uri)} → Current: {@code connection.removeDatabase(uri)}</li>
 * </ul>
 *
 * @author Mark Paluch (original)
 * @author Ivo Gaydazhiev (original)
 * @author Ali Takavci (adapted)
 */
class MultiDbClientIntegrationTests {

    private MultiDbClient client;

    private StatefulRedisMultiDbConnection<String, String> connection;

    private static final Pattern runIdPattern = Pattern.compile("run_id:([0-9a-f]+)");

    @AfterEach
    void tearDown() {
        if (connection != null) {
            connection.close();
        }
        if (client != null) {
            client.shutdown();
        }
    }

    @Test
    void shouldCreateWithMultipleEndpoints() {
        RedisURI uri1 = MultiDbTestSupport.URI1;
        RedisURI uri2 = MultiDbTestSupport.URI2;

        client = MultiDbClient.create(MultiDbTestSupport.getDatabaseConfigs(uri1, uri2));

        assertThat(client).isNotNull();
    }

    @Test
    void shouldRejectNullDatabaseConfigs() {
        assertThatThrownBy(() -> MultiDbClient.create((List<DatabaseConfig>) null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectEmptyRedisURIs() {
        assertThatThrownBy(() -> MultiDbClient.create(Arrays.asList())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldSetActiveEndpoint() {
        RedisURI uri1 = MultiDbTestSupport.URI1;
        RedisURI uri2 = MultiDbTestSupport.URI2;

        client = MultiDbClient.create(MultiDbTestSupport.getDatabaseConfigs(uri1, uri2));
        connection = client.connect();
        await().atMost(Durations.ONE_SECOND).pollInterval(Durations.ONE_HUNDRED_MILLISECONDS).untilAsserted(() -> {
            assertThat(connection.isHealthy(uri1)).isTrue();
            assertThat(connection.isHealthy(uri2)).isTrue();
        });

        // API CHANGE: Original used multiDbClient.setActive(uri2)
        connection.switchTo(uri2);

        // API CHANGE: Original used multiDbClient.getActive()
        assertThat(connection.getCurrentEndpoint()).isEqualTo(uri2);
    }

    @Test
    void shouldRejectSettingNonExistentEndpointAsActive() {
        RedisURI uri1 = MultiDbTestSupport.URI1;
        RedisURI uri2 = MultiDbTestSupport.URI2;
        RedisURI uri3 = RedisURI.create("redis://localhost:9999");

        client = MultiDbClient.create(MultiDbTestSupport.getDatabaseConfigs(uri1, uri2));
        connection = client.connect();

        // API CHANGE: Original used multiDbClient.setActive(uri3)
        // Note: Current implementation throws IllegalArgumentException for non-existent endpoints
        assertThatThrownBy(() -> connection.switchTo(uri3)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldGetActiveEndpoint() {
        RedisURI uri1 = MultiDbTestSupport.URI1;
        RedisURI uri2 = MultiDbTestSupport.URI2;

        client = MultiDbClient.create(MultiDbTestSupport.getDatabaseConfigs(uri1, uri2));
        connection = client.connect();

        // API CHANGE: Original used multiDbClient.getActive()
        RedisURI active = connection.getCurrentEndpoint();
        assertThat(active).isIn(uri1, uri2);
    }

    @Test
    void shouldAddEndpoint() {
        RedisURI uri1 = MultiDbTestSupport.URI1;
        RedisURI uri2 = MultiDbTestSupport.URI2;

        client = MultiDbClient.create(MultiDbTestSupport.getDatabaseConfigs(uri1));
        connection = client.connect();

        // API CHANGE: Original used multiDbClient.addEndpoint(uri2)
        // Current API: connection.addDatabase(uri2, weight)
        connection.addDatabase(uri2, 1.0f);

        // Verify it was added
        assertThat(StreamSupport.stream(connection.getEndpoints().spliterator(), false).collect(Collectors.toList()))
                .contains(uri2);
    }

    @Test
    void shouldNotAddDuplicateEndpoint() {
        RedisURI uri1 = MultiDbTestSupport.URI1;

        client = MultiDbClient.create(MultiDbTestSupport.getDatabaseConfigs(uri1));
        connection = client.connect();

        // API CHANGE: Original used multiDbClient.addEndpoint(uri1) where uri1 already exists
        // Current API: connection.addDatabase(uri1, weight) should throw exception
        assertThatThrownBy(() -> connection.addDatabase(uri1, 1.0f)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRemoveEndpoint() {
        RedisURI uri1 = MultiDbTestSupport.URI1;
        RedisURI uri2 = MultiDbTestSupport.URI2;

        client = MultiDbClient.create(MultiDbTestSupport.getDatabaseConfigs(uri1, uri2));
        connection = client.connect();

        // Make sure we're not on uri2 before removing it
        connection.switchTo(uri1);

        // API CHANGE: Original used multiDbClient.removeEndpoint(uri2)
        // Current API: connection.removeDatabase(uri2)
        connection.removeDatabase(uri2);

        // Verify it was removed
        assertThat(StreamSupport.stream(connection.getEndpoints().spliterator(), false).collect(Collectors.toList()))
                .doesNotContain(uri2);
    }

    @Test
    void shouldNotRemoveNonExistentEndpoint() {
        RedisURI uri1 = MultiDbTestSupport.URI1;
        RedisURI nonExistent = RedisURI.create("redis://localhost:9999");

        client = MultiDbClient.create(MultiDbTestSupport.getDatabaseConfigs(uri1));
        connection = client.connect();

        // API CHANGE: Original used multiDbClient.removeEndpoint(nonExistent)
        // Current API: connection.removeDatabase(nonExistent) should throw exception
        assertThatThrownBy(() -> connection.removeDatabase(nonExistent)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldNotRemoveLastEndpoint() {
        RedisURI uri1 = MultiDbTestSupport.URI1;

        client = MultiDbClient.create(MultiDbTestSupport.getDatabaseConfigs(uri1));
        connection = client.connect();

        // API CHANGE: Original used multiDbClient.removeEndpoint(uri1) where uri1 is the last endpoint
        // Current API: connection.removeDatabase(uri1) should throw exception
        assertThatThrownBy(() -> connection.removeDatabase(uri1)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldSwitchActiveEndpointDynamically() {
        RedisURI uri1 = MultiDbTestSupport.URI1;
        RedisURI uri2 = MultiDbTestSupport.URI2;

        client = MultiDbClient.create(MultiDbTestSupport.getDatabaseConfigs(uri1, uri2));
        connection = client.connect();

        // API CHANGE: Original used multiDbClient.setActive(uri1)
        connection.switchTo(uri1);
        // API CHANGE: Original used multiDbClient.getActive()
        assertThat(connection.getCurrentEndpoint()).isEqualTo(uri1);

        try (RedisClient c = RedisClient.create()) {
            String initialServerId = extractServerId(connection.sync().info("server"));
            String serverId = extractServerId(c.connect(uri1).sync().info("server"));
            assertThat(serverId).isEqualTo(initialServerId);

            // API CHANGE: Original used multiDbClient.setActive(uri2)
            connection.switchTo(uri2);
            // API CHANGE: Original used multiDbClient.getActive()
            assertThat(connection.getCurrentEndpoint()).isEqualTo(uri2);

            String newServerId = extractServerId(connection.sync().info("server"));
            String secondServerId = extractServerId(c.connect(uri2).sync().info("server"));
            assertThat(secondServerId).isEqualTo(newServerId);
            assertThat(newServerId).isNotEqualTo(initialServerId);
        }
    }

    private String extractServerId(String info) {
        Matcher matcher = runIdPattern.matcher(info);

        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    @Test
    void shouldAddEndpointAndSetItActive() {
        RedisURI uri1 = MultiDbTestSupport.URI1;
        RedisURI uri2 = MultiDbTestSupport.URI2;

        client = MultiDbClient.create(MultiDbTestSupport.getDatabaseConfigs(uri1));
        connection = client.connect();

        // API CHANGE: Original used multiDbClient.addEndpoint(uri2) then multiDbClient.setActive(uri2)
        // Current API: connection.addDatabase(uri2, weight) then connection.switchToDatabase(uri2)
        connection.addDatabase(uri2, 1.0f);
        await().atMost(Durations.TWO_SECONDS).pollInterval(Durations.ONE_HUNDRED_MILLISECONDS).untilAsserted(() -> {
            assertThat(connection.isHealthy(uri2)).isTrue();
        });
        connection.switchTo(uri2);

        // Verify it's active
        assertThat(connection.getCurrentEndpoint()).isEqualTo(uri2);
    }

    @Test
    void shouldNotRemoveActiveEndpoint() {
        RedisURI uri1 = MultiDbTestSupport.URI1;
        RedisURI uri2 = MultiDbTestSupport.URI2;

        client = MultiDbClient.create(MultiDbTestSupport.getDatabaseConfigs(uri1, uri2));
        connection = client.connect();

        // Make sure we're on uri1
        connection.switchTo(uri1);

        // API CHANGE: Original used multiDbClient.removeEndpoint(uri1) where uri1 is active
        // Current API: connection.removeDatabase(uri1) should throw exception
        assertThatThrownBy(() -> connection.removeDatabase(uri1)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldGetEndpointsObject() {
        RedisURI uri1 = MultiDbTestSupport.URI1;
        RedisURI uri2 = MultiDbTestSupport.URI2;

        client = MultiDbClient.create(MultiDbTestSupport.getDatabaseConfigs(uri1, uri2));
        connection = client.connect();

        // API CHANGE: Original returned RedisEndpoints object with methods like getAll(), size(), etc.
        // Current API returns Iterable<RedisURI>
        Iterable<RedisURI> endpoints = connection.getEndpoints();
        assertThat(endpoints).isNotNull();

        // Convert to list to check contents
        List<RedisURI> endpointList = StreamSupport.stream(endpoints.spliterator(), false).collect(Collectors.toList());
        assertThat(endpointList).hasSize(2);
        assertThat(endpointList).containsExactlyInAnyOrder(uri1, uri2);
    }

}
