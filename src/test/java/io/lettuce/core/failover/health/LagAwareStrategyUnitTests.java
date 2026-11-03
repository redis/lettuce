/*
 * Copyright 2026-Present, Redis Ltd. and Contributors
 * 
 * All rights reserved.
 *
 * Licensed under the MIT License.
 */
package io.lettuce.core.failover.health;

import io.lettuce.core.RedisCredentials;
import io.lettuce.core.RedisException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.support.http.HttpClient;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link LagAwareStrategy}.
 */
@ExtendWith(MockitoExtension.class)
@Tag(UNIT_TEST)
class LagAwareStrategyUnitTests {

    private final URI restEndpoint = URI.create("https://localhost:9443");

    @Mock
    private Supplier<RedisCredentials> credentialsSupplier;

    @Mock
    private LagAwareStrategy.Config config;

    @Test
    void shouldNotCloseProvidedHttpClient() throws IOException {
        when(config.getRestEndpoint()).thenReturn(restEndpoint);
        when(config.getCredentialsSupplier()).thenReturn(credentialsSupplier);
        HttpClient externalClient = mock(HttpClient.class);
        LagAwareStrategy strategy = new LagAwareStrategy(config, externalClient);

        strategy.close();

        verify(externalClient, never()).close();
    }

    @Test
    void testConfigDelegationMethods() {
        LagAwareStrategy.Config config = LagAwareStrategy.Config.builder(restEndpoint, credentialsSupplier).interval(10000)
                .timeout(2000).numProbes(5).delayInBetweenProbes(1000).policy(ProbingPolicy.BuiltIn.ANY_SUCCESS).build();

        LagAwareStrategy strategy = new LagAwareStrategy(config);

        assertEquals(10000, strategy.getInterval());
        assertEquals(2000, strategy.getTimeout());
        assertEquals(5, strategy.getNumProbes());
        assertEquals(1000, strategy.getDelayInBetweenProbes());
        assertEquals(ProbingPolicy.BuiltIn.ANY_SUCCESS, strategy.getPolicy());

        strategy.close();
    }

    @Test
    void testConfigCreateWithDefaults() {
        LagAwareStrategy.Config config = LagAwareStrategy.Config.create(restEndpoint, credentialsSupplier);

        assertTrue(config.isExtendedCheckEnabled());
        assertEquals(Duration.ofMillis(5000), config.getAvailabilityLagTolerance());
        assertEquals(restEndpoint, config.getRestEndpoint());
        assertEquals(credentialsSupplier, config.getCredentialsSupplier());
        assertNull(config.getSslOptions());
        assertEquals(5000, config.getInterval());
        assertEquals(1000, config.getTimeout());
        assertEquals(3, config.getNumProbes());
        assertEquals(500, config.getDelayInBetweenProbes());
    }

    @Test
    void testConfigDatabaseAvailability() {
        LagAwareStrategy.Config config = LagAwareStrategy.Config.databaseAvailability(restEndpoint, credentialsSupplier);

        assertFalse(config.isExtendedCheckEnabled());
    }

    @Test
    void testConfigLagAware() {
        LagAwareStrategy.Config config = LagAwareStrategy.Config.lagAware(restEndpoint, credentialsSupplier);

        assertTrue(config.isExtendedCheckEnabled());
        assertEquals(Duration.ofMillis(5000), config.getAvailabilityLagTolerance());
    }

    @Test
    void testConfigLagAwareWithTolerance() {
        Duration customTolerance = Duration.ofMillis(10000);
        LagAwareStrategy.Config config = LagAwareStrategy.Config.lagAwareWithTolerance(restEndpoint, credentialsSupplier,
                customTolerance);

        assertTrue(config.isExtendedCheckEnabled());
        assertEquals(customTolerance, config.getAvailabilityLagTolerance());
    }

    @Test
    void testHealthCheckWithExtendedChecksEnabled() throws Exception {
        // Setup
        LagAwareStrategy.Config config = LagAwareStrategy.Config.lagAware(restEndpoint, credentialsSupplier);
        RedisURI dbEndpoint = RedisURI.create("redis://redis-1.example.com:12000");
        RedisRestClient mockRestClient = mock(RedisRestClient.class);

        when(mockRestClient.getBdbs()).thenReturn(createDefaultBdbs());
        when(mockRestClient.checkBdbAvailability(eq(1L), eq(true), anyLong())).thenReturn(true);

        LagAwareStrategy strategy = createStrategyWithMock(config, mockRestClient);

        HealthStatus status = strategy.doHealthCheck(dbEndpoint);

        // Verify
        assertEquals(HealthStatus.HEALTHY, status);
        verify(mockRestClient).getBdbs();
        verify(mockRestClient).checkBdbAvailability(1L, true, 5000L);

        strategy.close();
    }

    @Test
    void testHealthCheckWithExtendedChecksDisabled() throws Exception {
        // Setup
        LagAwareStrategy.Config config = LagAwareStrategy.Config.databaseAvailability(restEndpoint, credentialsSupplier);
        RedisURI dbEndpoint = RedisURI.create("redis://redis-1.example.com:12000");
        RedisRestClient mockRestClient = mock(RedisRestClient.class);

        when(mockRestClient.getBdbs()).thenReturn(createDefaultBdbs());
        when(mockRestClient.checkBdbAvailability(eq(1L), eq(false))).thenReturn(true);

        LagAwareStrategy strategy = createStrategyWithMock(config, mockRestClient);

        HealthStatus status = strategy.doHealthCheck(dbEndpoint);

        // Verify
        assertEquals(HealthStatus.HEALTHY, status);
        verify(mockRestClient).getBdbs();
        verify(mockRestClient).checkBdbAvailability(1L, false);

        strategy.close();
    }

    @Test
    void testHealthCheckReturnsUnhealthyWhenCheckFails() throws Exception {
        // Setup
        LagAwareStrategy.Config config = LagAwareStrategy.Config.lagAware(restEndpoint, credentialsSupplier);
        RedisURI dbEndpoint = RedisURI.create("redis://redis-1.example.com:12000");
        RedisRestClient mockRestClient = mock(RedisRestClient.class);

        when(mockRestClient.getBdbs()).thenReturn(createDefaultBdbs());
        when(mockRestClient.checkBdbAvailability(eq(1L), eq(true), anyLong())).thenReturn(false);

        LagAwareStrategy strategy = createStrategyWithMock(config, mockRestClient);

        HealthStatus status = strategy.doHealthCheck(dbEndpoint);

        // Verify
        assertEquals(HealthStatus.UNHEALTHY, status);

        strategy.close();
    }

    @Test
    void testBdbIdCaching() throws Exception {
        // Setup
        LagAwareStrategy.Config config = LagAwareStrategy.Config.lagAware(restEndpoint, credentialsSupplier);
        RedisURI dbEndpoint = RedisURI.create("redis://redis-1.example.com:12000");
        RedisRestClient mockRestClient = mock(RedisRestClient.class);

        when(mockRestClient.getBdbs()).thenReturn(createDefaultBdbs());
        when(mockRestClient.checkBdbAvailability(eq(1L), eq(true), anyLong())).thenReturn(true);

        LagAwareStrategy strategy = createStrategyWithMock(config, mockRestClient);

        // Test - call health check twice
        strategy.doHealthCheck(dbEndpoint);
        strategy.doHealthCheck(dbEndpoint);

        // Verify getBdbs() was only called once (BDB ID was cached)
        verify(mockRestClient, times(1)).getBdbs();
        verify(mockRestClient, times(2)).checkBdbAvailability(1L, true, 5000L);

        strategy.close();
    }

    @Test
    void testErrorWhenNoMatchingBdbFound() throws Exception {
        // Setup
        LagAwareStrategy.Config config = LagAwareStrategy.Config.lagAware(restEndpoint, credentialsSupplier);
        RedisURI dbEndpoint = RedisURI.create("redis://unknown-host.example.com:12000");
        RedisRestClient mockRestClient = mock(RedisRestClient.class);

        when(mockRestClient.getBdbs()).thenReturn(createDefaultBdbs());

        LagAwareStrategy strategy = createStrategyWithMock(config, mockRestClient);

        // Test - should throw RedisException because no matching BDB found
        assertThrows(RedisException.class, () -> strategy.doHealthCheck(dbEndpoint));

        strategy.close();
    }

    private List<RedisRestClient.BdbInfo> createDefaultBdbs() {
        List<String> addr = Collections.singletonList("10.0.0.1");
        RedisRestClient.EndpointInfo endpoint = new RedisRestClient.EndpointInfo(addr, "redis-1.example.com", 12000, "1:0");
        RedisRestClient.BdbInfo bdb = new RedisRestClient.BdbInfo(1L, Collections.singletonList(endpoint));
        return Collections.singletonList(bdb);
    }

    private LagAwareStrategy createStrategyWithMock(LagAwareStrategy.Config config, RedisRestClient mockRestClient) {
        return new LagAwareStrategy(config) {

            @Override
            RedisRestClient createRedisRestClient(LagAwareStrategy.Config cfg, HttpClient client) {
                return mockRestClient;
            }

        };
    }

}
