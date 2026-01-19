package io.lettuce.core.failover;

import static io.lettuce.TestTags.UNIT_TEST;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.lettuce.core.*;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.failover.AbstractRedisMultiDbConnectionBuilder.DatabaseFutureMap;
import io.lettuce.core.failover.AbstractRedisMultiDbConnectionBuilder.DatabaseMap;
import io.lettuce.core.failover.api.StatefulRedisMultiDbConnection;
import io.lettuce.core.failover.health.*;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;

/**
 * Comprehensive unit tests for {@link AbstractRedisMultiDbConnectionBuilder}, {@link MultiDbAsyncConnectionBuilder}, and
 * {@link MultiDbAsyncPubSubConnectionBuilder}.
 * <p>
 * These tests focus on the async connection building logic, health check integration, weight-based database selection, and
 * error handling scenarios.
 *
 * @author Ali Takavci
 * @since 7.4
 */
@Tag(UNIT_TEST)
@ExtendWith(MockitoExtension.class)
class MultiDbAsyncConnectionBuilderUnitTests {

    @Mock
    private MultiDbClientImpl client;

    @Mock
    private StatefulRedisConnection<String, String> mockConnection1;

    @Mock
    private StatefulRedisConnection<String, String> mockConnection2;

    @Mock
    private StatefulRedisConnection<String, String> mockConnection3;

    @Mock
    private StatefulRedisPubSubConnection<String, String> mockPubSubConnection1;

    @Mock
    private StatefulRedisPubSubConnection<String, String> mockPubSubConnection2;

    @Mock
    private StatefulRedisConnectionImpl<String, String> mockConnectionImpl1;

    @Mock
    private StatefulRedisConnectionImpl<String, String> mockConnectionImpl2;

    @Mock
    private DatabaseEndpoint mockEndpoint1;

    @Mock
    private DatabaseEndpoint mockEndpoint2;

    @Mock
    private HealthCheck mockHealthCheck1;

    @Mock
    private HealthCheck mockHealthCheck2;

    private ClientResources resources;

    private RedisCodec<String, String> codec;

    private RedisURI uri1;

    private RedisURI uri2;

    private RedisURI uri3;

    private DatabaseConfig config1;

    private DatabaseConfig config2;

    private DatabaseConfig config3;

    private MultiDbAsyncConnectionBuilder<String, String> regularBuilder;

    private MultiDbAsyncPubSubConnectionBuilder<String, String> pubSubBuilder;

    @BeforeEach
    void setUp() {
        resources = DefaultClientResources.create();
        codec = StringCodec.UTF8;

        uri1 = RedisURI.create("redis://localhost:6379");
        uri2 = RedisURI.create("redis://localhost:6380");
        uri3 = RedisURI.create("redis://localhost:6381");

        config1 = DatabaseConfig.builder(uri1).weight(1.0f).clientOptions(ClientOptions.create())
                .healthCheckStrategySupplier(HealthCheckStrategySupplier.NO_HEALTH_CHECK).build();

        config2 = DatabaseConfig.builder(uri2).weight(0.5f).clientOptions(ClientOptions.create())
                .healthCheckStrategySupplier(HealthCheckStrategySupplier.NO_HEALTH_CHECK).build();

        config3 = DatabaseConfig.builder(uri3).weight(0.25f).clientOptions(ClientOptions.create())
                .healthCheckStrategySupplier(HealthCheckStrategySupplier.NO_HEALTH_CHECK).build();

        regularBuilder = new MultiDbAsyncConnectionBuilder<>(client, resources, codec);
        pubSubBuilder = new MultiDbAsyncPubSubConnectionBuilder<>(client, resources, codec);
    }

    @AfterEach
    void tearDown() {
        if (resources != null) {
            resources.shutdown();
        }
    }

    // ============ Helper Methods ============

    private RedisDatabaseImpl<StatefulRedisConnection<String, String>> createMockDatabase(DatabaseConfig config,
            StatefulRedisConnection<String, String> connection, HealthCheck healthCheck) {
        CircuitBreakerImpl circuitBreaker = new CircuitBreakerImpl(config.getCircuitBreakerConfig());
        return new RedisDatabaseImpl<>(config, connection, mockEndpoint1, circuitBreaker, healthCheck);
    }

    // ============ Constructor Tests ============

    @Nested
    @DisplayName("Constructor Tests")
    @Tag(UNIT_TEST)
    class ConstructorTests {

        @Test
        @DisplayName("Should use provided client and codec when connecting for regular builder")
        void shouldUseProvidedClientAndCodecForRegularBuilder() {
            ConnectionFuture<StatefulRedisConnection<String, String>> mockFuture = ConnectionFuture
                    .from(new InetSocketAddress("localhost", 6379), CompletableFuture.completedFuture(mockConnection1));

            when(client.connectAsync(codec, uri1)).thenReturn(mockFuture);

            ConnectionFuture<StatefulRedisConnection<String, String>> result = regularBuilder.connectAsync(codec, uri1);

            assertThat((Object) result).isSameAs(mockFuture);
            verify(client).connectAsync(codec, uri1);
        }

        @Test
        @DisplayName("Should use provided client and codec when connecting for PubSub builder")
        void shouldUseProvidedClientAndCodecForPubSubBuilder() {
            ConnectionFuture<StatefulRedisPubSubConnection<String, String>> mockFuture = ConnectionFuture
                    .from(new InetSocketAddress("localhost", 6379), CompletableFuture.completedFuture(mockPubSubConnection1));

            when(client.connectPubSubAsync(codec, uri1)).thenReturn(mockFuture);

            ConnectionFuture<StatefulRedisPubSubConnection<String, String>> result = pubSubBuilder.connectAsync(codec, uri1);

            assertThat((Object) result).isSameAs(mockFuture);
            verify(client).connectPubSubAsync(codec, uri1);
        }

        @Test
        @DisplayName("Should create health status manager using provided resources")
        void shouldCreateHealthStatusManagerUsingResources() {
            HealthStatusManager manager = regularBuilder.createHealthStatusManager();

            assertThat(manager).isNotNull();
            assertThat(manager).isInstanceOf(HealthStatusManagerImpl.class);

            manager.close();
        }

    }

    // ============ Health Status Manager Tests ============

    @Nested
    @DisplayName("Health Status Manager Tests")
    @Tag(UNIT_TEST)
    class HealthStatusManagerTests {

        @Test
        @DisplayName("Should create health status manager")
        void shouldCreateHealthStatusManager() {
            HealthStatusManager manager = regularBuilder.createHealthStatusManager();
            assertThat(manager).isNotNull();
            assertThat(manager).isInstanceOf(HealthStatusManagerImpl.class);
        }

    }

    // ============ Database Endpoint Extraction Tests ============

    @Nested
    @DisplayName("Database Endpoint Extraction Tests")
    @Tag(UNIT_TEST)
    class DatabaseEndpointExtractionTests {

        @Mock
        private StatefulRedisConnectionImpl<String, String> mockConnectionImpl;

        @Mock
        private DatabaseEndpointImpl mockDatabaseEndpointImpl;

        @Test
        @DisplayName("Should extract DatabaseEndpoint directly when channel writer is DatabaseEndpoint")
        void shouldExtractDatabaseEndpointDirectly() {
            // DatabaseEndpointImpl extends DefaultEndpoint (which implements RedisChannelWriter)
            // and implements DatabaseEndpoint, so it can be used as both
            when(mockConnectionImpl.getChannelWriter()).thenReturn(mockDatabaseEndpointImpl);

            DatabaseEndpoint result = regularBuilder.extractDatabaseEndpoint(mockConnectionImpl);

            assertThat(result).isSameAs(mockDatabaseEndpointImpl);
            verify(mockConnectionImpl).getChannelWriter();
        }

        @Test
        @DisplayName("Should unwrap DatabaseEndpoint when channel writer implements Delegating")
        void shouldUnwrapDatabaseEndpointFromDelegating() {
            // Create a mock that implements both RedisChannelWriter and Delegating<RedisChannelWriter>
            RedisChannelWriter delegatingWriter = mock(RedisChannelWriter.class,
                    withSettings().extraInterfaces(Delegating.class));

            // Configure the Delegating behavior
            @SuppressWarnings("unchecked")
            Delegating<RedisChannelWriter> delegating = (Delegating<RedisChannelWriter>) delegatingWriter;

            // The extractDatabaseEndpoint method calls unwrap() on the Delegating interface
            // We need to stub unwrap() to return the actual DatabaseEndpoint
            when(delegating.unwrap()).thenReturn(mockDatabaseEndpointImpl);

            // Configure the connection to return the delegating writer
            when(mockConnectionImpl.getChannelWriter()).thenReturn(delegatingWriter);

            DatabaseEndpoint result = regularBuilder.extractDatabaseEndpoint(mockConnectionImpl);

            // Verify that the unwrapped DatabaseEndpoint is returned
            assertThat(result).isSameAs(mockDatabaseEndpointImpl);
            verify(mockConnectionImpl).getChannelWriter();
            verify(delegating).unwrap();
        }

    }

    // ============ Health Check Failure Detection Tests ============

    @Nested
    @DisplayName("Health Check Failure Detection Tests")
    @Tag(UNIT_TEST)
    class HealthCheckFailureDetectionTests {

        @Test
        @DisplayName("Should detect all health checks failed when all completed and none healthy")
        void shouldDetectAllHealthChecksFailed() {
            Map<RedisURI, CompletableFuture<HealthStatus>> healthStatusFutures = new HashMap<>();
            healthStatusFutures.put(uri1, CompletableFuture.completedFuture(HealthStatus.UNHEALTHY));
            healthStatusFutures.put(uri2, CompletableFuture.completedFuture(HealthStatus.UNHEALTHY));

            boolean allFailed = regularBuilder.checkIfAllFailed(healthStatusFutures);

            assertThat(allFailed).isTrue();
        }

        @Test
        @DisplayName("Should not detect all failed when at least one is healthy")
        void shouldNotDetectAllFailedWhenOneHealthy() {
            Map<RedisURI, CompletableFuture<HealthStatus>> healthStatusFutures = new HashMap<>();
            healthStatusFutures.put(uri1, CompletableFuture.completedFuture(HealthStatus.HEALTHY));
            healthStatusFutures.put(uri2, CompletableFuture.completedFuture(HealthStatus.UNHEALTHY));

            boolean allFailed = regularBuilder.checkIfAllFailed(healthStatusFutures);

            assertThat(allFailed).isFalse();
        }

        @Test
        @DisplayName("Should not detect all failed when some health checks are incomplete")
        void shouldNotDetectAllFailedWhenIncomplete() {
            Map<RedisURI, CompletableFuture<HealthStatus>> healthStatusFutures = new HashMap<>();
            healthStatusFutures.put(uri1, CompletableFuture.completedFuture(HealthStatus.UNHEALTHY));
            healthStatusFutures.put(uri2, new CompletableFuture<>()); // Not completed

            boolean allFailed = regularBuilder.checkIfAllFailed(healthStatusFutures);

            assertThat(allFailed).isFalse();
        }

        @Test
        @DisplayName("Should detect all failed when one exceptional and one unhealthy")
        void shouldDetectAllFailedWhenExceptionalAndUnhealthy() {
            Map<RedisURI, CompletableFuture<HealthStatus>> healthStatusFutures = new HashMap<>();
            CompletableFuture<HealthStatus> exceptionalFuture = new CompletableFuture<>();
            exceptionalFuture.completeExceptionally(new RuntimeException("Health check failed"));

            healthStatusFutures.put(uri1, exceptionalFuture);
            healthStatusFutures.put(uri2, CompletableFuture.completedFuture(HealthStatus.UNHEALTHY));

            boolean allFailed = regularBuilder.checkIfAllFailed(healthStatusFutures);

            // Exceptional futures are filtered out, so only UNHEALTHY is checked
            assertThat(allFailed).isTrue();
        }

        @Test
        @DisplayName("Should not detect all failed when one exceptional and one healthy")
        void shouldNotDetectAllFailedWhenExceptionalAndHealthy() {
            Map<RedisURI, CompletableFuture<HealthStatus>> healthStatusFutures = new HashMap<>();
            CompletableFuture<HealthStatus> exceptionalFuture = new CompletableFuture<>();
            exceptionalFuture.completeExceptionally(new RuntimeException("Health check failed"));

            healthStatusFutures.put(uri1, exceptionalFuture);
            healthStatusFutures.put(uri2, CompletableFuture.completedFuture(HealthStatus.HEALTHY));

            boolean allFailed = regularBuilder.checkIfAllFailed(healthStatusFutures);

            assertThat(allFailed).isFalse();
        }

    }

    // ============ Initial Database Candidate Selection Tests ============

    @Nested
    @DisplayName("Initial Database Candidate Selection Tests")
    @Tag(UNIT_TEST)
    class InitialDatabaseCandidateSelectionTests {

        @Test
        @DisplayName("Should select highest weighted healthy database")
        void shouldSelectHighestWeightedHealthyDatabase() {
            DatabaseMap<StatefulRedisConnection<String, String>> databases = new DatabaseMap<>();
            RedisDatabaseImpl<StatefulRedisConnection<String, String>> db1 = createMockDatabase(config1, mockConnection1, null);
            RedisDatabaseImpl<StatefulRedisConnection<String, String>> db2 = createMockDatabase(config2, mockConnection2, null);

            databases.put(uri1, db1);
            databases.put(uri2, db2);

            List<DatabaseConfig> sortedConfigs = Arrays.asList(config1, config2);

            RedisDatabaseImpl<StatefulRedisConnection<String, String>> selected = regularBuilder
                    .findInitialDbCandidate(sortedConfigs, databases, new AtomicReference<>());

            assertThat(selected).isNotNull();
            assertThat(selected).isSameAs(db1);
        }

        @Test
        @DisplayName("Should return null when highest weighted database not yet connected")
        void shouldReturnNullWhenHighestWeightedNotConnected() {
            DatabaseMap<StatefulRedisConnection<String, String>> databases = new DatabaseMap<>();
            RedisDatabaseImpl<StatefulRedisConnection<String, String>> db2 = createMockDatabase(config2, mockConnection2, null);

            databases.put(uri2, db2);

            List<DatabaseConfig> sortedConfigs = Arrays.asList(config1, config2);

            RedisDatabaseImpl<StatefulRedisConnection<String, String>> selected = regularBuilder
                    .findInitialDbCandidate(sortedConfigs, databases, new AtomicReference<>());

            assertThat(selected).isNull();
        }

        @Test
        @DisplayName("Should return null when health check result not yet available")
        void shouldReturnNullWhenHealthCheckPending() {
            DatabaseMap<StatefulRedisConnection<String, String>> databases = new DatabaseMap<>();
            RedisDatabaseImpl<StatefulRedisConnection<String, String>> db1 = createMockDatabase(config1, mockConnection1,
                    mockHealthCheck1);

            when(mockHealthCheck1.getStatus()).thenReturn(HealthStatus.UNKNOWN);

            databases.put(uri1, db1);

            List<DatabaseConfig> sortedConfigs = Arrays.asList(config1);

            RedisDatabaseImpl<StatefulRedisConnection<String, String>> selected = regularBuilder
                    .findInitialDbCandidate(sortedConfigs, databases, new AtomicReference<>());

            assertThat(selected).isNull();
        }

        @Test
        @DisplayName("Should skip unhealthy databases and select next healthy one")
        void shouldSkipUnhealthyDatabases() {
            DatabaseMap<StatefulRedisConnection<String, String>> databases = new DatabaseMap<>();
            RedisDatabaseImpl<StatefulRedisConnection<String, String>> db1 = createMockDatabase(config1, mockConnection1,
                    mockHealthCheck1);
            RedisDatabaseImpl<StatefulRedisConnection<String, String>> db2 = createMockDatabase(config2, mockConnection2,
                    mockHealthCheck2);

            when(mockHealthCheck1.getStatus()).thenReturn(HealthStatus.UNHEALTHY);
            when(mockHealthCheck2.getStatus()).thenReturn(HealthStatus.HEALTHY);

            databases.put(uri1, db1);
            databases.put(uri2, db2);

            List<DatabaseConfig> sortedConfigs = Arrays.asList(config1, config2);

            RedisDatabaseImpl<StatefulRedisConnection<String, String>> selected = regularBuilder
                    .findInitialDbCandidate(sortedConfigs, databases, new AtomicReference<>());

            assertThat(selected).isNotNull();
            assertThat(selected).isSameAs(db2);
        }

        @Test
        @DisplayName("Should use atomic reference to ensure only one database is selected")
        void shouldUseAtomicReferenceForSelection() {
            DatabaseMap<StatefulRedisConnection<String, String>> databases = new DatabaseMap<>();
            RedisDatabaseImpl<StatefulRedisConnection<String, String>> db1 = createMockDatabase(config1, mockConnection1, null);
            RedisDatabaseImpl<StatefulRedisConnection<String, String>> db2 = createMockDatabase(config2, mockConnection2, null);

            databases.put(uri1, db1);
            databases.put(uri2, db2);

            List<DatabaseConfig> sortedConfigs = Arrays.asList(config1, config2);

            AtomicReference<RedisDatabaseImpl<StatefulRedisConnection<String, String>>> initialDb = new AtomicReference<>();

            // First call should select db1
            RedisDatabaseImpl<StatefulRedisConnection<String, String>> selected1 = regularBuilder
                    .findInitialDbCandidate(sortedConfigs, databases, initialDb);

            // Second call should return null because atomic reference is already set
            RedisDatabaseImpl<StatefulRedisConnection<String, String>> selected2 = regularBuilder
                    .findInitialDbCandidate(sortedConfigs, databases, initialDb);

            assertThat(selected1).isNotNull();
            assertThat(selected1).isSameAs(db1);
            assertThat(selected2).isNull();
        }

    }

    // ============ Health Status Futures Creation Tests ============

    @Nested
    @DisplayName("Health Status Futures Creation Tests")
    @Tag(UNIT_TEST)
    class HealthStatusFuturesCreationTests {

        @Test
        @DisplayName("Should create health status futures for databases with health checks")
        void shouldCreateHealthStatusFuturesWithHealthChecks() {
            HealthStatusManager healthStatusManager = new HealthStatusManagerImpl();
            DatabaseFutureMap<StatefulRedisConnection<String, String>> databaseFutures = new DatabaseFutureMap<>();

            RedisDatabaseImpl<StatefulRedisConnection<String, String>> db1 = createMockDatabase(config1, mockConnection1,
                    mockHealthCheck1);

            databaseFutures.put(uri1, CompletableFuture.completedFuture(db1));

            Map<RedisURI, CompletableFuture<HealthStatus>> healthStatusFutures = regularBuilder
                    .createHealthStatusFutures(databaseFutures, healthStatusManager);

            assertThat(healthStatusFutures).isNotNull();
            assertThat(healthStatusFutures).hasSize(1);
            assertThat(healthStatusFutures).containsKey(uri1);

            healthStatusManager.close();
        }

        @Test
        @DisplayName("Should default to HEALTHY for databases without health checks")
        void shouldDefaultToHealthyWithoutHealthChecks() throws Exception {
            HealthStatusManager healthStatusManager = new HealthStatusManagerImpl();
            DatabaseFutureMap<StatefulRedisConnection<String, String>> databaseFutures = new DatabaseFutureMap<>();

            RedisDatabaseImpl<StatefulRedisConnection<String, String>> db1 = createMockDatabase(config1, mockConnection1, null);

            databaseFutures.put(uri1, CompletableFuture.completedFuture(db1));

            Map<RedisURI, CompletableFuture<HealthStatus>> healthStatusFutures = regularBuilder
                    .createHealthStatusFutures(databaseFutures, healthStatusManager);

            assertThat(healthStatusFutures).isNotNull();
            assertThat(healthStatusFutures).hasSize(1);

            CompletableFuture<HealthStatus> future = healthStatusFutures.get(uri1);
            assertThat(future).isNotNull();
            assertThat(future.get()).isEqualTo(HealthStatus.HEALTHY);

            healthStatusManager.close();
        }

    }

    // ============ Database Futures Creation Tests ============
    // Note: Cannot fully test createDatabaseFutures as it requires complex mocking of
    // connection internals and DatabaseEndpoint extraction which is package-private

    // ============ Build Connection Tests ============

    @Nested
    @DisplayName("Build Connection Tests")
    @Tag(UNIT_TEST)
    class BuildConnectionTests {

        @Test
        @DisplayName("Should build connection with selected database and no remaining futures")
        void shouldBuildConnectionWithSelectedDatabase() {
            HealthStatusManager healthStatusManager = new HealthStatusManagerImpl();
            DatabaseMap<StatefulRedisConnection<String, String>> databases = new DatabaseMap<>();
            DatabaseFutureMap<StatefulRedisConnection<String, String>> databaseFutures = new DatabaseFutureMap<>();

            RedisDatabaseImpl<StatefulRedisConnection<String, String>> db1 = createMockDatabase(config1, mockConnection1, null);
            RedisDatabaseImpl<StatefulRedisConnection<String, String>> db2 = createMockDatabase(config2, mockConnection2, null);

            databases.put(uri1, db1);
            databases.put(uri2, db2);

            // No remaining futures - all databases are already connected
            StatefulRedisMultiDbConnection<String, String> connection = regularBuilder.buildConn(healthStatusManager, databases,
                    databaseFutures, db1);

            assertThat(connection).isNotNull();
            assertThat(connection.getCurrentEndpoint()).isEqualTo(uri1);

            // Verify only the initially connected databases are present
            assertThat(connection.getEndpoints()).containsExactlyInAnyOrder(uri1, uri2);

            healthStatusManager.close();
        }

        @Test
        @DisplayName("Should build connection with remaining database futures and complete them asynchronously")
        void shouldBuildConnectionWithRemainingFutures() {
            HealthStatusManager healthStatusManager = new HealthStatusManagerImpl();
            DatabaseMap<StatefulRedisConnection<String, String>> databases = new DatabaseMap<>();
            DatabaseFutureMap<StatefulRedisConnection<String, String>> databaseFutures = new DatabaseFutureMap<>();

            RedisDatabaseImpl<StatefulRedisConnection<String, String>> db1 = createMockDatabase(config1, mockConnection1, null);
            RedisDatabaseImpl<StatefulRedisConnection<String, String>> db2 = createMockDatabase(config2, mockConnection2, null);

            databases.put(uri1, db1);
            databases.put(uri2, db2);

            // Add a future for a database that's not yet in the map - this simulates async connection
            CompletableFuture<RedisDatabaseImpl<StatefulRedisConnection<String, String>>> db3Future = new CompletableFuture<>();
            databaseFutures.put(uri3, db3Future);

            StatefulRedisMultiDbConnection<String, String> connection = regularBuilder.buildConn(healthStatusManager, databases,
                    databaseFutures, db1);

            assertThat(connection).isNotNull();
            assertThat(connection.getCurrentEndpoint()).isEqualTo(uri1);

            // Initially, only db1 and db2 should be present
            assertThat(connection.getEndpoints()).containsExactlyInAnyOrder(uri1, uri2);

            // Now complete the db3 future - this should trigger the async completion callback
            RedisDatabaseImpl<StatefulRedisConnection<String, String>> db3 = createMockDatabase(config3, mockConnection3, null);
            db3Future.complete(db3);

            // Wait for the async completion callback to process
            // The callback is registered via completion.whenComplete() which runs on the completing thread
            await().atMost(Duration.ofSeconds(1)).untilAsserted(() -> {
                // After async completion, db3 should be added to the connection
                assertThat(connection.getEndpoints()).containsExactlyInAnyOrder(uri1, uri2, uri3);
            });

            healthStatusManager.close();
        }

    }

    // ============ Async Connection Builder Specific Tests ============

    @Nested
    @DisplayName("MultiDbAsyncConnectionBuilder Specific Tests")
    @Tag(UNIT_TEST)
    class MultiDbAsyncConnectionBuilderSpecificTests {

        @Test
        @DisplayName("Should delegate connectAsync to client for regular connections")
        void shouldDelegateConnectAsyncToClient() {
            ConnectionFuture<StatefulRedisConnection<String, String>> mockFuture = ConnectionFuture
                    .from(new InetSocketAddress("localhost", 6379), CompletableFuture.completedFuture(mockConnection1));

            when(client.connectAsync(codec, uri1)).thenReturn(mockFuture);

            ConnectionFuture<StatefulRedisConnection<String, String>> result = regularBuilder.connectAsync(codec, uri1);

            assertThat((Object) result).isNotNull();
            assertThat((Object) result).isSameAs(mockFuture);
            verify(client).connectAsync(codec, uri1);
        }

    }

    // ============ PubSub Connection Builder Specific Tests ============

    @Nested
    @DisplayName("MultiDbAsyncPubSubConnectionBuilder Specific Tests")
    @Tag(UNIT_TEST)
    class MultiDbAsyncPubSubConnectionBuilderSpecificTests {

        @Test
        @DisplayName("Should delegate connectAsync to client for PubSub connections")
        void shouldDelegateConnectPubSubAsyncToClient() {
            ConnectionFuture<StatefulRedisPubSubConnection<String, String>> mockFuture = ConnectionFuture
                    .from(new InetSocketAddress("localhost", 6379), CompletableFuture.completedFuture(mockPubSubConnection1));

            when(client.connectPubSubAsync(codec, uri1)).thenReturn(mockFuture);

            ConnectionFuture<StatefulRedisPubSubConnection<String, String>> result = pubSubBuilder.connectAsync(codec, uri1);

            assertThat((Object) result).isNotNull();
            assertThat((Object) result).isSameAs(mockFuture);
            verify(client).connectPubSubAsync(codec, uri1);
        }

    }

}
