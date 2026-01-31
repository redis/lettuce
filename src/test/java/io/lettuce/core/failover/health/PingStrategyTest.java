package io.lettuce.core.failover.health;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.failover.RawConnectionFactory;

/**
 * Unit tests for {@link PingStrategy}.
 *
 * @author Ivo Gaydazhiev
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PingStrategy Unit Tests")
class PingStrategyTest {

    @Mock
    private RawConnectionFactory connectionFactory;

    @Mock
    private StatefulRedisConnection<?, ?> connection;

    @Mock
    private RedisCommands<?, ?> syncCommands;

    private RedisURI testUri;

    @BeforeEach
    void setUp() {
        testUri = RedisURI.builder().withHost("localhost").withPort(6379).build();
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create PingStrategy with default config")
        void shouldCreateWithDefaultConfig() {
            // When
            PingStrategy strategy = new PingStrategy(connectionFactory);

            // Then
            assertThat(strategy).isNotNull();
            assertThat(strategy.getInterval()).isEqualTo(HealthCheckStrategy.Config.create().getInterval());
            assertThat(strategy.getTimeout()).isEqualTo(HealthCheckStrategy.Config.create().getTimeout());
            assertThat(strategy.getNumProbes()).isEqualTo(HealthCheckStrategy.Config.create().getNumProbes());
            assertThat(strategy.getDelayInBetweenProbes())
                    .isEqualTo(HealthCheckStrategy.Config.create().getDelayInBetweenProbes());
            assertThat(strategy.getPolicy()).isEqualTo(HealthCheckStrategy.Config.create().getPolicy());
        }

        @Test
        @DisplayName("Should create PingStrategy with custom config")
        void shouldCreateWithCustomConfig() {
            // Given
            HealthCheckStrategy.Config customConfig = HealthCheckStrategy.Config.builder().interval(500).timeout(2000)
                    .numProbes(3).delayInBetweenProbes(100).policy(ProbingPolicy.BuiltIn.MAJORITY_SUCCESS).build();

            // When
            PingStrategy strategy = new PingStrategy(connectionFactory, customConfig);

            // Then
            assertThat(strategy.getInterval()).isEqualTo(500);
            assertThat(strategy.getTimeout()).isEqualTo(2000);
            assertThat(strategy.getNumProbes()).isEqualTo(3);
            assertThat(strategy.getDelayInBetweenProbes()).isEqualTo(100);
            assertThat(strategy.getPolicy()).isEqualTo(ProbingPolicy.BuiltIn.MAJORITY_SUCCESS);
        }

    }

    @Nested
    @DisplayName("Health Check Tests")
    class HealthCheckTests {

        @Test
        @DisplayName("Should return HEALTHY when PING returns PONG")
        @SuppressWarnings("unchecked")
        void shouldReturnHealthyWhenPingSucceeds() {
            // Given
            when(connectionFactory.create(testUri)).thenReturn((StatefulRedisConnection) connection);
            when(connection.sync()).thenReturn((RedisCommands) syncCommands);
            when(syncCommands.ping()).thenReturn("PONG");

            PingStrategy strategy = new PingStrategy(connectionFactory);

            // When
            HealthStatus status = strategy.doHealthCheck(testUri);

            // Then
            assertThat(status).isEqualTo(HealthStatus.HEALTHY);
            verify(connectionFactory).create(testUri);
            verify(connection).sync();
            verify(syncCommands).ping();
            verify(connection).close();
        }

        @Test
        @DisplayName("Should return UNHEALTHY when PING returns non-PONG response")
        @SuppressWarnings("unchecked")
        void shouldReturnUnhealthyWhenPingReturnsNonPong() {
            // Given
            when(connectionFactory.create(testUri)).thenReturn((StatefulRedisConnection) connection);
            when(connection.sync()).thenReturn((RedisCommands) syncCommands);
            when(syncCommands.ping()).thenReturn("UNEXPECTED");

            PingStrategy strategy = new PingStrategy(connectionFactory);

            // When
            HealthStatus status = strategy.doHealthCheck(testUri);

            // Then
            assertThat(status).isEqualTo(HealthStatus.UNHEALTHY);
            verify(connection).close();
        }

        @Test
        @DisplayName("Should return UNHEALTHY when connection is null")
        void shouldReturnUnhealthyWhenConnectionIsNull() {
            // Given
            when(connectionFactory.create(testUri)).thenReturn(null);

            PingStrategy strategy = new PingStrategy(connectionFactory);

            // When
            HealthStatus status = strategy.doHealthCheck(testUri);

            // Then
            assertThat(status).isEqualTo(HealthStatus.UNHEALTHY);
            verify(connectionFactory).create(testUri);
            verifyNoInteractions(connection);
        }

        @Test
        @DisplayName("Should return UNHEALTHY when connection throws exception")
        void shouldReturnUnhealthyWhenConnectionThrowsException() {
            // Given
            when(connectionFactory.create(testUri)).thenThrow(new RuntimeException("Connection failed"));

            PingStrategy strategy = new PingStrategy(connectionFactory);

            // When
            HealthStatus status = strategy.doHealthCheck(testUri);

            // Then
            assertThat(status).isEqualTo(HealthStatus.UNHEALTHY);
        }

        @Test
        @DisplayName("Should return UNHEALTHY when PING throws exception")
        @SuppressWarnings("unchecked")
        void shouldReturnUnhealthyWhenPingThrowsException() {
            // Given
            when(connectionFactory.create(testUri)).thenReturn((StatefulRedisConnection) connection);
            when(connection.sync()).thenReturn((RedisCommands) syncCommands);
            when(syncCommands.ping()).thenThrow(new RuntimeException("PING failed"));

            PingStrategy strategy = new PingStrategy(connectionFactory);

            // When
            HealthStatus status = strategy.doHealthCheck(testUri);

            // Then
            assertThat(status).isEqualTo(HealthStatus.UNHEALTHY);
            verify(connection).close();
        }

        @Test
        @DisplayName("Should close connection even when PING throws exception")
        @SuppressWarnings("unchecked")
        void shouldCloseConnectionWhenPingThrowsException() {
            // Given
            when(connectionFactory.create(testUri)).thenReturn((StatefulRedisConnection) connection);
            when(connection.sync()).thenReturn((RedisCommands) syncCommands);
            when(syncCommands.ping()).thenThrow(new RuntimeException("PING failed"));

            PingStrategy strategy = new PingStrategy(connectionFactory);

            // When
            strategy.doHealthCheck(testUri);

            // Then
            verify(connection).close();
        }

        @Test
        @DisplayName("Should handle multiple health checks")
        @SuppressWarnings("unchecked")
        void shouldHandleMultipleHealthChecks() {
            // Given
            when(connectionFactory.create(testUri)).thenReturn((StatefulRedisConnection) connection);
            when(connection.sync()).thenReturn((RedisCommands) syncCommands);
            when(syncCommands.ping()).thenReturn("PONG");

            PingStrategy strategy = new PingStrategy(connectionFactory);

            // When
            HealthStatus status1 = strategy.doHealthCheck(testUri);
            HealthStatus status2 = strategy.doHealthCheck(testUri);
            HealthStatus status3 = strategy.doHealthCheck(testUri);

            // Then
            assertThat(status1).isEqualTo(HealthStatus.HEALTHY);
            assertThat(status2).isEqualTo(HealthStatus.HEALTHY);
            assertThat(status3).isEqualTo(HealthStatus.HEALTHY);
            verify(connectionFactory, times(3)).create(testUri);
            verify(connection, times(3)).close();
        }

    }

    @Nested
    @DisplayName("Default Supplier Tests")
    class DefaultSupplierTests {

        @Test
        @DisplayName("Should create PingStrategy using DEFAULT supplier")
        void shouldCreateUsingDefaultSupplier() {
            // Given
            HealthCheckStrategySupplier supplier = PingStrategy.DEFAULT;

            // When
            HealthCheckStrategy strategy = supplier.get(testUri, connectionFactory);

            // Then
            assertThat(strategy).isInstanceOf(PingStrategy.class);
        }

    }

}
