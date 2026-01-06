package io.lettuce.core.failover;

import static io.lettuce.TestTags.INTEGRATION_TEST;
import static io.lettuce.core.codec.StringCodec.UTF8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.StreamSupport;

import javax.inject.Inject;

import org.awaitility.Durations;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.lettuce.core.RedisURI;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.failover.api.StatefulRedisMultiDbConnection;
import io.lettuce.test.LettuceExtension;
import io.lettuce.test.TestFutures;

/**
 * Integration tests for {@link MultiDbClient#connectAsync(RedisCodec)} method.
 * 
 * @author Ali Takavci
 * @since 7.4
 */
@ExtendWith(LettuceExtension.class)
@Tag(INTEGRATION_TEST)
class MultiDbClientConnectAsyncIntegrationTests extends MultiDbTestSupport {

    @Inject
    MultiDbClientConnectAsyncIntegrationTests(MultiDbClient client) {
        super(client);
    }

    @BeforeEach
    void setUp() {
        directClient1.connect().sync().flushall();
        directClient2.connect().sync().flushall();
    }

    @AfterAll
    void tearDownAll() {
        multiDbClient.shutdown();
    }

    @Test
    void connectAsyncWithCodec() throws Exception {
        CompletableFuture<StatefulRedisMultiDbConnection<String, String>> future = multiDbClient.connectAsync(UTF8);

        assertThat((Object) future).isNotNull();
        assertThat((Object) future).isInstanceOf(CompletableFuture.class);

        StatefulRedisMultiDbConnection<String, String> connection = future.get(10, TimeUnit.SECONDS);

        assertThat(connection).isNotNull();
        assertThat(connection.isOpen()).isTrue();
        assertThat(connection.getTimeout()).isEqualTo(RedisURI.DEFAULT_TIMEOUT_DURATION);
    }

    @Test
    void connectAsyncWithByteArrayCodec() throws Exception {
        CompletableFuture<StatefulRedisMultiDbConnection<byte[], byte[]>> future = multiDbClient
                .connectAsync(ByteArrayCodec.INSTANCE);

        assertThat((Object) future).isNotNull();

        StatefulRedisMultiDbConnection<byte[], byte[]> byteConnection = future.get(10, TimeUnit.SECONDS);

        assertThat(byteConnection).isNotNull();
        assertThat(byteConnection.isOpen()).isTrue();

        byteConnection.close();
    }

    @Test
    void connectAsyncShouldRejectNullCodec() {
        assertThatThrownBy(() -> multiDbClient.connectAsync(null)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("codec must not be null");
    }

    @Test
    void connectAsyncShouldCompleteSuccessfully() throws Exception {
        CompletableFuture<StatefulRedisMultiDbConnection<String, String>> future = multiDbClient.connectAsync(UTF8);

        // Wait for completion
        StatefulRedisMultiDbConnection<String, String> connection = future.get(10, TimeUnit.SECONDS);

        assertThat(connection).isNotNull();
        assertThat(connection.isOpen()).isTrue();
        assertThat(connection.getCurrentEndpoint()).isNotNull();
    }

    @Test
    void connectAsyncShouldAllowCommandExecution() throws Exception {
        CompletableFuture<StatefulRedisMultiDbConnection<String, String>> future = multiDbClient.connectAsync(UTF8);

        StatefulRedisMultiDbConnection<String, String> connection = future.get(10, TimeUnit.SECONDS);

        // Execute a command
        String result = TestFutures.getOrTimeout(connection.async().set("key1", "value1"));
        assertThat(result).isEqualTo("OK");

        String value = TestFutures.getOrTimeout(connection.async().get("key1"));
        assertThat(value).isEqualTo("value1");
    }

    @Test
    void connectAsyncShouldSupportDatabaseSwitching() throws Exception {
        CompletableFuture<StatefulRedisMultiDbConnection<String, String>> future = multiDbClient.connectAsync(UTF8);

        StatefulRedisMultiDbConnection<String, String> connection = future.get(10, TimeUnit.SECONDS);

        // Set a key on the current database
        TestFutures.awaitOrTimeout(connection.async().set("key1", "value1"));

        RedisURI currentEndpoint = connection.getCurrentEndpoint();
        RedisURI otherEndpoint = StreamSupport.stream(connection.getEndpoints().spliterator(), false)
                .filter(uri -> !uri.equals(currentEndpoint)).findFirst().get();

        // Switch to the other database
        connection.switchTo(otherEndpoint);

        // Key should not exist on the other database
        String value = TestFutures.getOrTimeout(connection.async().get("key1"));
        assertThat(value).isNull();

        // Set a different value on the other database
        TestFutures.awaitOrTimeout(connection.async().set("key1", "value2"));

        // Switch back to the original database
        connection.switchTo(currentEndpoint);

        // Original value should still be there
        value = TestFutures.getOrTimeout(connection.async().get("key1"));
        assertThat(value).isEqualTo("value1");
    }

    @Test
    void connectAsyncShouldWaitForHealthChecks() throws Exception {
        CompletableFuture<StatefulRedisMultiDbConnection<String, String>> future = multiDbClient.connectAsync(UTF8);

        StatefulRedisMultiDbConnection<String, String> connection = future.get(10, TimeUnit.SECONDS);

        // Verify that at least one database is healthy
        await().atMost(Durations.TWO_SECONDS).pollInterval(Durations.ONE_HUNDRED_MILLISECONDS).untilAsserted(() -> {
            boolean anyHealthy = StreamSupport.stream(connection.getEndpoints().spliterator(), false)
                    .anyMatch(uri -> connection.isHealthy(uri));
            assertThat(anyHealthy).isTrue();
        });
    }

    @Test
    void connectAsyncShouldReturnCompletableFuture() {
        CompletableFuture<StatefulRedisMultiDbConnection<String, String>> future = multiDbClient.connectAsync(UTF8);

        assertThat((Object) future).isInstanceOf(CompletableFuture.class);
    }

    @Test
    void connectAsyncShouldSupportMultipleConnections() throws Exception {
        CompletableFuture<StatefulRedisMultiDbConnection<String, String>> future1 = multiDbClient.connectAsync(UTF8);
        CompletableFuture<StatefulRedisMultiDbConnection<String, String>> future2 = multiDbClient.connectAsync(UTF8);

        StatefulRedisMultiDbConnection<String, String> connection = future1.get(10, TimeUnit.SECONDS);
        StatefulRedisMultiDbConnection<String, String> connection2 = future2.get(10, TimeUnit.SECONDS);

        assertThat(connection).isNotNull();
        assertThat(connection2).isNotNull();
        assertThat(connection).isNotSameAs(connection2);

        assertThat(connection.isOpen()).isTrue();
        assertThat(connection2.isOpen()).isTrue();

        connection2.close();
    }

    @Test
    void connectAsyncShouldHandleCompletionStageOperations() throws Exception {
        CompletableFuture<StatefulRedisMultiDbConnection<String, String>> future = multiDbClient.connectAsync(UTF8);

        CompletableFuture<String> resultFuture = future.thenApply(conn -> {
            return "connected";
        });

        String result = resultFuture.get(10, TimeUnit.SECONDS);
        assertThat(result).isEqualTo("connected");
        StatefulRedisMultiDbConnection<String, String> connection = future.get();
        assertThat(connection).isNotNull();
        assertThat(connection.isOpen()).isTrue();
    }

    @Test
    void connectAsyncShouldSupportWhenComplete() throws Exception {
        CompletableFuture<StatefulRedisMultiDbConnection<String, String>> future = multiDbClient.connectAsync(UTF8);

        CompletableFuture<Boolean> completionTracker = new CompletableFuture<>();

        future.whenComplete((conn, throwable) -> {
            if (throwable == null) {
                completionTracker.complete(true);
            } else {
                completionTracker.completeExceptionally(throwable);
            }
        });

        Boolean completed = completionTracker.get(10, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
        StatefulRedisMultiDbConnection<String, String> connection = future.get();
        assertThat(connection).isNotNull();
        assertThat(connection.isOpen()).isTrue();
    }

    @Test
    void connectAsyncShouldAllowSyncOperations() throws Exception {
        CompletableFuture<StatefulRedisMultiDbConnection<String, String>> future = multiDbClient.connectAsync(UTF8);

        StatefulRedisMultiDbConnection<String, String> connection = future.get(10, TimeUnit.SECONDS);

        // Use sync API
        String result = connection.sync().set("syncKey", "syncValue");
        assertThat(result).isEqualTo("OK");

        String value = connection.sync().get("syncKey");
        assertThat(value).isEqualTo("syncValue");
    }

    @Test
    void connectAsyncShouldAllowReactiveOperations() throws Exception {
        CompletableFuture<StatefulRedisMultiDbConnection<String, String>> future = multiDbClient.connectAsync(UTF8);

        StatefulRedisMultiDbConnection<String, String> connection = future.get(10, TimeUnit.SECONDS);

        // Use reactive API
        String result = connection.reactive().set("reactiveKey", "reactiveValue").block(Duration.ofSeconds(5));
        assertThat(result).isEqualTo("OK");

        String value = connection.reactive().get("reactiveKey").block(Duration.ofSeconds(5));
        assertThat(value).isEqualTo("reactiveValue");
    }

    @Test
    void connectAsyncShouldProvideAllEndpoints() throws Exception {
        CompletableFuture<StatefulRedisMultiDbConnection<String, String>> future = multiDbClient.connectAsync(UTF8);

        StatefulRedisMultiDbConnection<String, String> connection = future.get(10, TimeUnit.SECONDS);

        Iterable<RedisURI> endpoints = connection.getEndpoints();
        assertThat(endpoints).isNotNull();

        long count = StreamSupport.stream(endpoints.spliterator(), false).count();
        // We have 3 databases configured in MultiDbTestSupport (DB1, DB2, DB3)
        // Only databases that successfully connect AND remain open are included
        assertThat(count).isGreaterThanOrEqualTo(2).isLessThanOrEqualTo(3);
    }

    @Test
    void connectAsyncShouldProvideAccessToAllConnectedDatabases() throws Exception {
        CompletableFuture<StatefulRedisMultiDbConnection<String, String>> future = multiDbClient.connectAsync(UTF8);

        StatefulRedisMultiDbConnection<String, String> connection = future.get(10, TimeUnit.SECONDS);

        // Verify we can access all connected database endpoints
        Iterable<RedisURI> endpoints = connection.getEndpoints();
        assertThat(endpoints).isNotNull();

        long count = StreamSupport.stream(endpoints.spliterator(), false).count();
        assertThat(count).isGreaterThan(0);

        // Verify each endpoint is accessible
        for (RedisURI endpoint : endpoints) {
            assertThat(endpoint).isNotNull();
            assertThat(endpoint.getHost()).isNotEmpty();
            assertThat(endpoint.getPort()).isGreaterThan(0);
        }
    }

    /**
     * Edge case: Test that connection handles rapid successive calls correctly.
     */
    @Test
    void connectAsyncShouldHandleRapidSuccessiveCalls() throws Exception {
        // Create multiple connections rapidly
        CompletableFuture<StatefulRedisMultiDbConnection<String, String>> future1 = multiDbClient.connectAsync(UTF8);
        CompletableFuture<StatefulRedisMultiDbConnection<String, String>> future2 = multiDbClient.connectAsync(UTF8);
        CompletableFuture<StatefulRedisMultiDbConnection<String, String>> future3 = multiDbClient.connectAsync(UTF8);

        // All should complete successfully
        StatefulRedisMultiDbConnection<String, String> conn1 = future1.get(10, TimeUnit.SECONDS);
        StatefulRedisMultiDbConnection<String, String> conn2 = future2.get(10, TimeUnit.SECONDS);
        StatefulRedisMultiDbConnection<String, String> conn3 = future3.get(10, TimeUnit.SECONDS);

        assertThat(conn1).isNotNull();
        assertThat(conn2).isNotNull();
        assertThat(conn3).isNotNull();

        assertThat(conn1.isOpen()).isTrue();
        assertThat(conn2.isOpen()).isTrue();
        assertThat(conn3.isOpen()).isTrue();

        // Clean up
        conn1.closeAsync();
        conn2.closeAsync();
        conn3.closeAsync();
    }

    /**
     * Edge case: Test that connection can be closed immediately after creation.
     */
    @Test
    void connectAsyncShouldAllowImmediateClose() throws Exception {
        CompletableFuture<StatefulRedisMultiDbConnection<String, String>> future = multiDbClient.connectAsync(UTF8);

        StatefulRedisMultiDbConnection<String, String> conn = future.get(10, TimeUnit.SECONDS);

        // Close immediately
        conn.close();

        // Verify it's closed
        assertThat(conn.isOpen()).isFalse();
    }

    /**
     * Edge case: Test async close operation.
     */
    @Test
    void connectAsyncShouldSupportAsyncClose() throws Exception {
        CompletableFuture<StatefulRedisMultiDbConnection<String, String>> future = multiDbClient.connectAsync(UTF8);

        StatefulRedisMultiDbConnection<String, String> conn = future.get(10, TimeUnit.SECONDS);

        // Close asynchronously
        CompletableFuture<Void> closeFuture = conn.closeAsync();
        closeFuture.get(5, TimeUnit.SECONDS);

        // Verify it's closed
        assertThat(conn.isOpen()).isFalse();
    }

    /**
     * Edge case: Test connection with all databases having the same weight.
     */
    @Test
    void connectAsyncShouldHandleEqualWeights() throws Exception {
        // Create a client with equal weights
        DatabaseConfig db1 = DatabaseConfig.builder(MultiDbTestSupport.URI1).weight(1.0f).build();
        DatabaseConfig db2 = DatabaseConfig.builder(MultiDbTestSupport.URI2).weight(1.0f).build();

        MultiDbClient equalWeightClient = MultiDbClient.create(java.util.Arrays.asList(db1, db2));

        try {
            CompletableFuture<StatefulRedisMultiDbConnection<String, String>> future = equalWeightClient.connectAsync(UTF8);

            StatefulRedisMultiDbConnection<String, String> conn = future.get(10, TimeUnit.SECONDS);

            assertThat(conn).isNotNull();
            assertThat(conn.isOpen()).isTrue();

            // Verify we can execute commands
            String result = conn.sync().ping();
            assertThat(result).isEqualTo("PONG");

            conn.closeAsync();
        } finally {
            equalWeightClient.shutdown();
        }
    }

    /**
     * Test that connectAsync completes successfully even with partial database failures.
     */
    @Test
    void connectAsyncShouldSucceedWithPartialFailures() throws Exception {
        // Create a client with one valid and one invalid endpoint
        DatabaseConfig validDb = DatabaseConfig.builder(MultiDbTestSupport.URI1).weight(1.0f).build();
        DatabaseConfig invalidDb = DatabaseConfig.builder(RedisURI.create("redis://localhost:9999")).weight(0.5f).build();

        MultiDbClient partialClient = MultiDbClient.create(java.util.Arrays.asList(validDb, invalidDb));

        try {
            CompletableFuture<StatefulRedisMultiDbConnection<String, String>> future = partialClient.connectAsync(UTF8);

            StatefulRedisMultiDbConnection<String, String> conn = future.get(15, TimeUnit.SECONDS);

            assertThat(conn).isNotNull();
            assertThat(conn.isOpen()).isTrue();

            // Should be able to execute commands on the valid database
            String result = conn.sync().ping();
            assertThat(result).isEqualTo("PONG");

            conn.closeAsync();
        } finally {
            partialClient.shutdown();
        }
    }

    /**
     * Test that connectAsync fails when all databases are unreachable.
     */
    @Test
    void connectAsyncShouldFailWhenAllDatabasesUnreachable() {
        // Create a client with only invalid endpoints
        DatabaseConfig invalidDb1 = DatabaseConfig.builder(RedisURI.create("redis://localhost:9998")).weight(1.0f).build();
        DatabaseConfig invalidDb2 = DatabaseConfig.builder(RedisURI.create("redis://localhost:9999")).weight(0.5f).build();

        MultiDbClient failClient = MultiDbClient.create(java.util.Arrays.asList(invalidDb1, invalidDb2));

        try {
            CompletableFuture<StatefulRedisMultiDbConnection<String, String>> future = failClient.connectAsync(UTF8);

            assertThatThrownBy(() -> future.get(15, TimeUnit.SECONDS)).isInstanceOf(ExecutionException.class)
                    .hasCauseInstanceOf(io.lettuce.core.RedisConnectionException.class)
                    .hasMessageContaining("Failed to connect to any database");
        } finally {
            failClient.shutdown();
        }
    }

    /**
     * Test that connectAsync with null codec throws IllegalArgumentException.
     */
    @Test
    void connectAsyncShouldThrowOnNullCodec() {
        assertThatThrownBy(() -> multiDbClient.connectAsync(null)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("codec must not be null");
    }

    /**
     * Test that connectAsync can be called multiple times and creates independent connections.
     */
    @Test
    void connectAsyncShouldAllowMultipleConnections() throws Exception {
        CompletableFuture<StatefulRedisMultiDbConnection<String, String>> future1 = multiDbClient.connectAsync(UTF8);
        CompletableFuture<StatefulRedisMultiDbConnection<String, String>> future2 = multiDbClient.connectAsync(UTF8);

        StatefulRedisMultiDbConnection<String, String> conn1 = future1.get(10, TimeUnit.SECONDS);
        StatefulRedisMultiDbConnection<String, String> conn2 = future2.get(10, TimeUnit.SECONDS);

        try {
            assertThat(conn1).isNotNull();
            assertThat(conn2).isNotNull();
            assertThat(conn1).isNotSameAs(conn2);

            assertThat(conn1.isOpen()).isTrue();
            assertThat(conn2.isOpen()).isTrue();

            // Both should work independently
            assertThat(conn1.sync().ping()).isEqualTo("PONG");
            assertThat(conn2.sync().ping()).isEqualTo("PONG");
        } finally {
            conn1.close();
            conn2.close();
        }
    }

    /**
     * Test that connectAsync with ByteArrayCodec works correctly.
     */
    @Test
    void connectAsyncShouldWorkWithByteArrayCodec() throws Exception {
        CompletableFuture<StatefulRedisMultiDbConnection<byte[], byte[]>> future = multiDbClient
                .connectAsync(ByteArrayCodec.INSTANCE);

        StatefulRedisMultiDbConnection<byte[], byte[]> conn = future.get(10, TimeUnit.SECONDS);

        try {
            assertThat(conn).isNotNull();
            assertThat(conn.isOpen()).isTrue();

            // Test basic operations with byte arrays
            byte[] key = "testkey".getBytes();
            byte[] value = "testvalue".getBytes();

            conn.sync().set(key, value);
            byte[] retrieved = conn.sync().get(key);

            assertThat(retrieved).isEqualTo(value);
        } finally {
            conn.close();
        }
    }

    /**
     * Test that connectAsync future can be composed with other futures.
     */
    @Test
    void connectAsyncFutureShouldBeComposable() throws Exception {
        CompletableFuture<StatefulRedisMultiDbConnection<String, String>> future = multiDbClient.connectAsync(UTF8);

        CompletableFuture<String> pingFuture = future.thenApply(conn -> {
            return conn.sync().ping();
        });

        String result = pingFuture.get(10, TimeUnit.SECONDS);
        assertThat(result).isEqualTo("PONG");
        future.get().close();
    }

    /**
     * Test that connectAsync future handles exceptions properly in composition.
     */
    @Test
    void connectAsyncFutureShouldHandleExceptionsInComposition() {
        // Create a client with invalid endpoint
        DatabaseConfig invalidDb = DatabaseConfig.builder(RedisURI.create("redis://localhost:9999")).weight(1.0f).build();
        MultiDbClient failClient = MultiDbClient.create(java.util.Arrays.asList(invalidDb));

        try {
            CompletableFuture<StatefulRedisMultiDbConnection<String, String>> future = failClient.connectAsync(UTF8);

            CompletableFuture<String> composedFuture = future.thenApply(conn -> conn.sync().ping())
                    .exceptionally(throwable -> "ERROR: " + throwable.getMessage());

            String result = composedFuture.get(15, TimeUnit.SECONDS);
            assertThat(result).startsWith("ERROR:");
        } catch (Exception e) {
            // Expected
        } finally {
            failClient.shutdown();
        }
    }

}
