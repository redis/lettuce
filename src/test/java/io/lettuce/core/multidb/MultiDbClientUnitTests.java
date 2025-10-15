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

    private RedisClient redisClient;

    private RedisURI endpoint1;

    private RedisURI endpoint2;

    private RedisURI endpoint3;

    private Set<RedisURI> endpoints = LettuceSets.unmodifiableSet(endpoint1, endpoint2, endpoint3);

    @BeforeEach
    void setUp() {
        redisClient = RedisClient.create();
        endpoint1 = RedisURI.create("redis://localhost:6379");
        endpoint2 = RedisURI.create("redis://localhost:6380");
        endpoint3 = RedisURI.create("redis://localhost:6381");
    }

    @AfterEach
    void tearDown() {
        if (redisClient != null) {
            redisClient.shutdown();
        }
    }


    @Test
    void shouldCreateWithMultipleEndpoints() {
        MultiDbClient multiDbClient = MultiDbClient.create(redisClient, Arrays.asList(endpoint1, endpoint2, endpoint3));

        assertThat(multiDbClient.getEndpoints().size()).isEqualTo(3);
        assertThat(multiDbClient.getActive()).isEqualTo(endpoint1);
    }

    @Test
    void shouldCreateWithRedisEndpoints() {
        RedisEndpoints endpoints = RedisEndpoints.create(Arrays.asList(endpoint1, endpoint2));

        MultiDbClient multiDbClient = MultiDbClient.create(redisClient, endpoints);

        assertThat(multiDbClient.getEndpoints()).isSameAs(endpoints);
        assertThat(multiDbClient.getActive()).isEqualTo(endpoint1);
    }

    @Test
    void shouldRejectNullRedisClient() {
        assertThatThrownBy(() -> MultiDbClient.create(null, Arrays.asList(endpoint1, endpoint2, endpoint3))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("RedisClient must not be null");
    }


    @Test
    void shouldRejectNullRedisEndpoints() {
        assertThatThrownBy(() -> MultiDbClient.create(redisClient, (RedisEndpoints) null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("RedisEndpoints must not be null");
    }

    @Test
    void shouldSetActiveEndpoint() {
        MultiDbClient multiDbClient = MultiDbClient.create(redisClient, Arrays.asList(endpoint1, endpoint2, endpoint3));

        multiDbClient.setActive(endpoint2);

        assertThat(multiDbClient.getActive()).isEqualTo(endpoint2);
    }

    @Test
    void shouldRejectSettingNonExistentEndpointAsActive() {
        MultiDbClient multiDbClient = MultiDbClient.create(redisClient, Collections.singletonList(endpoint1));

        assertThatThrownBy(() -> multiDbClient.setActive(endpoint2)).isInstanceOf(RedisException.class)
                .hasMessageContaining("is not available");
    }

    @Test
    void shouldGetActiveEndpoint() {
        MultiDbClient multiDbClient = MultiDbClient.create(redisClient, Arrays.asList(endpoint1, endpoint2, endpoint3));

        RedisURI active = multiDbClient.getActive();

        assertThat(active).isEqualTo(endpoint1);
    }

    @Test
    void shouldAddEndpoint() {
        MultiDbClient multiDbClient = MultiDbClient.create(redisClient, Collections.singletonList(endpoint1));

        boolean added = multiDbClient.addEndpoint(endpoint2);

        assertThat(added).isTrue();
        assertThat(multiDbClient.getEndpoints().size()).isEqualTo(2);
        assertThat(multiDbClient.getEndpoints().contains(endpoint2)).isTrue();
    }

    @Test
    void shouldNotAddDuplicateEndpoint() {
        MultiDbClient multiDbClient = MultiDbClient.create(redisClient, Collections.singletonList(endpoint1));

        boolean added = multiDbClient.addEndpoint(endpoint1);

        assertThat(added).isFalse();
        assertThat(multiDbClient.getEndpoints().size()).isEqualTo(1);
    }

    @Test
    void shouldRemoveEndpoint() {
        MultiDbClient multiDbClient = MultiDbClient.create(redisClient, Arrays.asList(endpoint1, endpoint2));

        boolean removed = multiDbClient.removeEndpoint(endpoint2);

        assertThat(removed).isTrue();
        assertThat(multiDbClient.getEndpoints().size()).isEqualTo(1);
        assertThat(multiDbClient.getEndpoints().contains(endpoint2)).isFalse();
    }

    @Test
    void shouldNotRemoveNonExistentEndpoint() {
        MultiDbClient multiDbClient = MultiDbClient.create(redisClient, Collections.singletonList(endpoint1));

        boolean removed = multiDbClient.removeEndpoint(endpoint2);

        assertThat(removed).isFalse();
        assertThat(multiDbClient.getEndpoints().size()).isEqualTo(1);
    }

    @Test
    void shouldNotRemoveLastEndpoint() {
        MultiDbClient multiDbClient = MultiDbClient.create(redisClient, Collections.singletonList(endpoint1));

        assertThatThrownBy(() -> multiDbClient.removeEndpoint(endpoint1)).isInstanceOf(RedisException.class)
                .hasMessageContaining("Cannot remove the last endpoint");
    }

    @Test
    void shouldSwitchActiveEndpointDynamically() {
        MultiDbClient multiDbClient = MultiDbClient.create(redisClient, Arrays.asList(endpoint1, endpoint2, endpoint3));

        // Initially active is endpoint1
        assertThat(multiDbClient.getActive()).isEqualTo(endpoint1);

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
        MultiDbClient multiDbClient = MultiDbClient.create(redisClient, Collections.singletonList(endpoint1));

        // Add new endpoint
        multiDbClient.addEndpoint(endpoint2);

        // Set it as active
        multiDbClient.setActive(endpoint2);

        assertThat(multiDbClient.getActive()).isEqualTo(endpoint2);
        assertThat(multiDbClient.getEndpoints().size()).isEqualTo(2);
    }

    @Test
    void shouldHandleRemovingActiveEndpoint() {
        MultiDbClient multiDbClient = MultiDbClient.create(redisClient, Arrays.asList(endpoint1, endpoint2));
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
        MultiDbClient multiDbClient = MultiDbClient.create(redisClient, Arrays.asList(endpoint1, endpoint2));

        RedisEndpoints endpoints = multiDbClient.getEndpoints();

        assertThat(endpoints).isNotNull();
        assertThat(endpoints.size()).isEqualTo(2);
        assertThat(endpoints.getEndpoints()).containsExactlyInAnyOrder(endpoint1, endpoint2);
    }

}
