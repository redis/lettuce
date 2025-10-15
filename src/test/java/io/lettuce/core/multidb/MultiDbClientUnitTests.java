package io.lettuce.core.multidb;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.internal.LettuceSets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link MultiDbClient}.
 *
 * @author Ivo Gaydazhiev
 */
class MultiDbClientUnitTests {

    private MultiDbClient multiDbClient;

    private RedisURI endpoint1;

    private RedisURI endpoint2;

    private RedisURI endpoint3;

    @BeforeEach
    void setUp() {
        endpoint1 = RedisURI.create("redis://localhost:6379");
        endpoint2 = RedisURI.create("redis://localhost:6380");
        endpoint3 = RedisURI.create("redis://localhost:6381");
    }

    @AfterEach
    void tearDown() {
        if (multiDbClient != null) {
            multiDbClient.shutdown();
        }
    }

    @Test
    void shouldCreateWithMultipleEndpoints() {
        Set<RedisURI> uris = LettuceSets.unmodifiableSet(endpoint1, endpoint2, endpoint3);
        multiDbClient = MultiDbClient.create(uris);

        assertThat(multiDbClient.getEndpoints().size()).isEqualTo(3);
        assertThat(multiDbClient.getActive()).isIn(endpoint1, endpoint2, endpoint3);
    }

    @Test
    void shouldCreateWithSet() {
        Set<RedisURI> uris = LettuceSets.unmodifiableSet(endpoint1, endpoint2);
        multiDbClient = MultiDbClient.create(uris);

        assertThat(multiDbClient.getEndpoints().size()).isEqualTo(2);
        assertThat(multiDbClient.getActive()).isIn(endpoint1, endpoint2);
    }

    @Test
    void shouldRejectNullRedisURIs() {
        assertThatThrownBy(() -> MultiDbClient.create((Set<RedisURI>) null)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("RedisURIs must not be null");
    }

    @Test
    void shouldRejectEmptyRedisURIs() {
        assertThatThrownBy(() -> MultiDbClient.create(Collections.emptySet())).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("RedisURIs must not be empty");
    }

    @Test
    void shouldSetActiveEndpoint() {
        Set<RedisURI> uris = LettuceSets.unmodifiableSet(endpoint1, endpoint2, endpoint3);
        multiDbClient = MultiDbClient.create(uris);

        multiDbClient.setActive(endpoint2);

        assertThat(multiDbClient.getActive()).isEqualTo(endpoint2);
    }

    @Test
    void shouldRejectSettingNonExistentEndpointAsActive() {
        multiDbClient = MultiDbClient.create(Collections.singleton(endpoint1));

        assertThatThrownBy(() -> multiDbClient.setActive(endpoint2)).isInstanceOf(RedisException.class)
                .hasMessageContaining("is not available");
    }

    @Test
    void shouldGetActiveEndpoint() {
        Set<RedisURI> uris = LettuceSets.unmodifiableSet(endpoint1, endpoint2, endpoint3);
        multiDbClient = MultiDbClient.create(uris);

        RedisURI active = multiDbClient.getActive();

        assertThat(active).isIn(endpoint1, endpoint2, endpoint3);
    }

    @Test
    void shouldAddEndpoint() {
        multiDbClient = MultiDbClient.create(Collections.singleton(endpoint1));

        boolean added = multiDbClient.addEndpoint(endpoint2);

        assertThat(added).isTrue();
        assertThat(multiDbClient.getEndpoints().size()).isEqualTo(2);
        assertThat(multiDbClient.getEndpoints().contains(endpoint2)).isTrue();
    }

    @Test
    void shouldNotAddDuplicateEndpoint() {
        multiDbClient = MultiDbClient.create(Collections.singleton(endpoint1));

        boolean added = multiDbClient.addEndpoint(endpoint1);

        assertThat(added).isFalse();
        assertThat(multiDbClient.getEndpoints().size()).isEqualTo(1);
    }

    @Test
    void shouldRemoveEndpoint() {
        Set<RedisURI> uris = LettuceSets.unmodifiableSet(endpoint1, endpoint2);
        multiDbClient = MultiDbClient.create(uris);

        boolean removed = multiDbClient.removeEndpoint(endpoint2);

        assertThat(removed).isTrue();
        assertThat(multiDbClient.getEndpoints().size()).isEqualTo(1);
        assertThat(multiDbClient.getEndpoints().contains(endpoint2)).isFalse();
    }

    @Test
    void shouldNotRemoveNonExistentEndpoint() {
        multiDbClient = MultiDbClient.create(Collections.singleton(endpoint1));

        boolean removed = multiDbClient.removeEndpoint(endpoint2);

        assertThat(removed).isFalse();
        assertThat(multiDbClient.getEndpoints().size()).isEqualTo(1);
    }

    @Test
    void shouldNotRemoveLastEndpoint() {
        multiDbClient = MultiDbClient.create(Collections.singleton(endpoint1));

        assertThatThrownBy(() -> multiDbClient.removeEndpoint(endpoint1)).isInstanceOf(RedisException.class)
                .hasMessageContaining("Cannot remove the last endpoint");
    }

    @Test
    void shouldSwitchActiveEndpointDynamically() {
        Set<RedisURI> uris = LettuceSets.unmodifiableSet(endpoint1, endpoint2, endpoint3);
        multiDbClient = MultiDbClient.create(uris);

        // Get initial active
        RedisURI initialActive = multiDbClient.getActive();
        assertThat(initialActive).isIn(endpoint1, endpoint2, endpoint3);

        // Switch to endpoint2
        multiDbClient.setActive(endpoint2);
        assertThat(multiDbClient.getActive()).isEqualTo(endpoint2);

        // Switch to endpoint3
        multiDbClient.setActive(endpoint3);
        assertThat(multiDbClient.getActive()).isEqualTo(endpoint3);

        // Switch back to endpoint1
        multiDbClient.setActive(endpoint1);
        assertThat(multiDbClient.getActive()).isEqualTo(endpoint1);
    }

    @Test
    void shouldAddEndpointAndSetItActive() {
        multiDbClient = MultiDbClient.create(Collections.singleton(endpoint1));

        // Add new endpoint
        multiDbClient.addEndpoint(endpoint2);

        // Set it as active
        multiDbClient.setActive(endpoint2);

        assertThat(multiDbClient.getActive()).isEqualTo(endpoint2);
        assertThat(multiDbClient.getEndpoints().size()).isEqualTo(2);
    }

    @Test
    void shouldHandleRemovingActiveEndpoint() {
        Set<RedisURI> uris = LettuceSets.unmodifiableSet(endpoint1, endpoint2);
        multiDbClient = MultiDbClient.create(uris);
        multiDbClient.setActive(endpoint1);

        // Remove the active endpoint
        multiDbClient.removeEndpoint(endpoint1);

        // Active should be cleared
        assertThat(multiDbClient.getEndpoints().hasActive()).isFalse();
        assertThatThrownBy(multiDbClient::getActive).isInstanceOf(RedisException.class)
                .hasMessageContaining("No active endpoint is set");
    }

    @Test
    void shouldGetEndpointsObject() {
        Set<RedisURI> uris = LettuceSets.unmodifiableSet(endpoint1, endpoint2);
        multiDbClient = MultiDbClient.create(uris);

        RedisEndpoints endpoints = multiDbClient.getEndpoints();

        assertThat(endpoints).isNotNull();
        assertThat(endpoints.size()).isEqualTo(2);
        assertThat(endpoints.getEndpoints()).containsExactlyInAnyOrder(endpoint1, endpoint2);
    }

}
