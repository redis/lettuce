package io.lettuce.core.failover.health;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.lettuce.core.RedisURI;

/**
 * Unit tests for {@link HealthCheckCollection}.
 *
 * @author Ivo Gaydazhiev
 */
class HealthCheckCollectionUnitTests {

    @Mock
    private HealthCheckStrategy mockStrategy;

    private RedisURI testEndpoint;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        testEndpoint = RedisURI.create("redis://localhost:6379");

        // Default stubs for mockStrategy used across tests
        when(mockStrategy.getNumProbes()).thenReturn(1);
        when(mockStrategy.getDelayInBetweenProbes()).thenReturn(100);
        when(mockStrategy.getPolicy()).thenReturn(ProbingPolicy.BuiltIn.ANY_SUCCESS);
    }

    @Test
    void testAdd() {
        HealthCheckCollection collection = new HealthCheckCollection();
        HealthCheck healthCheck = new HealthCheckImpl(testEndpoint, mockStrategy);

        HealthCheck previous = collection.add(healthCheck);
        assertThat(previous).isNull();

        assertThat(collection.get(testEndpoint)).isEqualTo(healthCheck);
    }

    @Test
    void testRemoveByEndpoint() {
        HealthCheckCollection collection = new HealthCheckCollection();
        HealthCheck healthCheck = new HealthCheckImpl(testEndpoint, mockStrategy);

        collection.add(healthCheck);
        HealthCheck removed = collection.remove(testEndpoint);

        assertThat(removed).isEqualTo(healthCheck);
        assertThat(collection.get(testEndpoint)).isNull();
    }

    @Test
    void testReplacement() {
        HealthCheckCollection collection = new HealthCheckCollection();
        HealthCheck healthCheck1 = new HealthCheckImpl(testEndpoint, mockStrategy);
        HealthCheck healthCheck2 = new HealthCheckImpl(testEndpoint, mockStrategy);

        collection.add(healthCheck1);
        HealthCheck previous = collection.add(healthCheck2);

        assertThat(previous).isEqualTo(healthCheck1);
        assertThat(collection.get(testEndpoint)).isEqualTo(healthCheck2);
    }

    @Test
    void testRemoveByHealthCheck() {
        HealthCheckCollection collection = new HealthCheckCollection();
        HealthCheck healthCheck = new HealthCheckImpl(testEndpoint, mockStrategy);

        collection.add(healthCheck);
        HealthCheck removed = collection.remove(healthCheck);

        assertThat(removed).isEqualTo(healthCheck);
        assertThat(collection.get(testEndpoint)).isNull();
    }

    @Test
    void testClose() {
        HealthCheckCollection collection = new HealthCheckCollection();

        // Create spy health check
        HealthCheck healthCheck = spy(new HealthCheckImpl(testEndpoint, mockStrategy));

        collection.add(healthCheck);

        // Call close
        collection.close();

        // Verify stop was called on all health checks
        verify(healthCheck).stop();
    }

}
