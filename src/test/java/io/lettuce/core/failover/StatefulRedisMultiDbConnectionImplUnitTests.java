package io.lettuce.core.failover;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import io.lettuce.core.RedisConnectionStateListener;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.push.PushListener;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.failover.api.CircuitBreakerStateListener;
import io.lettuce.core.failover.health.HealthCheck;
import io.lettuce.core.failover.health.HealthStatus;
import io.lettuce.core.failover.health.HealthStatusChangeEvent;
import io.lettuce.core.failover.health.HealthStatusManager;
import io.lettuce.core.json.JsonParser;
import io.lettuce.core.protocol.RedisCommand;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.tracing.Tracing;

/**
 * Unit tests for {@link StatefulRedisMultiDbConnectionImpl}.
 *
 * @author Ali Takavci
 * @since 7.4
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@Tag(UNIT_TEST)
@DisplayName("StatefulRedisMultiDbConnectionImpl Unit Tests")
class StatefulRedisMultiDbConnectionImplUnitTests {

    @Mock
    private StatefulRedisConnection<String, String> connection1;

    @Mock
    private StatefulRedisConnection<String, String> connection2;

    @Mock
    private StatefulRedisConnection<String, String> connection3;

    @Mock
    private CircuitBreaker circuitBreaker1;

    @Mock
    private CircuitBreaker circuitBreaker2;

    @Mock
    private CircuitBreaker circuitBreaker3;

    @Mock
    private HealthCheck healthCheck1;

    @Mock
    private HealthCheck healthCheck2;

    @Mock
    private HealthCheck healthCheck3;

    @Mock
    private DatabaseEndpoint endpoint1;

    @Mock
    private DatabaseEndpoint endpoint2;

    @Mock
    private DatabaseEndpoint endpoint3;

    @Mock
    private ClientResources clientResources;

    @Mock
    private Tracing tracing;

    @Mock
    private HealthStatusManager healthStatusManager;

    @Mock
    private DatabaseConnectionFactory<StatefulRedisConnection<String, String>, String, String> connectionFactory;

    private RedisCodec<String, String> codec;

    private Supplier<JsonParser> parser;

    private RedisURI uri1;

    private RedisURI uri2;

    private RedisURI uri3;

    private RedisDatabase<StatefulRedisConnection<String, String>> db1;

    private RedisDatabase<StatefulRedisConnection<String, String>> db2;

    private RedisDatabase<StatefulRedisConnection<String, String>> db3;

    private Map<RedisURI, RedisDatabase<StatefulRedisConnection<String, String>>> databases;

    @BeforeEach
    void setUp() {
        codec = StringCodec.UTF8;
        parser = () -> null;

        when(clientResources.tracing()).thenReturn(tracing);

        uri1 = RedisURI.create("redis://localhost:6379");
        uri2 = RedisURI.create("redis://localhost:6380");
        uri3 = RedisURI.create("redis://localhost:6381");

        // Setup circuit breakers
        when(circuitBreaker1.getCurrentState()).thenReturn(CircuitBreaker.State.CLOSED);
        when(circuitBreaker2.getCurrentState()).thenReturn(CircuitBreaker.State.CLOSED);
        when(circuitBreaker3.getCurrentState()).thenReturn(CircuitBreaker.State.CLOSED);
        when(circuitBreaker1.getEndpoint()).thenReturn(uri1);
        when(circuitBreaker2.getEndpoint()).thenReturn(uri2);
        when(circuitBreaker3.getEndpoint()).thenReturn(uri3);

        // Setup health checks
        when(healthCheck1.getStatus()).thenReturn(HealthStatus.HEALTHY);
        when(healthCheck2.getStatus()).thenReturn(HealthStatus.HEALTHY);
        when(healthCheck3.getStatus()).thenReturn(HealthStatus.HEALTHY);

        // Setup connections
        when(connection1.isOpen()).thenReturn(true);
        when(connection2.isOpen()).thenReturn(true);
        when(connection3.isOpen()).thenReturn(true);
        when(connection1.closeAsync()).thenReturn(CompletableFuture.completedFuture(null));
        when(connection2.closeAsync()).thenReturn(CompletableFuture.completedFuture(null));
        when(connection3.closeAsync()).thenReturn(CompletableFuture.completedFuture(null));

        // Create database instances
        DatabaseConfig config1 = DatabaseConfig.builder(uri1).weight(1.0f).build();
        DatabaseConfig config2 = DatabaseConfig.builder(uri2).weight(0.5f).build();
        DatabaseConfig config3 = DatabaseConfig.builder(uri3).weight(0.25f).build();

        db1 = new RedisDatabase<>(config1, connection1, endpoint1, circuitBreaker1, healthCheck1);
        db2 = new RedisDatabase<>(config2, connection2, endpoint2, circuitBreaker2, healthCheck2);
        db3 = new RedisDatabase<>(config3, connection3, endpoint3, circuitBreaker3, healthCheck3);

        databases = new ConcurrentHashMap<>();
        databases.put(uri1, db1);
        databases.put(uri2, db2);
        databases.put(uri3, db3);
    }

    @Nested
    @Tag(UNIT_TEST)
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should throw IllegalArgumentException when connections is null")
        void shouldThrowWhenConnectionsIsNull() {
            assertThatThrownBy(() -> new StatefulRedisMultiDbConnectionImpl<>(null, clientResources, codec, parser,
                    connectionFactory, healthStatusManager)).isInstanceOf(IllegalArgumentException.class)
                            .hasMessageContaining("connections must not be empty");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when connections is empty")
        void shouldThrowWhenConnectionsIsEmpty() {
            Map<RedisURI, RedisDatabase<StatefulRedisConnection<String, String>>> emptyMap = new HashMap<>();
            assertThatThrownBy(() -> new StatefulRedisMultiDbConnectionImpl<>(emptyMap, clientResources, codec, parser,
                    connectionFactory, healthStatusManager)).isInstanceOf(IllegalArgumentException.class)
                            .hasMessageContaining("connections must not be empty");
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when healthStatusManager is null")
        void shouldThrowWhenHealthStatusManagerIsNull() {
            assertThatThrownBy(() -> new StatefulRedisMultiDbConnectionImpl<>(databases, clientResources, codec, parser,
                    connectionFactory, null)).isInstanceOf(IllegalArgumentException.class)
                            .hasMessageContaining("healthStatusManager must not be null");
        }

        @Test
        @DisplayName("Should initialize with healthy database as current")
        void shouldInitializeWithHealthyDatabase() {
            StatefulRedisMultiDbConnectionImpl<StatefulRedisConnection<String, String>, String, String> connection = new StatefulRedisMultiDbConnectionImpl<>(
                    databases, clientResources, codec, parser, connectionFactory, healthStatusManager);

            assertThat(connection.getCurrentDatabase()).isNotNull();
            assertThat(connection.getCurrentEndpoint()).isIn(uri1, uri2, uri3);
        }

        @Test
        @DisplayName("Should register circuit breaker listeners for all databases")
        void shouldRegisterCircuitBreakerListeners() {
            new StatefulRedisMultiDbConnectionImpl<>(databases, clientResources, codec, parser, connectionFactory,
                    healthStatusManager);

            verify(circuitBreaker1).addListener(any());
            verify(circuitBreaker2).addListener(any());
            verify(circuitBreaker3).addListener(any());
        }

        @Test
        @DisplayName("Should register health status listeners for all databases")
        void shouldRegisterHealthStatusListeners() {
            new StatefulRedisMultiDbConnectionImpl<>(databases, clientResources, codec, parser, connectionFactory,
                    healthStatusManager);

            verify(healthStatusManager).registerListener(eq(uri1), any());
            verify(healthStatusManager).registerListener(eq(uri2), any());
            verify(healthStatusManager).registerListener(eq(uri3), any());
        }

        @Test
        @DisplayName("Should select database with highest weight when all are healthy")
        void shouldSelectHighestWeightDatabase() {
            StatefulRedisMultiDbConnectionImpl<StatefulRedisConnection<String, String>, String, String> connection = new StatefulRedisMultiDbConnectionImpl<>(
                    databases, clientResources, codec, parser, connectionFactory, healthStatusManager);

            // db1 has weight 1.0, which is the highest
            assertThat(connection.getCurrentEndpoint()).isEqualTo(uri1);
        }

    }

    @Nested
    @Tag(UNIT_TEST)
    @DisplayName("Connection State Tests")
    class ConnectionStateTests {

        private StatefulRedisMultiDbConnectionImpl<StatefulRedisConnection<String, String>, String, String> connection;

        @BeforeEach
        void setUp() {
            connection = new StatefulRedisMultiDbConnectionImpl<>(databases, clientResources, codec, parser, connectionFactory,
                    healthStatusManager);
        }

        @Test
        @DisplayName("Should return current endpoint")
        void shouldReturnCurrentEndpoint() {
            RedisURI currentEndpoint = connection.getCurrentEndpoint();
            assertThat(currentEndpoint).isNotNull();
            assertThat(currentEndpoint).isIn(uri1, uri2, uri3);
        }

        @Test
        @DisplayName("Should return all endpoints")
        void shouldReturnAllEndpoints() {
            Iterable<RedisURI> endpoints = connection.getEndpoints();
            assertThat(endpoints).containsExactlyInAnyOrder(uri1, uri2, uri3);
        }

        @Test
        @DisplayName("Should return codec")
        void shouldReturnCodec() {
            assertThat(connection.getCodec()).isEqualTo(codec);
        }

        @Test
        @DisplayName("Should return resources")
        void shouldReturnResources() {
            assertThat(connection.getResources()).isEqualTo(clientResources);
        }

        @Test
        @DisplayName("Should delegate isOpen to current connection")
        void shouldDelegateIsOpen() {
            when(connection1.isOpen()).thenReturn(true);
            connection.switchTo(uri1);

            assertThat(connection.isOpen()).isTrue();
            verify(connection1).isOpen();
        }

    }

    @Nested
    @Tag(UNIT_TEST)
    @DisplayName("Timeout Management Tests")
    class TimeoutManagementTests {

        private StatefulRedisMultiDbConnectionImpl<StatefulRedisConnection<String, String>, String, String> connection;

        @BeforeEach
        void setUp() {
            connection = new StatefulRedisMultiDbConnectionImpl<>(databases, clientResources, codec, parser, connectionFactory,
                    healthStatusManager);
        }

        @Test
        @DisplayName("Should set timeout on all connections")
        void shouldSetTimeoutOnAllConnections() {
            Duration timeout = Duration.ofSeconds(5);
            connection.setTimeout(timeout);

            verify(connection1).setTimeout(timeout);
            verify(connection2).setTimeout(timeout);
            verify(connection3).setTimeout(timeout);
        }

        @Test
        @DisplayName("Should get timeout from current connection")
        void shouldGetTimeoutFromCurrentConnection() {
            Duration timeout = Duration.ofSeconds(10);
            when(connection1.getTimeout()).thenReturn(timeout);
            connection.switchTo(uri1);

            assertThat(connection.getTimeout()).isEqualTo(timeout);
            verify(connection1).getTimeout();
        }

    }

    @Nested
    @Tag(UNIT_TEST)
    @DisplayName("Auto Flush Tests")
    class AutoFlushTests {

        private StatefulRedisMultiDbConnectionImpl<StatefulRedisConnection<String, String>, String, String> connection;

        @BeforeEach
        void setUp() {
            connection = new StatefulRedisMultiDbConnectionImpl<>(databases, clientResources, codec, parser, connectionFactory,
                    healthStatusManager);
        }

        @Test
        @DisplayName("Should set auto flush on all connections")
        void shouldSetAutoFlushOnAllConnections() {
            connection.setAutoFlushCommands(false);

            verify(connection1).setAutoFlushCommands(false);
            verify(connection2).setAutoFlushCommands(false);
            verify(connection3).setAutoFlushCommands(false);
        }

        @Test
        @DisplayName("Should flush commands on current connection")
        void shouldFlushCommandsOnCurrentConnection() {
            connection.switchTo(uri1);
            connection.flushCommands();

            verify(connection1).flushCommands();
        }

        @Test
        @DisplayName("Should delegate isMulti to current connection")
        void shouldDelegateIsMulti() {
            when(connection1.isMulti()).thenReturn(true);
            connection.switchTo(uri1);

            assertThat(connection.isMulti()).isTrue();
            verify(connection1).isMulti();
        }

    }

    @Nested
    @Tag(UNIT_TEST)
    @DisplayName("Listener Management Tests")
    class ListenerManagementTests {

        private StatefulRedisMultiDbConnectionImpl<StatefulRedisConnection<String, String>, String, String> connection;

        @Mock
        private RedisConnectionStateListener stateListener;

        @Mock
        private PushListener pushListener;

        @BeforeEach
        void setUp() {
            connection = new StatefulRedisMultiDbConnectionImpl<>(databases, clientResources, codec, parser, connectionFactory,
                    healthStatusManager);
        }

        @Test
        @DisplayName("Should add connection state listener to current connection")
        void shouldAddConnectionStateListener() {
            connection.switchTo(uri1);
            connection.addListener(stateListener);

            verify(connection1).addListener(stateListener);
        }

        @Test
        @DisplayName("Should remove connection state listener from current connection")
        void shouldRemoveConnectionStateListener() {
            connection.switchTo(uri1);
            connection.addListener(stateListener);
            connection.removeListener(stateListener);

            verify(connection1).removeListener(stateListener);
        }

        @Test
        @DisplayName("Should add push listener to current connection")
        void shouldAddPushListener() {
            connection.switchTo(uri1);
            connection.addListener(pushListener);

            verify(connection1).addListener(pushListener);
        }

        @Test
        @DisplayName("Should remove push listener from current connection")
        void shouldRemovePushListener() {
            connection.switchTo(uri1);
            connection.addListener(pushListener);
            connection.removeListener(pushListener);

            verify(connection1).removeListener(pushListener);
        }

        @Test
        @DisplayName("Should move listeners when switching databases")
        void shouldMoveListenersWhenSwitching() {
            connection.switchTo(uri1);
            connection.addListener(stateListener);
            connection.addListener(pushListener);

            reset(connection1, connection2);

            connection.switchTo(uri2);

            verify(connection1).removeListener(stateListener);
            verify(connection1).removeListener(pushListener);
            verify(connection2).addListener(stateListener);
            verify(connection2).addListener(pushListener);
        }

    }

    @Nested
    @Tag(UNIT_TEST)
    @DisplayName("Database Switching Tests")
    class DatabaseSwitchingTests {

        private StatefulRedisMultiDbConnectionImpl<StatefulRedisConnection<String, String>, String, String> connection;

        @BeforeEach
        void setUp() {
            connection = new StatefulRedisMultiDbConnectionImpl<>(databases, clientResources, codec, parser, connectionFactory,
                    healthStatusManager);
        }

        @Test
        @DisplayName("Should switch to specified database")
        void shouldSwitchToDatabase() {
            connection.switchTo(uri2);
            assertThat(connection.getCurrentEndpoint()).isEqualTo(uri2);
        }

        @Test
        @DisplayName("Should not switch when already on target database")
        void shouldNotSwitchWhenAlreadyOnTarget() {
            connection.switchTo(uri1);
            RedisURI current = connection.getCurrentEndpoint();

            connection.switchTo(current);

            // Should still be on the same database
            assertThat(connection.getCurrentEndpoint()).isEqualTo(current);
        }

        @Test
        @DisplayName("Should throw exception when switching to unknown database")
        void shouldThrowWhenSwitchingToUnknownDatabase() {
            RedisURI unknownUri = RedisURI.create("redis://unknown:6379");

            assertThatThrownBy(() -> connection.switchTo(unknownUri)).isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("Unable to switch between endpoints");
        }

        @Test
        @DisplayName("Should hand over command queue when switching")
        void shouldHandOverCommandQueueWhenSwitching() {
            connection.switchTo(uri1);
            connection.switchTo(uri2);

            verify(endpoint1).handOverCommandQueue(endpoint2);
        }

        @Test
        @DisplayName("Should failover if switched to unhealthy database")
        void shouldFailoverIfSwitchedToUnhealthyDatabase() {
            // Make uri2 unhealthy
            when(healthCheck2.getStatus()).thenReturn(HealthStatus.UNHEALTHY);

            connection.switchTo(uri2);

            // Should failover to a healthy database (uri1 or uri3)
            assertThat(connection.getCurrentEndpoint()).isNotEqualTo(uri2);
        }

    }

    @Nested
    @Tag(UNIT_TEST)
    @DisplayName("Failover Tests")
    class FailoverTests {

        private StatefulRedisMultiDbConnectionImpl<StatefulRedisConnection<String, String>, String, String> connection;

        @BeforeEach
        void setUp() {
            connection = new StatefulRedisMultiDbConnectionImpl<>(databases, clientResources, codec, parser, connectionFactory,
                    healthStatusManager);
        }

        @Test
        @DisplayName("Should failover when circuit breaker opens")
        void shouldFailoverWhenCircuitBreakerOpens() {
            connection.switchTo(uri1);
            assertThat(connection.getCurrentEndpoint()).isEqualTo(uri1);

            // Capture the circuit breaker listener
            ArgumentCaptor<CircuitBreakerStateListener> listenerCaptor = ArgumentCaptor
                    .forClass(CircuitBreakerStateListener.class);
            verify(circuitBreaker1).addListener(listenerCaptor.capture());

            // Simulate circuit breaker opening
            when(circuitBreaker1.getCurrentState()).thenReturn(CircuitBreaker.State.OPEN);
            CircuitBreakerStateChangeEvent event = new CircuitBreakerStateChangeEvent(circuitBreaker1,
                    CircuitBreaker.State.CLOSED, CircuitBreaker.State.OPEN);
            listenerCaptor.getValue().onCircuitBreakerStateChange(event);

            // Should failover to another database
            assertThat(connection.getCurrentEndpoint()).isNotEqualTo(uri1);
        }

        @Test
        @DisplayName("Should failover when health status becomes unhealthy")
        void shouldFailoverWhenHealthStatusBecomesUnhealthy() {
            connection.switchTo(uri1);
            assertThat(connection.getCurrentEndpoint()).isEqualTo(uri1);

            // Capture the health status listener
            ArgumentCaptor<io.lettuce.core.failover.health.HealthStatusListener> listenerCaptor = ArgumentCaptor
                    .forClass(io.lettuce.core.failover.health.HealthStatusListener.class);
            verify(healthStatusManager).registerListener(eq(uri1), listenerCaptor.capture());

            // Simulate health status change to unhealthy
            when(healthCheck1.getStatus()).thenReturn(HealthStatus.UNHEALTHY);
            HealthStatusChangeEvent event = new HealthStatusChangeEvent(uri1, HealthStatus.HEALTHY, HealthStatus.UNHEALTHY);
            listenerCaptor.getValue().onStatusChange(event);

            // Should failover to another database
            assertThat(connection.getCurrentEndpoint()).isNotEqualTo(uri1);
        }

        @Test
        @DisplayName("Should select database with highest weight during failover")
        void shouldSelectHighestWeightDatabaseDuringFailover() {
            connection.switchTo(uri3); // Start with lowest weight
            assertThat(connection.getCurrentEndpoint()).isEqualTo(uri3);

            // Capture the circuit breaker listener
            ArgumentCaptor<CircuitBreakerStateListener> listenerCaptor = ArgumentCaptor
                    .forClass(CircuitBreakerStateListener.class);
            verify(circuitBreaker3).addListener(listenerCaptor.capture());

            // Simulate circuit breaker opening
            when(circuitBreaker3.getCurrentState()).thenReturn(CircuitBreaker.State.OPEN);
            CircuitBreakerStateChangeEvent event = new CircuitBreakerStateChangeEvent(circuitBreaker3,
                    CircuitBreaker.State.CLOSED, CircuitBreaker.State.OPEN);
            listenerCaptor.getValue().onCircuitBreakerStateChange(event);

            // Should failover to uri1 (highest weight)
            assertThat(connection.getCurrentEndpoint()).isEqualTo(uri1);
        }

        @Test
        @DisplayName("Should not failover when non-current database becomes unhealthy")
        void shouldNotFailoverWhenNonCurrentDatabaseBecomesUnhealthy() {
            connection.switchTo(uri1);
            RedisURI currentBefore = connection.getCurrentEndpoint();

            // Capture the health status listener for uri2
            ArgumentCaptor<io.lettuce.core.failover.health.HealthStatusListener> listenerCaptor = ArgumentCaptor
                    .forClass(io.lettuce.core.failover.health.HealthStatusListener.class);
            verify(healthStatusManager).registerListener(eq(uri2), listenerCaptor.capture());

            // Simulate health status change to unhealthy for uri2 (not current)
            when(healthCheck2.getStatus()).thenReturn(HealthStatus.UNHEALTHY);
            HealthStatusChangeEvent event = new HealthStatusChangeEvent(uri2, HealthStatus.HEALTHY, HealthStatus.UNHEALTHY);
            listenerCaptor.getValue().onStatusChange(event);

            // Should remain on uri1
            assertThat(connection.getCurrentEndpoint()).isEqualTo(currentBefore);
        }

    }

    @Nested
    @Tag(UNIT_TEST)
    @DisplayName("Database Management Tests")
    class DatabaseManagementTests {

        private StatefulRedisMultiDbConnectionImpl<StatefulRedisConnection<String, String>, String, String> connection;

        @Mock
        private StatefulRedisConnection<String, String> newConnection;

        @Mock
        private CircuitBreaker newCircuitBreaker;

        @Mock
        private HealthCheck newHealthCheck;

        @Mock
        private DatabaseEndpoint newEndpoint;

        @BeforeEach
        void setUp() {
            connection = new StatefulRedisMultiDbConnectionImpl<>(databases, clientResources, codec, parser, connectionFactory,
                    healthStatusManager);

            when(newCircuitBreaker.getCurrentState()).thenReturn(CircuitBreaker.State.CLOSED);
            when(newHealthCheck.getStatus()).thenReturn(HealthStatus.HEALTHY);
            when(newConnection.isOpen()).thenReturn(true);
        }

        @Test
        @DisplayName("Should add new database")
        void shouldAddNewDatabase() {
            RedisURI newUri = RedisURI.create("redis://localhost:6382");
            DatabaseConfig newConfig = DatabaseConfig.builder(newUri).weight(0.1f).build();
            RedisDatabase<StatefulRedisConnection<String, String>> newDb = new RedisDatabase<>(newConfig, newConnection,
                    newEndpoint, newCircuitBreaker, newHealthCheck);

            when(connectionFactory.createDatabase(eq(newConfig), eq(codec), eq(healthStatusManager))).thenReturn(newDb);

            connection.addDatabase(newConfig);

            assertThat(connection.getEndpoints()).contains(newUri);
            verify(healthStatusManager).registerListener(eq(newUri), any());
            verify(newCircuitBreaker).addListener(any());
        }

        @Test
        @DisplayName("Should throw when adding database with null config")
        void shouldThrowWhenAddingDatabaseWithNullConfig() {
            assertThatThrownBy(() -> connection.addDatabase(null)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("DatabaseConfig must not be null");
        }

        @Test
        @DisplayName("Should throw when adding database that already exists")
        void shouldThrowWhenAddingDatabaseThatAlreadyExists() {
            DatabaseConfig existingConfig = DatabaseConfig.builder(uri1).weight(1.0f).build();

            assertThatThrownBy(() -> connection.addDatabase(existingConfig)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Database already exists");
        }

        @Test
        @DisplayName("Should remove database")
        void shouldRemoveDatabase() {
            connection.switchTo(uri1); // Make sure we're not on uri2

            connection.removeDatabase(uri2);

            assertThat(connection.getEndpoints()).doesNotContain(uri2);
            verify(healthStatusManager).unregisterListener(eq(uri2), any());
            verify(healthStatusManager).remove(uri2);
            verify(connection2).close();
        }

        @Test
        @DisplayName("Should throw when removing null database")
        void shouldThrowWhenRemovingNullDatabase() {
            assertThatThrownBy(() -> connection.removeDatabase(null)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("RedisURI must not be null");
        }

        @Test
        @DisplayName("Should throw when removing current database")
        void shouldThrowWhenRemovingCurrentDatabase() {
            connection.switchTo(uri1);

            assertThatThrownBy(() -> connection.removeDatabase(uri1)).isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("Cannot remove the currently active database");
        }

        @Test
        @DisplayName("Should throw when removing unknown database")
        void shouldThrowWhenRemovingUnknownDatabase() {
            RedisURI unknownUri = RedisURI.create("redis://unknown:6379");

            assertThatThrownBy(() -> connection.removeDatabase(unknownUri)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Database not found");
        }

    }

    @Nested
    @Tag(UNIT_TEST)
    @DisplayName("Health Status Tests")
    class HealthStatusTests {

        private StatefulRedisMultiDbConnectionImpl<StatefulRedisConnection<String, String>, String, String> connection;

        @BeforeEach
        void setUp() {
            connection = new StatefulRedisMultiDbConnectionImpl<>(databases, clientResources, codec, parser, connectionFactory,
                    healthStatusManager);
        }

        @Test
        @DisplayName("Should return true when database is healthy and circuit breaker is closed")
        void shouldReturnTrueWhenDatabaseIsHealthy() {
            when(healthCheck1.getStatus()).thenReturn(HealthStatus.HEALTHY);
            when(circuitBreaker1.getCurrentState()).thenReturn(CircuitBreaker.State.CLOSED);

            assertThat(connection.isHealthy(uri1)).isTrue();
        }

        @Test
        @DisplayName("Should return false when database is unhealthy")
        void shouldReturnFalseWhenDatabaseIsUnhealthy() {
            when(healthCheck1.getStatus()).thenReturn(HealthStatus.UNHEALTHY);
            when(circuitBreaker1.getCurrentState()).thenReturn(CircuitBreaker.State.CLOSED);

            assertThat(connection.isHealthy(uri1)).isFalse();
        }

        @Test
        @DisplayName("Should return false when circuit breaker is open")
        void shouldReturnFalseWhenCircuitBreakerIsOpen() {
            when(healthCheck1.getStatus()).thenReturn(HealthStatus.HEALTHY);
            when(circuitBreaker1.getCurrentState()).thenReturn(CircuitBreaker.State.OPEN);

            assertThat(connection.isHealthy(uri1)).isFalse();
        }

        @Test
        @DisplayName("Should throw when checking health of unknown endpoint")
        void shouldThrowWhenCheckingHealthOfUnknownEndpoint() {
            RedisURI unknownUri = RedisURI.create("redis://unknown:6379");

            assertThatThrownBy(() -> connection.isHealthy(unknownUri)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unknown endpoint");
        }

        @Test
        @DisplayName("Should get database by URI")
        void shouldGetDatabaseByUri() {
            RedisDatabase<StatefulRedisConnection<String, String>> database = connection.getDatabase(uri1);

            assertThat(database).isNotNull();
            assertThat(database.getRedisURI()).isEqualTo(uri1);
        }

        @Test
        @DisplayName("Should throw when getting unknown database")
        void shouldThrowWhenGettingUnknownDatabase() {
            RedisURI unknownUri = RedisURI.create("redis://unknown:6379");

            assertThatThrownBy(() -> connection.getDatabase(unknownUri)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unknown endpoint");
        }

    }

    @Nested
    @Tag(UNIT_TEST)
    @DisplayName("Command Dispatch Tests")
    class CommandDispatchTests {

        private StatefulRedisMultiDbConnectionImpl<StatefulRedisConnection<String, String>, String, String> connection;

        @Mock
        private RedisCommand<String, String, String> command;

        @BeforeEach
        void setUp() {
            connection = new StatefulRedisMultiDbConnectionImpl<>(databases, clientResources, codec, parser, connectionFactory,
                    healthStatusManager);
        }

        @Test
        @DisplayName("Should dispatch command to current connection")
        void shouldDispatchCommandToCurrentConnection() {
            connection.switchTo(uri1);
            connection.dispatch(command);

            verify(connection1).dispatch(command);
        }

        @Test
        @DisplayName("Should dispatch collection of commands to current connection")
        void shouldDispatchCollectionToCurrentConnection() {
            connection.switchTo(uri1);
            Collection<RedisCommand<String, String, ?>> commands = Collections.singletonList(command);
            connection.dispatch(commands);

            verify(connection1).dispatch(commands);
        }

    }

    @Nested
    @Tag(UNIT_TEST)
    @DisplayName("Close Tests")
    class CloseTests {

        private StatefulRedisMultiDbConnectionImpl<StatefulRedisConnection<String, String>, String, String> connection;

        @BeforeEach
        void setUp() {
            connection = new StatefulRedisMultiDbConnectionImpl<>(databases, clientResources, codec, parser, connectionFactory,
                    healthStatusManager);
        }

        @Test
        @DisplayName("Should close all connections")
        void shouldCloseAllConnections() {
            connection.close();

            verify(connection1).close();
            verify(connection2).close();
            verify(connection3).close();
            verify(healthStatusManager).close();
        }

        @Test
        @DisplayName("Should close all connections asynchronously")
        void shouldCloseAllConnectionsAsync() {
            CompletableFuture<Void> future = connection.closeAsync();

            assertThat(future).isNotNull();
            verify(connection1).closeAsync();
            verify(connection2).closeAsync();
            verify(connection3).closeAsync();
        }

    }

    @Nested
    @Tag(UNIT_TEST)
    @DisplayName("Command Interface Tests")
    class CommandInterfaceTests {

        private StatefulRedisMultiDbConnectionImpl<StatefulRedisConnection<String, String>, String, String> connection;

        @BeforeEach
        void setUp() {
            connection = new StatefulRedisMultiDbConnectionImpl<>(databases, clientResources, codec, parser, connectionFactory,
                    healthStatusManager);
        }

        @Test
        @DisplayName("Should provide sync commands")
        void shouldProvideSyncCommands() {
            assertThat(connection.sync()).isNotNull();
        }

        @Test
        @DisplayName("Should provide async commands")
        void shouldProvideAsyncCommands() {
            assertThat(connection.async()).isNotNull();
        }

        @Test
        @DisplayName("Should provide reactive commands")
        void shouldProvideReactiveCommands() {
            assertThat(connection.reactive()).isNotNull();
        }

    }

}
